@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("OPT_IN_USAGE_ERROR")

import kotlinx.cinterop.*
import kotlin.test.*
import kotlin.native.internal.*

import org.jetbrains.skiko.skia.native.*
import platform.posix.printf

fun main() {
    val a = Data()
    a.setData(17)
    val b = Data(19)
    val c = Data(a)
    val d = Data(a, b)
    val e = Data(200).foo(a, b)!!

    val a1 = a.checkData(17) != 0
    val b1 = b.checkData(119) != 0
    val c1 = c.checkData(217) != 0
    val d1 = d.checkData(436) != 0
    val e1 = e.checkData(536) != 0

    // Use printf instead of println to avoid messages
    // appearing out of order with the native code.
    // The native code uses printf.
    printf("$a1 $b1 $c1 $d1 $e1\n")
}
