package measure

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArSessionTest {

    private val floorCorners = listOf(
        Vec3(0.0, 0.0, 0.0),
        Vec3(1.0, 0.0, 0.0),
        Vec3(1.0, 0.0, 1.0),
        Vec3(0.0, 0.0, 1.0),
    )

    private fun floorPlane() = Plane.fit(floorCorners).plane

    private fun downCamera(height: Double = 2.0) = CameraPose(
        position = Vec3(0.5, height, 0.5),
        forward = Vec3(0.0, -1.0, 0.0),
        up = Vec3(0.0, 0.0, -1.0),
    )

    @Test
    fun `fit recovers a floor plane from its corners`() {
        val fit = Plane.fit(floorCorners)
        assertEquals(Vec3(0.5, 0.0, 0.5), fit.plane.centroid)
        assertEquals(1.0, kotlin.math.abs(fit.plane.normal.y), 1e-9)
        assertEquals(PlaneOrientation.HORIZONTAL, fit.plane.orientation())
        assertEquals(0.0, fit.maxResidual, 1e-9)
        assertEquals(sqrt(0.5), fit.plane.extentRadius, 1e-9)
    }

    @Test
    fun `fit recovers a tilted plane and calls it inclined`() {
        // Points on x + y + z = 0: normal (1, 1, 1) / sqrt(3).
        val points = listOf(
            Vec3(0.0, 0.0, 0.0),
            Vec3(1.0, -1.0, 0.0),
            Vec3(1.0, 0.0, -1.0),
            Vec3(0.0, 1.0, -1.0),
        )
        val fit = Plane.fit(points)
        val expected = 1.0 / sqrt(3.0)
        assertEquals(expected, kotlin.math.abs(fit.plane.normal.x), 1e-9)
        assertEquals(expected, kotlin.math.abs(fit.plane.normal.y), 1e-9)
        assertEquals(expected, kotlin.math.abs(fit.plane.normal.z), 1e-9)
        assertEquals(PlaneOrientation.INCLINED, fit.plane.orientation())
        assertEquals(0.0, fit.maxResidual, 1e-9)
    }

    @Test
    fun `fit reports a non-planar cloud through its residual`() {
        // Three floor corners plus one point 0.5 m above the floor.
        val points = floorCorners.take(3) + Vec3(0.0, 0.5, 1.0)
        val fit = Plane.fit(points)
        assertTrue(fit.maxResidual > 0.1, "expected a visible residual, got ${fit.maxResidual}")
    }

    @Test
    fun `fit rejects too few collinear or coincident points`() {
        assertThrows<IllegalArgumentException> {
            Plane.fit(listOf(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0)))
        }
        assertThrows<IllegalArgumentException> {
            Plane.fit(listOf(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0), Vec3(2.0, 0.0, 0.0)))
        }
        assertThrows<IllegalArgumentException> {
            Plane.fit(listOf(Vec3(1.0, 1.0, 1.0), Vec3(1.0, 1.0, 1.0), Vec3(1.0, 1.0, 1.0)))
        }
    }

    @Test
    fun `plane distance project and contains behave`() {
        val plane = Plane(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), 5.0)
        assertEquals(2.0, plane.distanceTo(Vec3(0.0, 2.0, 0.0)), 1e-9)
        assertEquals(Vec3(3.0, 0.0, 0.0), plane.project(Vec3(3.0, 2.0, 0.0)))
        assertTrue(plane.contains(Vec3(3.0, 0.0, 0.0)))
        assertTrue(plane.contains(Vec3(0.0, 0.005, 0.0)))
        assertTrue(!plane.contains(Vec3(0.0, 0.02, 0.0)))
        assertTrue(!plane.contains(Vec3(6.0, 0.0, 0.0)))
    }

    @Test
    fun `a wall normal is vertical orientation`() {
        val wall = Plane(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0), 3.0)
        assertEquals(PlaneOrientation.VERTICAL, wall.orientation())
    }

    @Test
    fun `ray hits the floor misses when parallel and ignores behind`() {
        val floor = floorPlane()
        val down = Ray(Vec3(0.5, 1.0, 0.5), Vec3(0.0, -1.0, 0.0))
        assertEquals(Vec3(0.5, 0.0, 0.5), down.intersect(floor))
        val sideways = Ray(Vec3(0.5, 1.0, 0.5), Vec3(1.0, 0.0, 0.0))
        assertNull(sideways.intersect(floor))
        val up = Ray(Vec3(0.5, 1.0, 0.5), Vec3(0.0, 1.0, 0.0))
        assertNull(up.intersect(floor))
    }

    @Test
    fun `camera centre ray looks straight down and edge ray tilts`() {
        val camera = downCamera()
        assertEquals(Vec3(1.0, 0.0, 0.0), camera.right)
        val centre = camera.rayThrough(0.0, 0.0, 90.0, 1.0)
        assertEquals(Vec3(0.0, -1.0, 0.0), centre.direction)
        val edge = camera.rayThrough(1.0, 0.0, 90.0, 1.0)
        val s = 1.0 / sqrt(2.0)
        assertEquals(s, edge.direction.x, 1e-9)
        assertEquals(-s, edge.direction.y, 1e-9)
        assertEquals(0.0, edge.direction.z, 1e-9)
    }

    @Test
    fun `camera rejects bad poses and bad screen points`() {
        assertThrows<IllegalArgumentException> {
            CameraPose(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0))
        }
        assertThrows<IllegalArgumentException> {
            CameraPose(Vec3(0.0, 0.0, 0.0), Vec3(0.0, -1.0, 0.0), Vec3(0.0, 1.0, 0.0))
        }
        val camera = downCamera()
        assertThrows<IllegalArgumentException> {
            camera.rayThrough(1.5, 0.0, 90.0, 1.0)
        }
        assertThrows<IllegalArgumentException> {
            camera.rayThrough(0.0, 0.0, 0.0, 1.0)
        }
    }

    @Test
    fun `centre tap hits the floor centre and corner tap hits the corner`() {
        val session = SimulatedArSession(downCamera(), listOf(floorPlane()))
        val centre = session.hitTest(downCamera().rayThrough(0.0, 0.0, 90.0, 1.0))
        assertEquals(Vec3(0.5, 0.0, 0.5), centre!!.point)
        assertEquals(2.0, centre.distance, 1e-9)
        // Corner (0, 0, 0) seen from (0.5, 2, 0.5) at 90 deg FOV.
        // Tolerance, not exact equality: the ray math leaves ~1e-17 noise.
        val corner = session.hitTest(downCamera().rayThrough(-0.25, 0.25, 90.0, 1.0))
        assertTrue(corner!!.point.isCloseTo(Vec3(0.0, 0.0, 0.0), 1e-9))
    }

    @Test
    fun `a table in front of the floor occludes it`() {
        val table = Plane(Vec3(0.5, 1.0, 0.5), Vec3(0.0, 1.0, 0.0), 0.3)
        val session = SimulatedArSession(
            downCamera(),
            listOf(floorPlane(), table),
        )
        val hit = session.hitTest(downCamera().rayThrough(0.0, 0.0, 90.0, 1.0))
        assertEquals(1.0, hit!!.plane.centroid.y, 1e-9)
        assertEquals(1.0, hit.distance, 1e-9)
    }

    @Test
    fun `tap past a planes edge misses and tap places anchors in session`() {
        val session = SimulatedArSession(downCamera(), listOf(floorPlane()))
        val measuring = MeasurementSession()
        val miss = session.tap(measuring, "X", 0.95, 0.95, 90.0, 1.0)
        assertTrue(miss is TapResult.Miss, "expected a miss, got $miss")
        assertEquals(0, measuring.anchors.size)

        val first = session.tap(measuring, "A", 0.0, 0.0, 90.0, 1.0)
        assertTrue(first is TapResult.Hit, "expected a hit, got $first")
        val second = session.tap(measuring, "B", -0.25, 0.25, 90.0, 1.0)
        assertTrue(second is TapResult.Hit, "expected a hit, got $second")
        assertEquals(2, measuring.anchors.size)
        // Centre (0.5, 0, 0.5) to corner (0, 0, 0): sqrt(0.5).
        assertEquals(sqrt(0.5), measuring.totalLength()!!, 1e-9)

        // Same tap again: the hit duplicates the previous anchor.
        val duplicate = session.tap(measuring, "C", -0.25, 0.25, 90.0, 1.0)
        assertTrue(duplicate is TapResult.Miss, "expected a duplicate miss, got $duplicate")
        assertEquals(2, measuring.anchors.size)
    }
}
