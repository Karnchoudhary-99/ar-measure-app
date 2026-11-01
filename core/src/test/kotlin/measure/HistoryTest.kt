package measure

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryTest {

    private fun roomSession(): MeasurementSession {
        val session = MeasurementSession()
        session.place(Anchor("A", Vec3(0.0, 0.0, 0.0)))
        session.place(Anchor("B", Vec3(4.20, 0.0, 0.0)))
        session.place(Anchor("C", Vec3(4.20, 0.0, 3.10)))
        session.place(Anchor("D", Vec3(0.0, 0.0, 3.10)))
        return session
    }

    @Test
    fun `record snapshots the session and lists newest first`() {
        val history = MeasurementHistory()
        val session = roomSession()
        history.record(session, "room", "Living room", createdAtMillis = 1000L)
        history.record(session, "desk", "Desk", createdAtMillis = 2000L)
        assertEquals(listOf("desk", "room"), history.all().map { it.id })
        assertEquals(13.02, history.get("room")!!.area()!!, 1e-9)
    }

    @Test
    fun `later session edits never rewrite a recorded snapshot`() {
        val history = MeasurementHistory()
        val session = roomSession()
        history.record(session, "room", "Living room", createdAtMillis = 1000L)
        session.undo()
        session.clear()
        assertEquals(4, history.get("room")!!.anchors.size)
    }

    @Test
    fun `recording under an existing id replaces it instead of duplicating`() {
        val history = MeasurementHistory()
        val session = roomSession()
        history.record(session, "room", "Living room", createdAtMillis = 1000L)
        history.record(session, "room", "Living room, remeasured", createdAtMillis = 2000L)
        assertEquals(1, history.size())
        assertEquals("Living room, remeasured", history.get("room")!!.name)
        assertEquals(2000L, history.get("room")!!.createdAtMillis)
    }

    @Test
    fun `remove and rename manage entries`() {
        val history = MeasurementHistory()
        val session = roomSession()
        history.record(session, "room", "Living room", createdAtMillis = 1000L)
        assertTrue(history.rename("room", "Lounge"))
        assertEquals("Lounge", history.get("room")!!.name)
        assertEquals(4, history.get("room")!!.anchors.size)
        assertTrue(history.remove("room"))
        assertTrue(history.isEmpty())
    }

    @Test
    fun `remove and rename report false for unknown ids`() {
        val history = MeasurementHistory()
        assertTrue(!history.remove("missing"))
        assertTrue(!history.rename("missing", "Nope"))
    }

    @Test
    fun `search matches name or note case-insensitively, blank returns all`() {
        val history = MeasurementHistory()
        val session = roomSession()
        history.record(session, "room", "Living room", note = "for the new sofa", createdAtMillis = 1000L)
        history.record(session, "desk", "Desk corner", createdAtMillis = 2000L)
        assertEquals(listOf("room"), history.search("SOFA").map { it.id })
        assertEquals(listOf("room"), history.search("living").map { it.id })
        assertEquals(emptyList(), history.search("kitchen").map { it.id })
        assertEquals(2, history.search("  ").size)
    }

    @Test
    fun `get returns null for an unknown id`() {
        assertNull(MeasurementHistory().get("missing"))
        assertTrue(MeasurementHistory().isEmpty())
    }
}
