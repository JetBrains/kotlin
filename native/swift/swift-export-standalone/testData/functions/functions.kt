// KIND: STANDALONE
// MODULE: main
// FILE: main.kt
package namespace1.main

import namespace1.*
import namespace2.*

fun foobar(param: Int): Int = foo(15) + bar() + param

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

    arg12: Char,
): Unit = Unit

// FILE: bar.kt
package namespace1

fun bar(): Int = 123

// FILE: any.kt

fun return_any_should_append_runtime_import(): Any = TODO()

// FILE: foo.kt
package namespace2

fun foo(arg1: Int) = 123

// FILE: internal.kt
internal fun foo_internal() = 123

// FILE: local_functions.kt
package namespace1.local_functions

fun foo() {
    fun bar() {
        val baz = 0
    }
}

// FILE: no_package.kt
fun foo(): Int = 123

// FILE: overload.kt
package overload

fun foo(arg1: Int) = 123

fun foo(arg1: Double) = 321

// FILE: extension_fun.kt
// we do not support extention fun. This should not be exported
fun Int.foo(): Unit = TODO()

// FILE: suspend_function.kt
// we do not support sus fun. This should not be exported
suspend fun suspending_fun(): Int = TODO()
