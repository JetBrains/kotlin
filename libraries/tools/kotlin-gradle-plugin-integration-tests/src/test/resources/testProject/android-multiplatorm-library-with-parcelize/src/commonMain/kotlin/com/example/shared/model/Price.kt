package com.example.shared.model

import com.example.shared.Parcelable
import com.example.shared.Parcelize
import com.example.shared.IgnoredOnParcel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

@Parcelize
data class Price(
    /** 12.34 ¤ → 1234L */
    val cents: Long = 0L,
) : Parcelable {

    /** 12.34 ¤ → 12L */
    @IgnoredOnParcel
    val units: Long
        get() = cents / 100

    /** 12.34 ¤ → 12.34D */
    @IgnoredOnParcel
    val floating: Double
        get() = cents / 100.0

    operator fun plus(other: Price?): Price = copy(cents = cents + (other?.cents ?: 0))
    operator fun minus(other: Price?): Price = copy(cents = cents - (other?.cents ?: 0))
    operator fun times(n: Int): Price = copy(cents = cents * n)
    operator fun times(n: Long): Price = copy(cents = cents * n)
    operator fun times(n: Float): Price = copy(cents = (cents * n).roundToLong())
    operator fun times(n: Double): Price = copy(cents = (cents * n).roundToLong())
    operator fun div(n: Int): Price = copy(cents = cents / n)
    operator fun div(n: Float): Price = copy(cents = (cents / n).roundToLong())
    operator fun div(other: Price): Double = cents.toDouble() / other.cents.toDouble()

}
