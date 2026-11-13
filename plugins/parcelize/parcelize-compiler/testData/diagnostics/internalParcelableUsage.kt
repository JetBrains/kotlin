// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB

// FILE: generated.kt
package test

import android.os.Parcelable as AndroidParcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class User(val id: Int)

@Parcelize
class Other(val name: String)

val asAndroidParcelable: AndroidParcelable = User(1)

val mixed = listOf(User(1), Other(""))

fun either(flag: Boolean) = if (flag) User(1) else Other("")

fun useEither() = either(true).toString()

// FILE: direct.kt
package test.direct

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.internal.Parcelable

@Parcelize
class Explicit(val id: Int) : <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>

fun parameter(p: <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>) {}

val classLiteral = <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>::class

// FILE: suppressed.kt
@file:Suppress("INTERNAL_PARCELABLE_USAGE")

package test.suppressed

import kotlinx.parcelize.internal.Parcelable

fun parameter(p: Parcelable) {}
