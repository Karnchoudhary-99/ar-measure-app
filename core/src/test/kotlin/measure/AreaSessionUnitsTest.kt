package measure

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AreaSessionUnitsTest {

    private fun square(side: Double = 2.0): List<Anchor> = listOf(
        Anchor("A", Vec3(0.0, 0.0, 0.0)),
        Anchor("B", Vec3(side, 0.0, 0.0)),
        Anchor("C", Vec3(side, 0.0, side)),
        Anchor("D", Vec3(0.0, 0.0, side)),
    )

    @Test
    fun `square area and perimeter`() {
        val anchors = square(4.20)
        // Rectangle 4.20 x 3.10 is covered by the room demo; here a square.
        assertEquals(4.20 * 4.20, polygonArea(anchors), 1e-9)
        assertEquals(4.0 * 4.20, polygonPerimeter(anchors), 1e-9)
    }

    @Test
    fun `room rectangle area matches length times width`() {
        val anchors = listOf(
            Anchor("A", Vec3(0.0, 0.0, 0.0)),
            Anchor("B", Vec3(4.20, 0.0, 0.0)),
            Anchor("C", Vec3(4.20, 0.0, 3.10)),
            Anchor("D", Vec3(0.0, 0.0, 3.10)),
        )
        assertEquals(13.02, polygonArea(anchors), 1e-9)
        assertEquals(14.60, polygonPerimeter(anchors), 1e-9)
    }

    @Test
    fun `triangle area is half base times height`() {
        val anchors = listOf(
            Anchor("A", Vec3(0.0, 0.0, 0.0)),
            Anchor("B", Vec3(4.0, 0.0, 0.0)),
            Anchor("C", Vec3(0.0, 0.0, 3.0)),
        )
        assertEquals(6.0, polygonArea(anchors), 1e-9)
    }

    @Test
    fun `tilted square keeps its area (Newell, not axis projection)`() {
        // A 2x2 square rotated 45 degrees about the x-axis: still area 4.
        val c = 0.7071067811865476
        val anchors = listOf(
            Anchor("A", Vec3(0.0, 0.0, 0.0)),
            Anchor("B", Vec3(2.0, 0.0, 0.0)),
            Anchor("C", Vec3(2.0, 2.0 * c, 2.0 * c)),
            Anchor("D", Vec3(0.0, 2.0 * c, 2.0 * c)),
        )
        assertEquals(4.0, polygonArea(anchors), 1e-9)
    }

    @Test
    fun `degenerate polygon reports zero area`() {
        val anchors = listOf(
            Anchor("A", Vec3(0.0, 0.0, 0.0)),
            Anchor("B", Vec3(1.0, 0.0, 0.0)),
            Anchor("C", Vec3(2.0, 0.0, 0.0)),
        )
        assertEquals(0.0, polygonArea(anchors), 1e-12)
    }

    @Test
    fun `area needs three anchors`() {
        assertThrows<IllegalArgumentException> {
            polygonArea(listOf(Anchor("A", Vec3(0.0, 0.0, 0.0)), Anchor("B", Vec3(1.0, 0.0, 0.0))))
        }
    }

    @Test
    fun `session derives length, perimeter and area`() {
        val session = MeasurementSession()
        square(2.0).forEach { session.place(it) }
        assertEquals(6.0, session.totalLength()!!, 1e-9)
        assertEquals(8.0, session.perimeter()!!, 1e-9)
        assertEquals(4.0, session.area()!!, 1e-9)
        assertEquals(listOf(2.0, 2.0, 2.0), session.segmentDistances())
    }

    @Test
    fun `session results are null until enough points exist`() {
        val session = MeasurementSession()
        assertNull(session.totalLength())
        assertNull(session.area())
        session.place(Anchor("A", Vec3(0.0, 0.0, 0.0)))
        assertNull(session.totalLength())
        assertNull(session.area())
        session.place(Anchor("B", Vec3(1.0, 0.0, 0.0)))
        assertEquals(1.0, session.totalLength()!!, 1e-12)
        assertNull(session.area())
    }

    @Test
    fun `duplicate tap within tolerance is ignored, undo removes last`() {
        val session = MeasurementSession()
        assertTrue(session.place(Anchor("A", Vec3(0.0, 0.0, 0.0))))
        assertFalse(session.place(Anchor("A2", Vec3(0.005, 0.0, 0.0))))
        assertTrue(session.place(Anchor("B", Vec3(1.0, 0.0, 0.0))))
        assertEquals(2, session.anchors.size)
        assertTrue(session.undo())
        assertEquals(1, session.anchors.size)
        assertTrue(session.undo())
        assertEquals(0, session.anchors.size)
        assertFalse(session.undo())
    }

    @Test
    fun `unit conversions are consistent`() {
        assertEquals(100.0, LengthUnit.CENTIMETRES.fromMetres(1.0), 1e-9)
        assertEquals(3.280839895, LengthUnit.FEET.fromMetres(1.0), 1e-6)
        assertEquals(39.37007874, LengthUnit.INCHES.fromMetres(1.0), 1e-6)
        assertEquals("4.20 m", formatLength(4.2))
        assertEquals("13.78 ft", formatLength(4.2, LengthUnit.FEET))
        assertEquals("13.02 m²", formatArea(13.02))
    }

    @Test
    fun `session summary mentions all results`() {
        val session = MeasurementSession()
        square(2.0).forEach { session.place(it) }
        val summary = session.summarize()
        assertTrue(summary.contains("4 point(s)"))
        assertTrue(summary.contains("length 6.00 m"))
        assertTrue(summary.contains("perimeter 8.00 m"))
        assertTrue(summary.contains("area 4.00 m²"))
    }
}
