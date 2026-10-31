package measure

/**
 * Display units. The core always measures in metres (SI); conversion
 * happens only at the presentation edge, so no conversion factor can ever
 * leak into a stored measurement.
 */
enum class LengthUnit(val symbol: String, val metresPerUnit: Double) {
    METRES("m", 1.0),
    CENTIMETRES("cm", 0.01),
    INCHES("in", 0.0254),
    FEET("ft", 0.3048);

    /** Convert a value in metres to this unit. */
    fun fromMetres(metres: Double): Double {
        require(metres.isFinite()) { "metres must be finite" }
        return metres / metresPerUnit
    }
}

/** Format a length in metres for display, e.g. "4.20 m" or "13.78 ft". */
fun formatLength(metres: Double, unit: LengthUnit = LengthUnit.METRES): String {
    val value = unit.fromMetres(metres)
    return "%.2f %s".format(value, unit.symbol)
}

/** Format an area in square metres, e.g. "13.02 m²" or "140.15 ft²". */
fun formatArea(squareMetres: Double, unit: LengthUnit = LengthUnit.METRES): String {
    require(squareMetres.isFinite()) { "area must be finite" }
    val value = squareMetres / (unit.metresPerUnit * unit.metresPerUnit)
    return "%.2f %s²".format(value, unit.symbol)
}
