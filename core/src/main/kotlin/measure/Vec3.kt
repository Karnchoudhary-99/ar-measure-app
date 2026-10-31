package measure

import kotlin.math.sqrt

/**
 * A point or vector in 3D space, in metres, in the AR session's world
 * coordinate frame (the same frame ARCore anchors report poses in).
 *
 * Immutable; every operation returns a new instance. Coordinates must be
 * finite — [requireFinite] guards every construction so a NaN from a bad
 * tracking frame can never silently poison a measurement.
 */
data class Vec3(val x: Double, val y: Double, val z: Double) {

    init {
        requireFinite()
    }

    fun requireFinite() {
        require(x.isFinite() && y.isFinite() && z.isFinite()) {
            "Vec3 coordinates must be finite, got ($x, $y, $z)"
        }
    }

    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = Vec3(x * scale, y * scale, z * scale)
    operator fun div(scale: Double): Vec3 {
        require(scale != 0.0 && scale.isFinite()) { "scale must be finite and non-zero" }
        return Vec3(x / scale, y / scale, z / scale)
    }

    infix fun dot(other: Vec3): Double = x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    /** Euclidean length in metres. */
    fun length(): Double = sqrt(this dot this)

    /** Distance to [other] in metres. */
    fun distanceTo(other: Vec3): Double = (this - other).length()

    /** Unit vector, or null for the zero vector (normalising it is undefined). */
    fun normalized(): Vec3? {
        val len = length()
        return if (len == 0.0) null else this / len
    }

    /** True when every coordinate is within [tolerance] metres of [other]. */
    fun isCloseTo(other: Vec3, tolerance: Double = 1e-9): Boolean =
        distanceTo(other) <= tolerance

    companion object {
        val ORIGIN = Vec3(0.0, 0.0, 0.0)
    }
}
