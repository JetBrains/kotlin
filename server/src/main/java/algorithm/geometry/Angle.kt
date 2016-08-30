package algorithm.geometry

data class Angle(private val degrees: Int) {

    constructor(radians: Double) : this((180.0 * radians / Math.PI).toInt())

    fun rads(): Double {
        return Math.PI * degrees / 180.0
    }

    fun degs(): Int {
        return degrees
    }

    operator fun minus(other: Angle): Angle {
        return Angle(degrees - other.degrees)
    }

    operator fun unaryMinus(): Angle {
        return Angle(-degrees)
    }

    operator fun plus(other: Angle): Angle {
        return Angle(degrees + other.degrees)
    }
}