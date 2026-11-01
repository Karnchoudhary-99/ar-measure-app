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
