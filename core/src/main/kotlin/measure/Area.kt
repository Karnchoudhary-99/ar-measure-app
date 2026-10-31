package measure

/**
 * Area of the polygon through [anchors] in order, in square metres.
 *
 * Uses Newell's method: the polygon's area vector is half the sum of the
 * cross products of consecutive vertices, and its length is the area —
 * regardless of how the polygon is tilted in 3D space. That matters for
 * AR: a floor polygon is rarely exactly axis-aligned, and projecting it
 * onto the wrong plane would under-report the area.
 *
 * The polygon is implicitly closed (last vertex connects back to the
 * first), so it needs at least 3 anchors. Degenerate polygons (all points
 * collinear, or all coincident) correctly report 0.0.
 */
fun polygonArea(anchors: List<Anchor>): Double {
    require(anchors.size >= 3) { "polygon area needs at least 3 anchors, got ${anchors.size}" }
    var nx = 0.0
    var ny = 0.0
    var nz = 0.0
    for (i in anchors.indices) {
        val current = anchors[i].position
        val next = anchors[(i + 1) % anchors.size].position
        nx += (current.y - next.y) * (current.z + next.z)
        ny += (current.z - next.z) * (current.x + next.x)
        nz += (current.x - next.x) * (current.y + next.y)
    }
    return Vec3(nx, ny, nz).length() / 2.0
}

/**
 * Perimeter of the closed polygon through [anchors] in order, in metres
 * (path length plus the closing edge back to the first anchor).
 */
fun polygonPerimeter(anchors: List<Anchor>): Double {
    require(anchors.size >= 3) { "polygon perimeter needs at least 3 anchors, got ${anchors.size}" }
    return pathLength(anchors) + distanceBetween(anchors.last(), anchors.first())
}
