package measure

import kotlin.math.tan

/**
 * A camera pose in world space: where the phone is and where it looks.
 * On Android this is fed by the ARCore camera pose each frame; here it is
 * pure data, so tap-to-world placement is testable without a device.
 *
 * @param position world-space camera position, in metres.
 * @param forward unit vector the camera looks along.
 * @param up unit vector pointing to the top of the screen. Must not be
 *   parallel to [forward]; it is re-orthogonalised against [forward],
 *   so a slightly tilted input still yields an exact right-handed basis.
 */
class CameraPose(
    val position: Vec3,
    forward: Vec3,
    up: Vec3,
) {
    /** Unit vector the camera looks along. */
    val forward: Vec3 = forward.normalized()
        ?: throw IllegalArgumentException("camera forward must be non-zero")

    /** Unit vector to the top of the screen, orthogonal to [forward]. */
    val up: Vec3

    /** Unit vector to the right of the screen. */
    val right: Vec3

    init {
        val upNorm = up.normalized()
            ?: throw IllegalArgumentException("camera up must be non-zero")
        val rightVec = (this.forward cross upNorm).normalized()
            ?: throw IllegalArgumentException(
                "camera up must not be parallel to forward",
            )
        right = rightVec
        // Re-orthogonalise: right x forward is exactly perpendicular to
        // forward and points screen-up (up to the original up's sign).
        this.up = (rightVec cross this.forward).let { reUp ->
            if ((reUp dot upNorm) < 0.0) reUp * -1.0 else reUp
        }
    }

    /**
     * Ray through a screen point, with [ndcX]/[ndcY] in normalised device
     * coordinates ([-1, 1], +y is screen-up) and [verticalFovDegrees] the
     * camera's vertical field of view. The ray is unprojected with a
     * pinhole model: direction = forward + right·(x·tan·aspect) +
     * up·(y·tan), normalised.
     */
    fun rayThrough(
        ndcX: Double,
        ndcY: Double,
        verticalFovDegrees: Double,
        aspect: Double,
    ): Ray {
        require(ndcX in -1.0..1.0) { "ndcX must be in [-1, 1], got $ndcX" }
        require(ndcY in -1.0..1.0) { "ndcY must be in [-1, 1], got $ndcY" }
        require(verticalFovDegrees in 1.0..179.0) {
            "vertical FOV must be in (0, 180), got $verticalFovDegrees"
        }
        require(aspect > 0.0 && aspect.isFinite()) {
            "aspect must be finite and positive, got $aspect"
        }
        val tanHalf = tan(Math.toRadians(verticalFovDegrees) / 2.0)
        val dir = forward +
            right * (ndcX * tanHalf * aspect) +
            up * (ndcY * tanHalf)
        return Ray(position, dir.normalized()!!)
    }
}
