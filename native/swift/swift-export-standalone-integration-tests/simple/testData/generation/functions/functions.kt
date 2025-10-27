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

// FILE: void.kt

package namespace3

// KT-79227 Swift Export: Fix First Release Issues
// Void function parameters aren't passed down correctly
fun foo(faux: Unit): Unit = Unit

// KT-79227 Swift Export: Fix First Release Issues
// Void has to be the first and the only argument
fun foo(arg1: Int, faux: Unit): Unit = Unit

val bar: Unit get() = Unit

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

class Foo
fun foo(arg1: Foo): Unit = TODO()
fun foo(arg1: Foo?): Unit = TODO()

// FILE: extension_fun.kt
class Foo {
    fun String.ext(): Unit = TODO()
    val String.extVal: String
        get() = TODO()
    var String.extVar: String
        get() = TODO()
        set(v) {}
}
fun Int.foo(): Unit = TODO()
fun Int?.foo(): Unit = TODO()
fun Foo.foo(): Unit = TODO()
fun Foo?.foo(): Unit = TODO()

var Int.foo: String
    get() = TODO()
    set(v) { }

var Int?.foo: String
    get() = TODO()
    set(v) = TODO()

var Foo.foo: String
    get() = TODO()
    set(v) = TODO()

var Foo?.foo: String
    get() = TODO()
    set(v) = TODO()

val Int.bar: String get() = TODO()
val Int?.bar: String get() = TODO()
val Foo.bar: String get() = TODO()
val Foo?.bar: String get() = TODO()

// FILE: operator_fun.kt
package operators

import kotlin.reflect.KProperty

data class Foo(var value: Int = 0) {
    // Iterators
    object EmptyIterator: Iterator<Int> {
        override operator fun next(): Int = 0
        override operator fun hasNext(): Boolean = false
    }

    operator fun iterator(): Iterator<Int> = EmptyIterator

    // Subscripting

    operator fun get(index: Int): Int = index
    operator fun set(index: Int, value: Int) {}

    // Unary
    operator fun not(): Foo = this
    operator fun unaryPlus(): Foo = this
    operator fun unaryMinus(): Foo = this

    // Unary
    operator fun inc(): Foo = this
    operator fun dec(): Foo = this

    // Binary
    operator fun plus(other: Foo): Foo = this // +
    operator fun minus(other: Foo): Foo = this // -
    operator fun times(other: Foo): Foo = this // *
    operator fun div(other: Foo): Foo = this // /
    operator fun rem(other: Foo): Foo = this // %

    operator fun rangeTo(other: Foo): Foo = this // ..
    operator fun rangeUntil(other: Foo): Foo = this // ..<

    // Binary-words
    operator fun contains(other: Foo): Boolean = false

    // Compount-assignment

    operator fun plusAssign(other: Foo): Unit {} // +=
    operator fun minusAssign(other: Foo): Unit {} // -=
    operator fun timesAssign(other: Foo): Unit {} // *=
    operator fun divAssign(other: Foo): Unit {} // /=
    operator fun remAssign(other: Foo): Unit {} // %=

    // Equality & Comparsion

    override operator fun equals(other: Any?): Boolean = this === other
    operator fun compareTo(other: Foo): Int = 0

    // Delegation

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): Foo = this

    // function call

    operator fun invoke(): Foo = this
}

operator fun Foo.invoke(other: Foo): Foo = other
