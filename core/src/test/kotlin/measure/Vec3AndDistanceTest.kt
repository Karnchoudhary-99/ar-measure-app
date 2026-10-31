package measure

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Vec3AndDistanceTest {

    @Test
    fun `3-4-5 triangle distance`() {
        val a = Anchor("A", Vec3(0.0, 0.0, 0.0))
        val b = Anchor("B", Vec3(3.0, 4.0, 0.0))
        assertEquals(5.0, distanceBetween(a, b), 1e-9)
    }

    @Test
    fun `distance is symmetric and zero for identical points`() {
        val a = Anchor("A", Vec3(1.0, 2.0, 3.0))
        val b = Anchor("B", Vec3(4.0, 6.0, 3.0))
        assertEquals(distanceBetween(a, b), distanceBetween(b, a), 1e-12)
        assertEquals(0.0, distanceBetween(a, a), 1e-12)
    }

    @Test
    fun `distance in 3D uses all axes`() {
        val a = Anchor("A", Vec3(0.0, 0.0, 0.0))
        val b = Anchor("B", Vec3(1.0, 2.0, 2.0))
        assertEquals(3.0, distanceBetween(a, b), 1e-9)
    }

    @Test
    fun `path length sums segments`() {
        val anchors = listOf(
            Anchor("A", Vec3(0.0, 0.0, 0.0)),
            Anchor("B", Vec3(3.0, 0.0, 0.0)),
            Anchor("C", Vec3(3.0, 4.0, 0.0)),
        )
        assertEquals(7.0, pathLength(anchors), 1e-9)
    }

    @Test
    fun `path length needs two anchors`() {
        assertThrows<IllegalArgumentException> {
            pathLength(listOf(Anchor("A", Vec3(0.0, 0.0, 0.0))))
        }
    }

    @Test
    fun `right angle measures 90 degrees`() {
        val vertex = Anchor("B", Vec3(1.0, 0.0, 0.0))
        val a = Anchor("A", Vec3(0.0, 0.0, 0.0))
        val c = Anchor("C", Vec3(1.0, 1.0, 0.0))
        assertEquals(90.0, angleAt(vertex, a, c)!!, 1e-9)
    }

    @Test
    fun `straight angle measures 180 degrees`() {
        val vertex = Anchor("B", Vec3(1.0, 0.0, 0.0))
        val a = Anchor("A", Vec3(0.0, 0.0, 0.0))
        val c = Anchor("C", Vec3(2.0, 0.0, 0.0))
        assertEquals(180.0, angleAt(vertex, a, c)!!, 1e-9)
    }

    @Test
    fun `angle at a coincident point is null`() {
        val vertex = Anchor("B", Vec3(1.0, 0.0, 0.0))
        assertNull(angleAt(vertex, vertex, Anchor("C", Vec3(2.0, 0.0, 0.0))))
    }

    @Test
    fun `Vec3 ops: add, dot, cross, normalize`() {
        val v = Vec3(1.0, 0.0, 0.0)
        val w = Vec3(0.0, 1.0, 0.0)
        assertEquals(Vec3(1.0, 1.0, 0.0), v + w)
        assertEquals(0.0, v dot w, 1e-12)
        assertEquals(Vec3(0.0, 0.0, 1.0), v cross w)
        assertEquals(1.0, Vec3(3.0, 4.0, 0.0).normalized()!!.length(), 1e-12)
        assertNull(Vec3.ORIGIN.normalized())
    }

    @Test
    fun `Vec3 rejects NaN and infinite coordinates`() {
        assertThrows<IllegalArgumentException> { Vec3(Double.NaN, 0.0, 0.0) }
        assertThrows<IllegalArgumentException> { Vec3(0.0, Double.POSITIVE_INFINITY, 0.0) }
    }

    @Test
    fun `cross product magnitude equals parallelogram area`() {
        val v = Vec3(3.0, 0.0, 0.0)
        val w = Vec3(0.0, 4.0, 0.0)
        assertEquals(12.0, (v cross w).length(), 1e-9)
        assertEquals(sqrt(2.0), Vec3(1.0, 1.0, 0.0).length(), 1e-12)
    }

    @Test
    fun `anchor requires a non-blank id`() {
        assertThrows<IllegalArgumentException> { Anchor("  ", Vec3(0.0, 0.0, 0.0)) }
        assertEquals("corner", Anchor("A", Vec3(0.0, 0.0, 0.0), "corner").displayName())
        assertEquals("A", Anchor("A", Vec3(0.0, 0.0, 0.0)).displayName())
    }

    @Test
    fun `isCloseTo honours tolerance`() {
        val a = Vec3(0.0, 0.0, 0.0)
        assertTrue(a.isCloseTo(Vec3(0.005, 0.0, 0.0), tolerance = 0.01))
        assertTrue(!a.isCloseTo(Vec3(0.02, 0.0, 0.0), tolerance = 0.01))
    }
}
