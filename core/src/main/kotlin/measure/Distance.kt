package measure

import kotlin.math.acos

/**
 * Straight-line distance between two anchors, in metres.
 * This is the core "tape measure" operation: two taps, one number.
 */
fun distanceBetween(a: Anchor, b: Anchor): Double =
    a.position.distanceTo(b.position)

/**
 * Total length of the polyline through [anchors] in order, in metres —
 * the "walk the tape around the room" measurement. Needs at least two
 * anchors; a single anchor has no length.
 */
fun pathLength(anchors: List<Anchor>): Double {
    require(anchors.size >= 2) { "path length needs at least 2 anchors, got ${anchors.size}" }
    return anchors.zipWithNext { a, b -> distanceBetween(a, b) }.sum()
}

/**
 * Interior angle at [vertex] between the segments vertex→[a] and
 * vertex→[b], in degrees in [0, 180]. Returns null when either segment
 * has zero length (the angle is undefined at a coincident point).
 */
fun angleAt(vertex: Anchor, a: Anchor, b: Anchor): Double? {
    val v1 = a.position - vertex.position
    val v2 = b.position - vertex.position
    val n1 = v1.normalized() ?: return null
    val n2 = v2.normalized() ?: return null
    // Clamp: floating-point rounding can push the cosine just outside [-1, 1].
    val cos = (n1 dot n2).coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cos))
}
