// ISSUE: KT-82637
// TARGET_BACKEND: NATIVE

// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE&&mode=TWO_STAGE_MULTI_MODULE
// ^^^ Unmute it when KT-82637 is fixed.
// The test works in the one-stage mode though, because the per-file cache is not applied to the module there: KT-77365.

// DISABLE_IR_VISIBILITY_CHECKS: NATIVE
// ^^^ Because AtomicFU plugin generates an IR property reference node that refers to a private property, KT-85180.

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

    foo.bar()
    val resultAfterBar = foo.a.value

    foo.baz()
    val resultAfterBaz = foo.a.value

    return if (resultAfterBar != 1 || resultAfterBaz != 2) "FAIL: $resultAfterBar, $resultAfterBaz" else "OK"
}
