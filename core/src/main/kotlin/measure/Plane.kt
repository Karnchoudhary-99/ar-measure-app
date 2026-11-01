package measure

import kotlin.math.abs

/**
 * How a detected plane sits in the world. A floor or tabletop is
 * [HORIZONTAL] (normal near vertical); a wall is [VERTICAL] (normal near
 * horizontal); anything in between — a ramp, a tilted board — is
 * [INCLINED]. The bands are 45° around vertical for horizontal and 15°
 * around horizontal for vertical, so a plane is never ambiguously both.
 */
enum class PlaneOrientation {
    HORIZONTAL,
    VERTICAL,
    INCLINED,
}

/**
 * A detected AR plane: an infinite geometric plane plus the radius of the
 * point cloud it was fitted from, which bounds hit-testing (see
 * [contains]). On Android this mirrors an ARCore Plane; here it is pure
 * data so the whole pipeline runs on the JVM.
 *
 * @param centroid mean of the fitted points, in metres.
 * @param normal unit normal. Its sign is arbitrary — it depends on point
 *   order — so callers must use [distanceTo] magnitudes, never the sign.
 * @param extentRadius distance from [centroid] to the farthest fitted
 *   point; hit tests accept points inside this disc.
 */
data class Plane(
    val centroid: Vec3,
    val normal: Vec3,
    val extentRadius: Double,
) {
    init {
        require(normal.length() in 0.999999..1.000001) {
            "plane normal must be a unit vector, got length ${normal.length()}"
        }
        require(extentRadius >= 0.0 && extentRadius.isFinite()) {
            "extent radius must be finite and non-negative, got $extentRadius"
        }
    }

    /** Signed distance from [point] to the plane, in metres. */
    fun distanceTo(point: Vec3): Double = (point - centroid) dot normal

    /** Closest point on the plane to [point]. */
    fun project(point: Vec3): Vec3 = point - normal * distanceTo(point)

    /** Orientation band of this plane (see [PlaneOrientation]). */
    fun orientation(): PlaneOrientation {
        val verticality = abs(normal.y)
        return when {
            verticality >= 0.7071067811865476 -> PlaneOrientation.HORIZONTAL
            verticality <= 0.25881904510252074 -> PlaneOrientation.VERTICAL
            else -> PlaneOrientation.INCLINED
        }
    }

    /**
     * True when [point] lies on the plane (within [tolerance] metres) and
     * inside the fitted disc. The disc is an approximation — a real plane
     * is a polygon, not a circle — but it rejects the common failure of
     * tapping "through" a small table onto the floor behind it.
     */
    fun contains(point: Vec3, tolerance: Double = 0.01): Boolean {
        require(tolerance >= 0.0) { "tolerance must be non-negative" }
        if (abs(distanceTo(point)) > tolerance) return false
        return project(point).distanceTo(centroid) <= extentRadius
    }

    companion object {
        /**
         * Fit a plane to [points] (at least 3, not all collinear).
         *
         * The normal comes from the two most spread-out directions in the
         * cloud: the point farthest from the centroid, and the point
         * farthest from the line through the centroid and that point.
         * This is deterministic (ties break toward the earliest point)
         * and needs no iterative solver, which keeps it dependency-free.
         * [PlaneFit.maxResidual] tells the caller how planar the cloud
         * actually was — a large residual means the "plane" is suspect
         * (e.g. points sampled across a table edge and the floor).
         */
        fun fit(points: List<Vec3>): PlaneFit {
            require(points.size >= 3) {
                "plane fit needs at least 3 points, got ${points.size}"
            }
            val n = points.size.toDouble()
            val centroid = Vec3(
                points.sumOf { it.x } / n,
                points.sumOf { it.y } / n,
                points.sumOf { it.z } / n,
            )
            val first = points.maxByOrNull { it.distanceTo(centroid) }
                ?: throw IllegalArgumentException("empty point cloud")
            val lineDir = (first - centroid).normalized()
                ?: throw IllegalArgumentException(
                    "cannot fit a plane to coincident points",
                )
            val second = points.maxByOrNull { p ->
                ((p - centroid) cross lineDir).length()
            } ?: throw IllegalArgumentException("empty point cloud")
            val spread = (second - centroid) cross lineDir
            if (spread.length() < 1e-9) {
                throw IllegalArgumentException(
                    "cannot fit a plane to collinear points",
                )
            }
            val normal = ((first - centroid) cross (second - centroid))
                .normalized()
                ?: throw IllegalArgumentException("degenerate point cloud")
            val extent = points.maxOf { it.distanceTo(centroid) }
            val plane = Plane(centroid, normal, extent)
            val residuals = points.map { abs(plane.distanceTo(it)) }
            return PlaneFit(
                plane = plane,
                maxResidual = residuals.max(),
                meanResidual = residuals.sum() / residuals.size,
            )
        }
    }
}

/** Result of [Plane.fit]: the plane plus how well the points fit it. */
data class PlaneFit(
    val plane: Plane,
    /** Largest absolute distance of any fitted point from the plane. */
    val maxResidual: Double,
    /** Mean absolute distance of the fitted points from the plane. */
    val meanResidual: Double,
)
