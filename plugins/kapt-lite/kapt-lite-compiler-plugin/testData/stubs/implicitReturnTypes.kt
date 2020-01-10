// WITH_RUNTIME

// FILE: lib/Prop.java
package lib;

public abstract class Prop<T> {
    public abstract int get(T key);
    public abstract void set(T key, int value);
}

// FILE: test.kt
package test

import lib.Prop

class Cl(var name: String)

val testObj = object : Prop<Cl>() {
    override fun get(key: Cl) = key.name.length
    override fun set(key: Cl, value: Int) {
        key.name = " ".repeat(value)
    }
}

private val testArray = arrayOf(object : Prop<Cl>() {
    override fun get(key: Cl) = key.name.length
    override fun set(key: Cl, value: Int) {
        key.name = " ".repeat(value)
    }
})

private val testList = listOf(object : Prop<Cl>() {
    override fun get(key: Cl) = key.name.length
    override fun set(key: Cl, value: Int) {
        key.name = " ".repeat(value)
    }
})

private fun test1() = (0..10).map { n ->
    object {
        override fun hashCode() = n
    }
}

private fun test2() = (0..10).map { n ->
    object : Runnable {
        override fun run() {}
    }
}

abstract class Foo

private fun test3() = (0..10).map { n ->
    object : Foo() {}
}

private fun test4() = (0..10).map { n ->
    object : Foo(), Runnable {
        override fun run() {}
    }
}

open class Bar {
    private val notReally = object : Runnable {
        override fun run() {
            throw UnsupportedOperationException()
        }
    }

    private fun a() = object : Runnable {
        override fun run() {}
    }

    private fun b() = object : java.io.Serializable, Runnable {
        override fun run() {}
    }

    private fun c() = object : Bar(), Runnable {
        override fun run() {}
    }

    private fun d() = listOf(object : Runnable {
        override fun run() {}
    })

    private fun e() = arrayOf(object : Runnable {
        override fun run() {}
    })
}

fun e1(a: Array<CharSequence>) {}
fun e2(a: Array<in CharSequence>) {}
fun e3(a: Array<out CharSequence>) {}
fun e3(a: Array<*>) {}

class Test<T : CharSequence, N : Number> {
    private val x = object : MyCallback {
        override fun fire(a: Int, b: String) {}
    }
}

interface MyCallback {
    fun fire(a: Int, b: String)
}