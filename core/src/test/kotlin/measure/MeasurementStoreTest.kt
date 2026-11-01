package measure

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeasurementStoreTest {

    private fun room() = SavedMeasurement(
        "room",
        "Living \"room\" \\ corner",
        listOf(
            Anchor("A", Vec3(0.0, 0.0, 0.0), "south-west"),
            Anchor("B", Vec3(4.20, 0.0, 0.0), null),
        ),
        1_700_000_000_000L,
        note = "line1\nline2",
    )

    @Test
    fun `encode escapes quotes backslashes and newlines`() {
        val json = encodeMeasurement(room())
        assertTrue(json.contains("\"name\":\"Living \\\"room\\\" \\\\ corner\""))
        assertTrue(json.contains("\"note\":\"line1\\nline2\""))
        assertTrue(json.contains("\"label\":null"))
        assertTrue(json.contains("\"x\":4.2"))
    }

    @Test
    fun `encodeHistory wraps measurements in a versioned document`() {
        val json = encodeHistory(listOf(room()))
        assertTrue(json.startsWith("{\"version\":1,\"measurements\":["))
        assertTrue(json.endsWith("]}"))
        assertEquals(1, json.split("\"id\":\"room\"").size - 1)
    }

    @Test
    fun `decode round-trips encode exactly, including escapes`() {
        val original = room()
        val decoded = decodeHistory(encodeHistory(listOf(original)))
        assertEquals(listOf(original), decoded)
        assertEquals("Living \"room\" \\ corner", decoded[0].name)
        assertEquals("line1\nline2", decoded[0].note)
        assertEquals("south-west", decoded[0].anchors[0].label)
    }

    @Test
    fun `decode rejects unknown versions, bad json and missing fields`() {
        assertThrows<IllegalArgumentException> {
            decodeHistory("{\"version\":2,\"measurements\":[]}")
        }
        assertThrows<IllegalArgumentException> {
            decodeHistory("{\"version\":1,\"measurements\":[")
        }
        assertThrows<IllegalArgumentException> {
            decodeHistory("{\"version\":1,\"measurements\":[{\"id\":\"x\"}]}")
        }
        assertThrows<IllegalArgumentException> {
            decodeHistory("{\"version\":1,\"measurements\":[]} trailing")
        }
    }

    @Test
    fun `save then load round-trips through a real file`() {
        val dir = java.nio.file.Files.createTempDirectory("ar-measure-history")
        val path = dir.resolve("history.json")
        saveHistory(path, listOf(room()))
        assertEquals(listOf(room()), loadHistory(path))
        // No temp files left behind by the atomic move.
        assertEquals(listOf("history.json"), dir.toFile().list()!!.toList())
    }

    @Test
    fun `load of a missing file is empty, corrupt file throws`() {
        val dir = java.nio.file.Files.createTempDirectory("ar-measure-history")
        assertEquals(emptyList(), loadHistory(dir.resolve("missing.json")))
        val corrupt = dir.resolve("corrupt.json")
        java.nio.file.Files.writeString(corrupt, "{\"version\":1,")
        assertThrows<IllegalArgumentException> { loadHistory(corrupt) }
    }

    @Test
    fun `encodeHistory of an empty list is a valid empty document`() {
        assertEquals(
            "{\"version\":1,\"measurements\":[]}",
            encodeHistory(emptyList()),
        )
    }
}
