// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82637
// TARGET_BACKEND: NATIVE
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: foo.kt
class Foo {
    internal val a: kotlinx.atomicfu.AtomicInt = kotlinx.atomicfu.atomic(0)

    inline internal fun bar() {
        a.compareAndSet(0, 1)
    }

    inline internal fun baz() {
        a.casPlus1()
    }
}

internal inline fun kotlinx.atomicfu.AtomicInt.casPlus1() { while (!compareAndSet(value, value + 1)) {} }

// FILE: bar.kt
fun box(): String {
    val foo = Foo()

    foo.<!LEAKED_VOLATILE_FIELD!>bar()<!>
    val resultAfterBar = foo.<!LEAKED_VOLATILE_FIELD!>a<!>.value

    foo.<!LEAKED_VOLATILE_FIELD!>baz()<!>
    val resultAfterBaz = foo.<!LEAKED_VOLATILE_FIELD!>a<!>.value

    return if (resultAfterBar != 1 || resultAfterBaz != 2) "FAIL: $resultAfterBar, $resultAfterBaz" else "OK"
}
