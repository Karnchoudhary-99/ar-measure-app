package measure.demo

import measure.Anchor
import measure.LengthUnit
import measure.MeasurementSession
import measure.Vec3
import measure.angleAt
import measure.formatLength

/**
 * JVM demo of the measurement core — no Android device needed.
 *
 * Simulates placing anchors on the four corners of a room floor plus a
 * diagonal, then prints every derived measurement. In the Android app the
 * same [MeasurementSession] is fed by real ARCore anchor poses; nothing
 * in the core changes.
 */
fun main(args: Array<String>) {
    val unit = if (args.contains("--feet")) LengthUnit.FEET else LengthUnit.METRES
    if (args.contains("--ar")) {
        runArDemo(unit)
        return
    }
    val session = MeasurementSession()
    // A 4.20 m x 3.10 m room, corners on the floor plane (y = 0).
    val corners = listOf(
        Anchor("A", Vec3(0.0, 0.0, 0.0), "south-west corner"),
        Anchor("B", Vec3(4.20, 0.0, 0.0), "south-east corner"),
        Anchor("C", Vec3(4.20, 0.0, 3.10), "north-east corner"),
        Anchor("D", Vec3(0.0, 0.0, 3.10), "north-west corner"),
    )
    corners.forEach { session.place(it) }

    println("AR Measure — room demo (${corners.size} anchors placed)")
    session.anchors.forEach { anchor ->
        val p = anchor.position
        println("  ${anchor.id} ${anchor.displayName()}: (${p.x}, ${p.y}, ${p.z}) m")
    }
    println("Segments:")
    session.segmentDistances().forEachIndexed { i, d ->
        val from = session.anchors[i].id
        val to = session.anchors[i + 1].id
        println("  $from -> $to: ${formatLength(d, unit)}")
    }
    println("Session: ${session.summarize(unit)}")

    val cornerB = corners[1]
    val angle = angleAt(cornerB, corners[0], corners[2])
    println("Angle at B (should be ~90 deg): ${angle?.let { "%.1f deg".format(it) }}")

    // A duplicate tap on corner D is ignored, not measured twice.
    val duplicate = session.place(Anchor("D2", Vec3(0.005, 0.0, 3.10)))
    println("Duplicate tap near D accepted? $duplicate (expected false)")
    println("Session after duplicate tap: ${session.summarize(unit)}")
}
