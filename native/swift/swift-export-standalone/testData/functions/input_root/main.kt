package namespace1.main

import namespace1.*
import namespace2.*

fun foobar(param: Int): Int = foo() + bar() + param

fun all_args(
    arg1: Boolean,

    arg2: Byte,
    arg3: Short,
    arg4: Int,
    arg5: Long,

    arg6: UByte,
    arg7: UShort,
    arg8: UInt,
    arg9: ULong,

    arg10: Float,
    arg11: Double,
): Unit = Unit
