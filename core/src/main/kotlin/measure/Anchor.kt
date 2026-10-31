package measure

/**
 * A point the user placed in the AR scene. In a real Android build this
 * wraps an ARCore Anchor (its pose position); here it is just an id plus a
 * world-space position, which keeps the whole measurement core runnable and
 * testable on the JVM with no Android SDK.
 *
 * @param id stable within a session, e.g. "A", "B" — used in result labels.
 * @param position world-space position in metres.
 * @param label optional user label ("sofa corner", "door frame").
 */
data class Anchor(
    val id: String,
    val position: Vec3,
    val label: String? = null,
) {
    init {
        require(id.isNotBlank()) { "anchor id must not be blank" }
    }

    /** Display name: the label when set, otherwise the id. */
    fun displayName(): String = label?.takeIf { it.isNotBlank() } ?: id
}
