@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("a__TypesOfArguments__Int8_Int16_Int32_Int64__")
public fun a__TypesOfArguments__Int8_Int16_Int32_Int64__(p0: Byte, p1: Short, p2: Int, p3: Long): Int {
    val __p0 = p0
    val __p1 = p1
    val __p2 = p2
    val __p3 = p3
    val _result = pkg.a(__p0, __p1, __p2, __p3)
    return _result
}

@ExportedBridge("b__TypesOfArguments__UInt8_UInt16_UInt32_UInt64__")
public fun b__TypesOfArguments__UInt8_UInt16_UInt32_UInt64__(p0: UByte, p1: UShort, p2: UInt, p3: ULong): UInt {
    val __p0 = p0
    val __p1 = p1
    val __p2 = p2
    val __p3 = p3
    val _result = pkg.b(__p0, __p1, __p2, __p3)
    return _result
}

@ExportedBridge("c__TypesOfArguments__Bool__")
public fun c__TypesOfArguments__Bool__(p0: Boolean): Boolean {
    val __p0 = p0
    val _result = pkg.c(__p0)
    return _result
}
