import objc_misc.*
import kotlin.native.Platform

val a = B.giveC()!! as C

fun main() {
  Platform.isMemoryLeakCheckerActive = true
  println("OK")
}
