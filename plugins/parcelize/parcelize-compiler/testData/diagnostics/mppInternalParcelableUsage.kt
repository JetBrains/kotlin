// LANGUAGE: +MultiPlatformProjects
// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// METADATA_ONLY_COMPILATION
// METADATA_TARGET_PLATFORMS: JVM, JS
// WITH_STDLIB

// FILE: generated.kt
package test

import kotlinx.parcelize.Parcelize

@Parcelize
class User(val id: Int)

@Parcelize
sealed interface Shape

@Parcelize
class Circle(val radius: Double) : Shape

val mixed = listOf(User(1), Circle(1.0))

fun either(flag: Boolean) = if (flag) User(1) else Circle(1.0)

fun useEither() = either(true).toString()

// FILE: direct.kt
package test.direct

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.internal.Parcelable

class Plain : <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>

@Parcelize
class Explicit(val id: Int) : <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>

fun parameter(p: <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>) {}

val list: List<<!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>> = emptyList()

fun isCheck(x: Any): Boolean = x is <!INTERNAL_PARCELABLE_USAGE!>kotlinx.parcelize.internal.Parcelable<!>

typealias Alias = <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>

val classLiteral = <!INTERNAL_PARCELABLE_USAGE!>Parcelable<!>::class
