package measure

/**
 * Persistence for [MeasurementHistory]: one JSON file, versioned so a
 * future format can migrate instead of crashing on old files.
 *
 * No JSON library is involved — the format is small and fixed, and a
 * hand-rolled encoder/decoder keeps the core dependency-free (the same
 * reason the rest of the core avoids Android and third-party code).
 * Numbers use [Double.toString], which round-trips exactly, so a saved
 * room reloads with the same area to the last bit.
 *
 * File format (version 1):
 * `{"version":1,"measurements":[{...}, ...]}`
 * where each measurement is
 * `{"id":..,"name":..,"createdAtMillis":..,"note":..|null,
 *   "anchors":[{"id":..,"x":..,"y":..,"z":..,"label":..|null}, ...]}`
 */

/** JSON-escape a string: quotes, backslashes and control characters. */
internal fun escapeJson(value: String): String {
    val out = StringBuilder(value.length + 2)
    out.append('"')
    for (ch in value) {
        when (ch) {
            '"' -> out.append("\\\"")
            '\\' -> out.append("\\\\")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> {
                if (ch < ' ') out.append("\\u%04x".format(ch.code)) else out.append(ch)
            }
        }
    }
    out.append('"')
    return out.toString()
}

private fun encodeAnchor(anchor: Anchor): String {
    val label = anchor.label?.let { escapeJson(it) } ?: "null"
    val p = anchor.position
    return "{\"id\":${escapeJson(anchor.id)}," +
        "\"x\":${p.x},\"y\":${p.y},\"z\":${p.z},\"label\":$label}"
}

internal fun encodeMeasurement(saved: SavedMeasurement): String {
    val note = saved.note?.let { escapeJson(it) } ?: "null"
    val anchors = saved.anchors.joinToString(",") { encodeAnchor(it) }
    return "{\"id\":${escapeJson(saved.id)}," +
        "\"name\":${escapeJson(saved.name)}," +
        "\"createdAtMillis\":${saved.createdAtMillis}," +
        "\"note\":$note,\"anchors\":[$anchors]}"
}

/** Encode a whole history as one version-1 document. */
internal fun encodeHistory(measurements: List<SavedMeasurement>): String {
    val body = measurements.joinToString(",") { encodeMeasurement(it) }
    return "{\"version\":1,\"measurements\":[$body]}"
}

/** Minimal JSON values, just enough to decode our own format. */
internal sealed interface JsonValue {
    data class Obj(val fields: Map<String, JsonValue>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Double) : JsonValue
    data object Null : JsonValue
}

/**
 * A small strict JSON parser for the store format: trailing content,
 * unterminated strings and bad escapes are all rejected instead of
 * silently tolerated, so a half-written file is never read as good data.
 */
internal class JsonParser(private val input: String) {
    private var pos = 0

    fun parseDocument(): JsonValue {
        val value = parseValue()
        skipWhitespace()
        require(pos == input.length) { "trailing content after JSON document" }
        return value
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun peek(): Char {
        require(pos < input.length) { "unexpected end of JSON input" }
        return input[pos]
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            'n' -> {
                expect("null")
                JsonValue.Null
            }
            else -> parseNumber()
        }
    }

    private fun expect(word: String) {
        require(input.startsWith(word, pos)) { "expected '$word' at offset $pos" }
        pos += word.length
    }

    private fun parseObject(): JsonValue.Obj {
        pos++ // '{'
        val fields = linkedMapOf<String, JsonValue>()
        skipWhitespace()
        if (pos < input.length && input[pos] == '}') {
            pos++
            return JsonValue.Obj(fields)
        }
        while (true) {
            skipWhitespace()
            require(peek() == '"') { "expected object key at offset $pos" }
            val key = parseString()
            skipWhitespace()
            require(peek() == ':') { "expected ':' after key at offset $pos" }
            pos++
            fields[key] = parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> pos++
                '}' -> {
                    pos++
                    return JsonValue.Obj(fields)
                }
                else -> throw IllegalArgumentException("expected ',' or '}' at offset $pos")
            }
        }
    }

    private fun parseArray(): JsonValue.Arr {
        pos++ // '['
        val items = mutableListOf<JsonValue>()
        skipWhitespace()
        if (pos < input.length && input[pos] == ']') {
            pos++
            return JsonValue.Arr(items)
        }
        while (true) {
            items += parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> pos++
                ']' -> {
                    pos++
                    return JsonValue.Arr(items)
                }
                else -> throw IllegalArgumentException("expected ',' or ']' at offset $pos")
            }
        }
    }

    private fun parseString(): String {
        require(peek() == '"') { "expected string at offset $pos" }
        pos++
        val out = StringBuilder()
        while (true) {
            require(pos < input.length) { "unterminated string" }
            val ch = input[pos++]
            when (ch) {
                '"' -> return out.toString()
                '\\' -> {
                    require(pos < input.length) { "unterminated escape" }
                    when (val esc = input[pos++]) {
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        '/' -> out.append('/')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'u' -> {
                            require(pos + 4 <= input.length) { "short unicode escape" }
                            val code = input.substring(pos, pos + 4).toIntOrNull(16)
                                ?: throw IllegalArgumentException("bad unicode escape")
                            out.append(code.toChar())
                            pos += 4
                        }
                        else -> throw IllegalArgumentException("bad escape '\\$esc'")
                    }
                }
                else -> out.append(ch)
            }
        }
    }

    private fun parseNumber(): JsonValue.Num {
        val start = pos
        while (pos < input.length && input[pos] in "-+0123456789.eE") pos++
        require(pos > start) { "expected value at offset $start" }
        val value = input.substring(start, pos).toDoubleOrNull()
            ?: throw IllegalArgumentException("bad number at offset $start")
        return JsonValue.Num(value)
    }
}

private fun JsonValue.Obj.stringField(name: String): String =
    (fields[name] as? JsonValue.Str)?.value
        ?: throw IllegalArgumentException("missing or non-string field '$name'")

private fun JsonValue.Obj.numberField(name: String): Double =
    (fields[name] as? JsonValue.Num)?.value
        ?: throw IllegalArgumentException("missing or non-number field '$name'")

private fun JsonValue.Obj.nullableStringField(name: String): String? =
    when (val value = fields[name]) {
        null, JsonValue.Null -> null
        is JsonValue.Str -> value.value
        else -> throw IllegalArgumentException("field '$name' must be a string or null")
    }

private fun decodeAnchor(value: JsonValue): Anchor {
    val obj = value as? JsonValue.Obj
        ?: throw IllegalArgumentException("anchor must be an object")
    return Anchor(
        id = obj.stringField("id"),
        position = Vec3(
            obj.numberField("x"),
            obj.numberField("y"),
            obj.numberField("z"),
        ),
        label = obj.nullableStringField("label"),
    )
}

internal fun decodeMeasurement(value: JsonValue): SavedMeasurement {
    val obj = value as? JsonValue.Obj
        ?: throw IllegalArgumentException("measurement must be an object")
    val anchors = (obj.fields["anchors"] as? JsonValue.Arr)?.items?.map { decodeAnchor(it) }
        ?: throw IllegalArgumentException("missing or non-array field 'anchors'")
    return SavedMeasurement(
        id = obj.stringField("id"),
        name = obj.stringField("name"),
        anchors = anchors,
        createdAtMillis = obj.numberField("createdAtMillis").toLong(),
        note = obj.nullableStringField("note"),
    )
}

/**
 * Decode a version-1 history document. Rejects unknown versions,
 * missing fields and malformed JSON with [IllegalArgumentException] —
 * callers treat that as "this file is not a usable history", never as
 * an empty history.
 */
internal fun decodeHistory(json: String): List<SavedMeasurement> {
    val doc = JsonParser(json).parseDocument() as? JsonValue.Obj
        ?: throw IllegalArgumentException("history document must be an object")
    val version = doc.numberField("version")
    require(version == 1.0) { "unsupported history version: $version" }
    val items = doc.fields["measurements"] as? JsonValue.Arr
        ?: throw IllegalArgumentException("missing or non-array field 'measurements'")
    return items.items.map { decodeMeasurement(it) }
}
