// KIND: STANDALONE
// MODULE: Smokes(deps)
// FILE: smoke0.kt
fun fooByte(): Byte {
    return -1
}

fun fooShort(): Short {
    return -1
}

fun fooInt(): Int {
    return -1
}

fun fooLong(): Long {
    return -1
}

fun fooUByte(): UByte {
    return 0u
}

fun fooUShort(): UShort {
    return 0u
}

fun fooUInt(): UInt {
    return 0u
}

fun fooULong(): ULong {
    return 0u
}

// FILE: smokes.kt
package org.kotlin

fun plus(a: Int, b: Int, c: Int) =
    a + b + c

fun logicalOr(a: Boolean, b: Boolean) = a || b

fun xor(a: Boolean, b: Boolean) = a xor b

fun plus(a: UByte, b: UByte) = a + b
fun minus(a: UByte, b: UByte) = a - b

fun plus(a: UShort, b: UShort) = a + b
fun minus(a: UShort, b: UShort) = a - b

fun plus(a: UInt, b: UInt) = a + b
fun minus(a: UInt, b: UInt) = a - b

fun plus(a: ULong, b: ULong) = a + b
fun minus(a: ULong, b: ULong) = a - b

// FILE: dependency_usage.kt
import dependency.one.*

fun dependency_usage() = dep_fun()

// MODULE: deps(deps_2)
// FILE: deps_file.kt
package dependency.one

import dependency.two.*

fun dep_fun() = dep_fun_2()

// MODULE: deps_2(deps_3)
// FILE: deps_file_2.kt
package dependency.two
import dependency.three.*

fun dep_fun_2() = dep_fun_3()

// MODULE: deps_3
// FILE: deps_file_3.kt
package dependency.three

fun dep_fun_3() = 5
