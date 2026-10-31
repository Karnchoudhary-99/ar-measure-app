package measure

/**
 * One measuring session: the ordered list of anchors the user placed,
 * plus the results derived from them.
 *
 * Placing an anchor within [duplicateTolerance] metres of the previous
 * anchor is ignored (returns false) — a shaky tap that lands twice on the
 * same corner must not create a zero-length segment. Placing is otherwise
 * always accepted; validation of "enough points for this measurement"
 * lives in the measurement functions themselves.
 */
class MeasurementSession(
    /** Anchors closer than this to the previous one count as duplicates. */
    val duplicateTolerance: Double = 0.01,
) {
    private val _anchors = mutableListOf<Anchor>()

    /** Anchors in placement order; a snapshot, not a live view. */
    val anchors: List<Anchor> get() = _anchors.toList()

    /** Place an anchor. Returns false when it duplicates the previous one. */
    fun place(anchor: Anchor): Boolean {
        val last = _anchors.lastOrNull()
        if (last != null && last.position.distanceTo(anchor.position) < duplicateTolerance) {
            return false
        }
        _anchors.add(anchor)
        return true
    }

    /** Remove the most recently placed anchor; false when there is none. */
    fun undo(): Boolean {
        if (_anchors.isEmpty()) return false
        _anchors.removeAt(_anchors.lastIndex)
        return true
    }

    fun clear() {
        _anchors.clear()
    }

    /** Distances between consecutive anchors, in metres, in order. */
    fun segmentDistances(): List<Double> =
        _anchors.zipWithNext { a, b -> distanceBetween(a, b) }

    /** Total polyline length in metres, or null with fewer than 2 anchors. */
    fun totalLength(): Double? =
        if (_anchors.size >= 2) pathLength(_anchors) else null

    /** Closed-polygon area in m², or null with fewer than 3 anchors. */
    fun area(): Double? =
        if (_anchors.size >= 3) polygonArea(_anchors) else null

    /** Closed-polygon perimeter in metres, or null with fewer than 3 anchors. */
    fun perimeter(): Double? =
        if (_anchors.size >= 3) polygonPerimeter(_anchors) else null

    /** One-line human summary of the session's results. */
    fun summarize(unit: LengthUnit = LengthUnit.METRES): String {
        val parts = mutableListOf<String>()
        parts += "${_anchors.size} point(s)"
        totalLength()?.let { parts += "length ${formatLength(it, unit)}" }
        perimeter()?.let { parts += "perimeter ${formatLength(it, unit)}" }
        area()?.let { parts += "area ${formatArea(it, unit)}" }
        return parts.joinToString(", ")
    }
}
