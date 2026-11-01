package measure

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end workflow test: the exact loop the Android history screen
 * will run — measure, record, rename, save, reload into a fresh
 * history, search — must round-trip without losing a point or a note.
 */
class HistoryWorkflowTest {

    @Test
    fun `measure record save reload search round-trips`() {
        val session = MeasurementSession()
        session.place(Anchor("A", Vec3(0.0, 0.0, 0.0)))
        session.place(Anchor("B", Vec3(4.20, 0.0, 0.0)))
        session.place(Anchor("C", Vec3(4.20, 0.0, 3.10)))
        session.place(Anchor("D", Vec3(0.0, 0.0, 3.10)))

        val history = MeasurementHistory()
        history.record(session, "room", "Living room", note = "for the new sofa", createdAtMillis = 1000L)
        history.rename("room", "Lounge")

        val file = Files.createTempDirectory("ar-measure-workflow").resolve("history.json")
        saveHistory(file, history.all())

        val reloaded = MeasurementHistory()
        loadHistory(file).forEach { reloaded.add(it) }
        assertEquals(1, reloaded.size())
        val entry = reloaded.get("room")!!
        assertEquals("Lounge", entry.name)
        assertEquals("for the new sofa", entry.note)
        assertEquals(4, entry.anchors.size)
        assertEquals(13.02, entry.area()!!, 1e-9)
        assertEquals(listOf("room"), reloaded.search("sofa").map { it.id })
        assertTrue(reloaded.search("kitchen").isEmpty())
    }
}
