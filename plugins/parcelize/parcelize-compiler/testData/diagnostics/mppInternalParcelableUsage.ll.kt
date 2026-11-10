// LL_FIR_DIVERGENCE
// LL FIR analyzes m1-common against the expect Parcelable interface without android.os.Parcelable members, so Plain does not get ABSTRACT_MEMBER_NOT_IMPLEMENTED.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB

// MODULE: m1-common
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

// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt
package test
