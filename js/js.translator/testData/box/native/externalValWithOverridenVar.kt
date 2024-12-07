// KT-46643
// IGNORE_BACKEND: WASM
// WITH_STDLIB

import kotlin.reflect.KProperty

external interface IBase {
    val foo: String
}

external abstract class Base : IBase

open class A : Base() {
    override var foo: String = "Error: A setter was not called."
        set(k) { result = "O$k"}

    lateinit var result: String
}

open class B : Base() {
    override val foo: String = "OK"

    open val result: String get() = foo
}

class C : B() {
    override var foo: String = "Error: C setter was not called."
        set(k) { result = "O$k"}

    override lateinit var result: String
}

open class D : B() {
    override val foo: String = "OK"
}

open class E : D() {
    override var foo: String = "Error: E setter was not called."
        set(k) { result = "O$k"}

    override lateinit var result: String
}

open class F: B() {
    override var foo: String by CustomDelegator

    private object CustomDelegator {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return "Error: F setter was not called."
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            result = "O$value"
        }

        lateinit var result: String
    }

    override val result: String get() = CustomDelegator.result
}

class G(val b: B): IBase by b {
    val result: String get() = b.result
}

fun box(): String {
    val a = A()
    if (a.result != "OK") return a.foo

    val b = B()
    if (b.result != "OK") return b.foo

    val c = C()
    if (c.result != "OK") return c.foo

    val d = D()
    if (d.result != "OK") return d.foo

    val e = E()
    if (e.result != "OK") return e.foo

    try {
        val f = F()
        return "Failed: it should not work for now, because of delegating objects initialization order"
    } catch (e: Throwable) {}

    val g = G(e)
    if (g.result != "OK") return g.foo

    return "OK"
}