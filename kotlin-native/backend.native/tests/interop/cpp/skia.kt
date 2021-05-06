@file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")
import kotlinx.cinterop.*
import kotlin.test.*
import kotlin.native.internal.*

import org.jetbrains.skiko.skia.native.*

fun main() {
    kotlin.native.internal.Debugging.forceCheckedShutdown = true

    var f: Foo? = Foo()
    var a: Value? = Value()
    a!!.cpp.data = 17
    // TODO: update refcount calculations when ref/unref are fixed
    a!!.cpp.refcount = 100;

    var b: Value? = f!!.qux();

    println("a: ${a?.managed}, f: ${f?.managed}, b: ${b?.managed}")
    println("a: ${a?.cleaner != null}, f: ${f?.cleaner != null}, b: ${b?.cleaner != null}")

    var x: Value? = f!!.foo(a)
    println("refcount = ${a?.cpp?.refcount} ${x?.cpp?.refcount}")
    var z: Value? = f!!.bar(x!!)
    println("refcount = ${a?.cpp?.refcount} ${x?.cpp?.refcount} ${z?.cpp?.refcount}")
    println("data = ${a?.cpp?.data} ${x?.cpp?.data} ${z?.cpp?.data}")
    f = null
    a = null
    x = null
    z = null
    b = null
}
