package measure

/**
 * A measurement the user chose to keep: a named snapshot of a session's
 * anchors, frozen at record time. The snapshot is a copy — later edits to
 * the live session (undo, new points) never rewrite history.
 *
 * All stored values are in metres / square metres, exactly like the live
 * session; units are a display concern only (see [LengthUnit]). The
 * [createdAtMillis] timestamp is epoch milliseconds, so a saved file
 * sorts and displays identically on any device.
 *
 * @param id unique within a [MeasurementHistory], e.g. "living-room".
 * @param name human label shown in the history list.
 * @param anchors the snapshot, in placement order; may be empty only for
 *   a placeholder the user has not measured yet — real measurements have
 *   at least one anchor.
 */
data class SavedMeasurement(
    val id: String,
    val name: String,
    val anchors: List<Anchor>,
    val createdAtMillis: Long,
    val note: String? = null,
) {
    init {
        require(id.isNotBlank()) { "saved measurement id must not be blank" }
        require(name.isNotBlank()) { "saved measurement name must not be blank" }
        require(createdAtMillis >= 0) { "createdAtMillis must not be negative" }
    }

    /** Snapshot copy: anchors are immutable values, so a list copy suffices. */
    fun snapshot(): SavedMeasurement = copy(anchors = anchors.toList())
}
