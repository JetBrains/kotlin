// https://issuetracker.google.com/576960273
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

package test

import android.os.Parcelable

data class Accepted(val x: String)
data class PrivateField(private val x: String)
@ConsistentCopyVisibility
data class PrivateConstructor private constructor(val x: String)
data class UnsupportedField(val x: Any)
data class Generic<T>(val x: T)
data class GenericOut<out T>(val x: T)
data class GenericBounded<T : Parcelable>(val x: T)
data class BadButSerializable(val x: Any) : java.io.Serializable
data class Nested(val accepted: Accepted, val unsupported: UnsupportedField)

// MODULE: main(lib)
// FILE: main.kt

@file:OptIn(kotlinx.parcelize.Experimental::class)

package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class C(
    val disabled: <!PARCELABLE_TYPE_NOT_SUPPORTED!>Accepted<!>,
    val a: @DataClass Accepted,
    val b: @DataClass PrivateField,
    val c: <!PARCELABLE_TYPE_NOT_SUPPORTED!>@DataClass PrivateConstructor<!>,
    val d: @DataClass UnsupportedField,
    val e: @DataClass Generic<String>,
    val f: @DataClass GenericOut<String>,
    val g: @DataClass Generic<Any>,
    val h: @DataClass GenericBounded<*>,
    val i: @DataClass BadButSerializable,
    val j: @DataClass Nested,
) : Parcelable
