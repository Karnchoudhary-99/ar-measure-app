package measure

/**
 * One detected plane hit by a tap ray: the plane, the exact world-space
 * hit point, and the distance from the camera in metres.
 */
data class PlaneHit(
    val plane: Plane,
    val point: Vec3,
    val distance: Double,
)

/** Outcome of tapping the AR view to place an anchor. */
sealed interface TapResult {
    /** The tap landed on a plane and produced an anchor. */
    data class Hit(val anchor: Anchor, val plane: Plane, val distance: Double) : TapResult

    /** The tap placed nothing; [reason] says why (no plane, duplicate…). */
    data class Miss(val reason: String) : TapResult
}

/**
 * A simulated AR session: a camera plus the planes it has detected so
 * far. On Android the planes come from ARCore plane detection and the
 * camera from the AR frame; here both are supplied explicitly, which is
 * what makes tap placement testable on the JVM.
 *
 * A tap ray is tested against every plane and the nearest hit inside a
 * plane's fitted disc wins — the same "closest surface wins" rule a real
 * AR hit test applies, so a table in front of a wall correctly occludes
 * the wall behind it.
 */
class SimulatedArSession(
    val camera: CameraPose,
    val planes: List<Plane>,
) {
    /**
     * Nearest plane hit for [ray] within [maxDistance] metres, or null.
     * A geometric intersection outside a plane's fitted disc does not
     * count — the ray passed the plane's edge, not the surface.
     */
    fun hitTest(ray: Ray, maxDistance: Double = 10.0): PlaneHit? {
        require(maxDistance > 0.0 && maxDistance.isFinite()) {
            "maxDistance must be finite and positive, got $maxDistance"
        }
        var best: PlaneHit? = null
        for (plane in planes) {
            val point = ray.intersect(plane) ?: continue
            if (!plane.contains(point)) continue
            val distance = ray.origin.distanceTo(point)
            if (distance > maxDistance) continue
            if (best == null || distance < best.distance) {
                best = PlaneHit(plane, point, distance)
            }
        }
        return best
    }

    /**
     * Place an anchor in [session] from a screen tap at ([ndcX], [ndcY]).
     * Returns [TapResult.Miss] when the tap hits no plane, or when the
     * hit duplicates the session's previous anchor (the session's own
     * duplicate guard — a shaky double-tap must not add a point).
     */
    fun tap(
        session: MeasurementSession,
        id: String,
        ndcX: Double,
        ndcY: Double,
        verticalFovDegrees: Double = 60.0,
        aspect: Double = 1.0,
    ): TapResult {
        val ray = camera.rayThrough(ndcX, ndcY, verticalFovDegrees, aspect)
        val hit = hitTest(ray)
            ?: return TapResult.Miss("tap hit no detected plane")
        val anchor = Anchor(id, hit.point)
        if (!session.place(anchor)) {
            return TapResult.Miss(
                "tap duplicates the previous anchor (within ${session.duplicateTolerance} m)",
            )
        }
        return TapResult.Hit(anchor, hit.plane, hit.distance)
    }
}
