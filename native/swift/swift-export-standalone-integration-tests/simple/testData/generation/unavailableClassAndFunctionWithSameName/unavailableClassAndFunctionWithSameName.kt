// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
// https://youtrack.jetbrains.com/issue/KT-90846
package com.github.jetbrains.swiftexport.se

@Deprecated("Use BannerModel", level = DeprecationLevel.ERROR)
class Banner(val text: String)

@Deprecated("Use loadBannerModel", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
fun loadBanner(): Banner = Banner("b")

fun Banner(text: String, onClose: () -> Unit) {}

@Suppress("DEPRECATION_ERROR")
fun Banner(text: String, dummy: Int): Banner = TODO()
