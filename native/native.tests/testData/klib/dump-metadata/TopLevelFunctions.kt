// FIR_IDENTICAL
@file:Suppress("UNUSED_PARAMETER")

annotation class A
annotation class B

class Foo

fun f1(x: Foo): Unit {}
fun f2(x: Foo, y: Foo) = 0

// inline
inline fun i1(block: () -> Foo) {}
inline fun i2(noinline block: () -> Foo) {}
inline fun i3(crossinline block: () -> Foo) {}

// callable args
fun i4(block: (Foo) -> Int) {}
fun i5(block: (Foo, Foo) -> Int) {}
fun i6(block: Foo.() -> Int) {}
fun i7(block: Foo.(Foo) -> Int) {}

// type parameters
fun <T> t1(x: Foo) {}
fun <T> t2(x: T) {}
fun <T, F> t3(x: T, y: F) {}
inline fun <reified T> t4(x: T) {}
fun <T: Number> t5(x: T) {}

// extension
fun Foo.e() {}

// annotations
@A @B fun a() {}