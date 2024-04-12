package test

import kotlin.experimental.*
import kotlin.reflect.*
import kotlin.time.*
import kotlin.native.OsFamily

class Foo

val boolean: Boolean
    get() = true

enum class E {
    A;
}

@ExperimentalNativeApi
fun main() {
    // Just some call
    val a = setOf(1, 2, 3)

    // Inline function
    require(boolean) { "Some message" }

    // common reflect
    val c: KClass<*> = Foo::class

    // Inline function + @JvmInline
    val d: Duration = measureTime { }

    // Comparable + enums - tricky "builtins"
    val e: Comparable<*> = E.A;

    // Reference to native stdlib
    val f = OsFamily.MACOSX
}
