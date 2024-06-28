// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// ISSUE: KT-67523

// MODULE: lib
// FILE: Screen.kt
package test

import android.os.Parcelable

interface Screen : Parcelable
interface AndroidScreen : Screen

// MODULE: m1-common(lib)
// FILE: common.kt
package test

expect <!ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class OpenUrlScreen<!>(url: String) : Screen {
    val url: String
}

// MODULE: m1-jvm(lib)()(m1-common)
@file:JvmName("TestKt")
package test

import android.os.Parcelable
import kotlinx.parcelize.*

@Parcelize
actual data class OpenUrlScreen actual constructor(actual val url: String) : Screen

fun box() = "OK"
