import kotlinx.atomicfu.*
import kotlin.test.*

class AA(val value: Int) {
    val b = B(value + 1)
    val c = C(D(E(value + 1)))

    fun updateToB(affected: Any): Boolean {
        (affected as AtomicState).state.compareAndSet(this, b)
        return (affected.state.value is B && (affected.state.value as B).value == value + 1)
    }

    fun manyProperties(affected: Any): Boolean {
        (affected as AtomicState).state.compareAndSet(this, c.d.e)
        return (affected.state.value is E && (affected.state.value as E).x == value + 1)
    }
}

class B (val value: Int)

class C (val d: D)
class D (val e: E)
class E (val x: Int)


private class AtomicState(value: Any) {
    val state = atomic<Any?>(value)
}

class ScopeTest {
    fun scopeTest() {
        val a = AA(0)
        val affected: Any = AtomicState(a)
        check(a.updateToB(affected))
        val a1 = AA(0)
        val affected1: Any = AtomicState(a1)
        check(a1.manyProperties(affected1))
    }
}

fun box(): String {
    val testClass = ScopeTest()
    testClass.scopeTest()
    return "OK"
}
