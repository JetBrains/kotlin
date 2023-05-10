// API_VERSION: 2.0
// LANGUAGE_VERSION: 2.1
package test

import org.jetbrains.kotlin.fir.plugin.MyComposable

fun runComposable(block: @MyComposable () -> Unit) {}
