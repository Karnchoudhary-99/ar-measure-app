package measure

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedMeasurementTest {

    private fun room(): List<Anchor> = listOf(
        Anchor("A", Vec3(0.0, 0.0, 0.0)),
        Anchor("B", Vec3(4.20, 0.0, 0.0)),
        Anchor("C", Vec3(4.20, 0.0, 3.10)),
        Anchor("D", Vec3(0.0, 0.0, 3.10)),
    )

    private fun saved(anchors: List<Anchor> = room()) =
        SavedMeasurement("room", "Living room", anchors, 1_700_000_000_000L)

    @Test
    fun `room snapshot derives length perimeter and area`() {
        val m = saved()
        assertEquals(11.50, m.totalLength()!!, 1e-9)
        assertEquals(14.60, m.perimeter()!!, 1e-9)
        assertEquals(13.02, m.area()!!, 1e-9)
        assertEquals(listOf(4.20, 3.10, 4.20), m.segmentDistances())
    }

    @Test
    fun `single anchor has no derived metrics`() {
        val m = saved(listOf(Anchor("A", Vec3(0.0, 0.0, 0.0))))
        assertNull(m.totalLength())
        assertNull(m.perimeter())
        assertNull(m.area())
        assertTrue(m.segmentDistances().isEmpty())
    }

    @Test
    fun `summary mirrors the session summary format`() {
        val summary = saved().summarize()
        assertTrue(summary.contains("\"Living room\""))
        assertTrue(summary.contains("length 11.50 m"))
        assertTrue(summary.contains("perimeter 14.60 m"))
        assertTrue(summary.contains("area 13.02 m²"))
    }

    @Test
    fun `summary in feet converts at display time only`() {
        val summary = saved().summarize(LengthUnit.FEET)
        assertTrue(summary.contains("ft"))
        // Stored values stay in metres even after a feet summary.
        assertEquals(13.02, saved().area()!!, 1e-9)
    }

    @Test
    fun `blank id name and negative timestamp are rejected`() {
        assertThrows<IllegalArgumentException> {
            SavedMeasurement("", "Room", room(), 0L)
        }
        assertThrows<IllegalArgumentException> {
            SavedMeasurement("room", "  ", room(), 0L)
        }
        assertThrows<IllegalArgumentException> {
            SavedMeasurement("room", "Room", room(), -1L)
        }
    }
}
