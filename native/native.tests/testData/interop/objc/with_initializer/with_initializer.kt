@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import with_initializer.*
import kotlin.native.Platform

val a = B.giveC()!! as C

fun main() {
  Platform.isMemoryLeakCheckerActive = true
  println("OK")
}
