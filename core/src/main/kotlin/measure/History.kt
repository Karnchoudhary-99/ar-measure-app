package measure

/**
 * The user's saved measurements, newest first.
 *
 * History is deliberately an in-memory list: persistence lives in
 * [MeasurementStore], so the list itself stays trivially testable and the
 * Android app can back it with Room/DataStore later without changing
 * callers. Ids are unique — recording under an existing id replaces
 * that entry (re-measuring a room updates it in place, it does not
 * duplicate it).
 */
class MeasurementHistory {
    private val entries = mutableListOf<SavedMeasurement>()

    /** All saved measurements, newest first; a snapshot, not a live view. */
    fun all(): List<SavedMeasurement> =
        entries.sortedByDescending { it.createdAtMillis }

    /** Number of saved measurements. */
    fun size(): Int = entries.size

    /** True when nothing has been saved yet. */
    fun isEmpty(): Boolean = entries.isEmpty()

    /** Find a measurement by id, or null. */
    fun get(id: String): SavedMeasurement? = entries.find { it.id == id }

    /**
     * Snapshot [session] under [id] with the given [name].
     * Replaces any existing entry with the same id and returns the saved
     * measurement. [createdAtMillis] defaults to now; tests pass it
     * explicitly for deterministic ordering.
     */
    fun record(
        session: MeasurementSession,
        id: String,
        name: String,
        note: String? = null,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): SavedMeasurement {
        val saved = SavedMeasurement(
            id = id,
            name = name,
            anchors = session.anchors,
            createdAtMillis = createdAtMillis,
            note = note,
        )
        entries.removeAll { it.id == id }
        entries += saved
        return saved
    }

    /** Add an already-built measurement; replaces any entry with its id. */
    fun add(saved: SavedMeasurement): SavedMeasurement {
        entries.removeAll { it.id == saved.id }
        entries += saved
        return saved
    }

    /** Remove the entry with [id]; false when there is none. */
    fun remove(id: String): Boolean = entries.removeAll { it.id == id }

    /**
     * Rename the entry with [id]; false when there is none. Renaming
     * keeps the id, anchors and timestamp — only the display name changes.
     */
    fun rename(id: String, newName: String): Boolean {
        require(newName.isNotBlank()) { "new name must not be blank" }
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0) return false
        entries[index] = entries[index].copy(name = newName)
        return true
    }

    /** Remove every saved measurement. */
    fun clear() {
        entries.clear()
    }
}
