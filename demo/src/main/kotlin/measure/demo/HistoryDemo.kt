package measure.demo

import measure.Anchor
import measure.LengthUnit
import measure.MeasurementHistory
import measure.MeasurementSession
import measure.Vec3
import measure.loadHistory
import measure.saveHistory
import java.nio.file.Files

/**
 * JVM demo of measurement history — no device needed.
 *
 * Records the same 4.20 m × 3.10 m room into a history, renames it,
 * saves it to a temp file, reloads it into a fresh history, and searches
 * it — the full save / reload / find loop the Android history screen
 * will use, with the file store standing in for on-device storage.
 */
fun runHistoryDemo(unit: LengthUnit) {
    val session = MeasurementSession()
    listOf(
        Anchor("A", Vec3(0.0, 0.0, 0.0), "south-west corner"),
        Anchor("B", Vec3(4.20, 0.0, 0.0), "south-east corner"),
        Anchor("C", Vec3(4.20, 0.0, 3.10), "north-east corner"),
        Anchor("D", Vec3(0.0, 0.0, 3.10), "north-west corner"),
    ).forEach { session.place(it) }

    println("AR Measure — history demo")
    val history = MeasurementHistory()
    val saved = history.record(session, "room", "Living room", note = "for the new sofa")
    println("  Recorded: ${saved.summarize(unit)}")
    history.rename("room", "Lounge")
    println("  Renamed: ${history.get("room")!!.summarize(unit)}")

    val file = Files.createTempFile("ar-measure-history", ".json")
    saveHistory(file, history.all())
    println("  Saved ${history.size()} measurement(s) to ${file.fileName}")
    val reloaded = MeasurementHistory()
    loadHistory(file).forEach { reloaded.add(it) }
    println("  Reloaded: ${reloaded.all().joinToString { it.summarize(unit) }}")
    println("  Search \"sofa\": ${reloaded.search("sofa").map { it.name }}")
    println("  Search \"kitchen\": ${reloaded.search("kitchen").map { it.name }} (expected empty)")
    Files.deleteIfExists(file)
}
