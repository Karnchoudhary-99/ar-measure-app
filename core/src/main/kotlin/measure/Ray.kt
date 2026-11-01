package measure

import kotlin.math.abs

/**
 * A ray in world space: an [origin] plus a unit [direction]. Produced by
 * a camera for a screen tap (see [CameraPose.rayThrough]) and tested
 * against planes to find where the user tapped in the world.
 */
data class Ray(val origin: Vec3, val direction: Vec3) {
    init {
        require(direction.length() in 0.999999..1.000001) {
            "ray direction must be a unit vector, got length ${direction.length()}"
        }
    }

    /**
     * Intersection with [plane], or null when the ray is parallel to the
     * plane or the intersection lies behind the origin. The hit point is
     * exact on the infinite plane — whether it is inside the detected
     * region is [Plane.contains]'s job, not this function's.
     */
    fun intersect(plane: Plane): Vec3? {
        val denom = direction dot plane.normal
        if (abs(denom) < 1e-12) return null // parallel: no single hit
        val t = (plane.centroid - origin) dot plane.normal / denom
        if (t < 0.0) return null // behind the camera
        return origin + direction * t
    }
}
