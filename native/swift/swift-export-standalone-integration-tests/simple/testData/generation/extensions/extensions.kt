// KIND: STANDALONE
// MODULE: main
// SWIFT_EXPORT_CONFIG: packageRoot=foo
// FILE: foo.kt

package foo

import Bar

interface Foo {
    fun Bar.doubleReceiverExtFun(): String
    val Bar.doubleReceiverExtProp: Int
}

fun Foo.simpleExtFun(): String = TODO()

fun Foo.simpleExtFunWithArgs(arg1: Int, arg2: Bar): String = TODO()

context(bar: Bar)
fun Foo.contextExtFun(arg: Boolean): Int = TODO()

fun Foo.varargExtFun(vararg args: String): Int = TODO()

fun Foo?.nullableFun(): Boolean = TODO()

val Foo.simpleProp: String get() = TODO()

var Foo.simplePropVar: String
    get() = TODO()
    set(_) = TODO()

context(bar: Bar)
var Foo.contextProp: Int
    get() = TODO()
    set(_) = TODO()

var Foo?.nullableProp: Boolean
    get() = TODO()
    set(_) = TODO()

// FILE: bar.kt

@file:OptIn(ExperimentalApi::class)

import foo.Foo

@RequiresOptIn(message = "This is an experimental API", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
annotation class ExperimentalApi

@ExperimentalApi
class Bar {
    fun Foo.doubleReceiverExtFun(): String = TODO()
    val Foo.doubleReceiverExtProp: Int get() = TODO()
}

var Bar.deprecatedSetterProp: Boolean
    get() = TODO()
    @Deprecated("deprecated", level = DeprecationLevel.WARNING)
    set(value) = TODO()

var Bar.errorDeprecatedSetterProp: Boolean
    get() = TODO()
    @Deprecated("deprecated", level = DeprecationLevel.ERROR)
    set(value) = TODO()

var Bar.hiddenDeprecatedSetterProp: Boolean
    get() = TODO()
    @Deprecated("deprecated", level = DeprecationLevel.HIDDEN)
    set(value) = TODO()

@Deprecated("deprecated", level = DeprecationLevel.ERROR)
class DeprecatedBar

@Suppress("DEPRECATION_ERROR")
fun DeprecatedBar.deprecatedClassFun(): String = TODO()

// FILE: baz.kt

import foo.Foo

object Baz {
    fun Foo.doubleReceiverExtFun(): String = TODO()
    val Foo.doubleReceiverExtProp: Int get() = TODO()
}

@ExperimentalApi
fun Baz.optInExtFun(): Boolean = TODO()

@ExperimentalApi
var Baz.optInProp: Int
    get() = TODO()
    set(value) = TODO()

var Baz.optInSetterProp: String
    get() = TODO()
    @ExperimentalApi
    set(value) = TODO()

fun (() -> Unit).funExtFun(): Boolean = TODO()

val (() -> Unit).funExtProp: Int get() = TODO()

class GenericClass<T>

fun GenericClass<*>.genericExtFun(): String = TODO()

val GenericClass<*>.genericExtProp: Int get() = TODO()

fun GenericClass<Any?>.genericUpperBoundExtFun(): String = TODO()

val GenericClass<Any?>.genericUpperBoundExtProp: Int get() = TODO()

fun GenericClass<String>.genericConstrainedExtFun(): String = TODO()

val GenericClass<String>.genericConstrainedExtProp: Int get() = TODO()

// FILE: other.kt

package other

class Other

fun Other.otherExtFun(): String = TODO()

var Other.otherProp: Int
    get() = TODO()
    set(value) = TODO()
