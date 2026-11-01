package measure

import org.junit.jupiter.api.Test
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
    fun `encodeHistory of an empty list is a valid empty document`() {
        assertEquals(
            "{\"version\":1,\"measurements\":[]}",
            encodeHistory(emptyList()),
        )
    }
}
