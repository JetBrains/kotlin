// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// FILE: TEST_FILE1.kt
// CHECK-NOT: @CallInitGlobalPossiblyLock({{.*}}TEST_FILE1

import kotlin.reflect.*

operator fun String.getValue(thisRef: Any?, prop: KProperty<*>) = this
operator fun String.setValue(thisRef: Any?, prop: KProperty<*>, value: String) {}

class A {
    val a by "A"
    var b by "B"
}

val c by "C"
val d by "D"

fun box(): String {
    val e by "E"
    var f by "F"
    val res = A().a + A().b + c + d + e + f + g
    if (res != "ABCDEFG") return "FAIL"
    return "OK"
}

// FILE: TEST_FILE2.kt

// to check code-generation of static scope initialization didn't change
// CHECK: @CallInitGlobalPossiblyLock({{.*}}TEST_FILE2

fun foo() = "G"
val g = foo()