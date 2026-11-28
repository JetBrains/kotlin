// @JvmName only changes the JVM name; the Kotlin signatures still clash.

// MODULE: main
// ALLOW_KOTLIN_PACKAGE
// FILE: kotlin/jvm/JvmName.kt
package kotlin.jvm

public annotation class JvmName(val name: String)

// FILE: sample/JvmNameClash.kt
package sample

import kotlin.jvm.JvmName

@JvmName("gFirst")
fun g(x: String) {}

@JvmName("gSecond")
fun g(x: String) {}
