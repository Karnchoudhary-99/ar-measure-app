package measure.demo

import measure.CameraPose
import measure.LengthUnit
import measure.MeasurementSession
import measure.Plane
import measure.SimulatedArSession
import measure.TapResult
import measure.Vec3
import measure.formatLength

/**
 * JVM demo of the camera + AR session layer — no device needed.
 *
 * A camera 3 m above the centre of the 4.20 m x 3.10 m room looks
 * straight down at the floor plane (fitted from the room's corners).
 * Screen taps are computed from the corners themselves: a tap at
 * normalised coordinates ((x - 2.1) / 3, (1.55 - z) / 3) lands exactly
 * on corner (x, z) for this camera, so the demo places real anchors
 * through the same tap path the Android app will use. A final tap
 * outside the floor's fitted disc demonstrates a miss.
 */
fun runArDemo(unit: LengthUnit) {
    val corners = listOf(
        Vec3(0.0, 0.0, 0.0),
        Vec3(4.20, 0.0, 0.0),
        Vec3(4.20, 0.0, 3.10),
        Vec3(0.0, 0.0, 3.10),
    )
    val fit = Plane.fit(corners)
    val plane = fit.plane
    println("AR Measure — tap demo (camera 3 m above the room centre)")
    println("  Floor plane: centroid ${plane.centroid}, normal ${plane.normal}")
    println("  Orientation: ${plane.orientation()}, fit residual ${fit.maxResidual} m")

    val camera = CameraPose(
        position = Vec3(2.1, 3.0, 1.55),
        forward = Vec3(0.0, -1.0, 0.0),
        up = Vec3(0.0, 0.0, -1.0),
    )
    val ar = SimulatedArSession(camera, listOf(plane))
    val session = MeasurementSession()

    val ids = listOf("A", "B", "C", "D")
    corners.forEachIndexed { i, corner ->
        // Exact tap for this corner under this camera (see doc comment).
        val ndcX = (corner.x - 2.1) / 3.0
        val ndcY = (1.55 - corner.z) / 3.0
        when (val result = ar.tap(session, ids[i], ndcX, ndcY, 90.0, 1.0)) {
            is TapResult.Hit -> println(
                "  Tap ${ids[i]} at (${"%.2f".format(ndcX)}, ${"%.2f".format(ndcY)}) " +
                    "-> ${result.anchor.position} (${formatLength(result.distance, unit)} away)",
            )
            is TapResult.Miss -> println("  Tap ${ids[i]} missed: ${result.reason}")
        }
    }
    println("Session: ${session.summarize(unit)}")

    val miss = ar.tap(session, "X", 0.95, 0.95, 90.0, 1.0)
    println("Tap outside the floor disc: $miss (expected a Miss)")
    println("Session after miss: ${session.summarize(unit)}")
}
