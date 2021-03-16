import kotlinx.atomicfu.*
import kotlin.test.*

class LoopTest {
    private val a = atomic(0)
    private val r = atomic<A>(A("aaaa"))
    private val rs = atomic<String>("bbbb")

    private class A(val s: String)

    private inline fun casLoop(to: Int): Int {
        a.loop { cur ->
            if (a.compareAndSet(cur, to)) return a.value
            return 777
        }
    }

    private inline fun AtomicInt.extensionLoop(to: Int): Int {
        loop { cur ->
            if (compareAndSet(cur, to)) return value
            return 777
        }
    }

    private inline fun AtomicInt.extensionLoopMixedReceivers(to: Int): Int {
        loop { cur ->
            compareAndSet(cur, to)
            a.compareAndSet(cur, to)
            return value
        }
    }

    private inline fun AtomicInt.extensionLoopRecursive(to: Int): Int {
        loop { cur ->
            compareAndSet(cur, to)
            a.extensionLoop(5)
            return value
        }
    }

    private inline fun AtomicInt.returnExtensionLoop(to: Int): Int =
        loop { cur ->
            lazySet(cur + 10)
            return if (compareAndSet(cur, to)) value else incrementAndGet()
        }

    private inline fun AtomicRef<A>.casLoop(to: String): String = loop { cur ->
        if (compareAndSet(cur, A(to))) {
            val res = value.s
            return "${res}_AtomicRef<A>"
        }
    }

    private inline fun AtomicRef<String>.casLoop(to: String): String = loop { cur ->
        if (compareAndSet(cur, to)) return "${value}_AtomicRef<String>"
    }

        fun testDeclarationWithEqualNames() {
        check(r.casLoop("kk") == "kk_AtomicRef<A>")
        check(rs.casLoop("pp") == "pp_AtomicRef<String>")
    }

    fun testIntExtensionLoops() {
        check(casLoop(5) == 5)
        assertEquals(a.extensionLoop(66), 66)
        check(a.returnExtensionLoop(777) == 77)
    }

    abstract class Segment<S : Segment<S>>(val id: Int)
    class SemaphoreSegment(id: Int) : Segment<SemaphoreSegment>(id)

    private inline fun <S : Segment<S>> AtomicRef<S>.foo(
        id: Int,
        startFrom: S
    ) {
        startFrom.getSegmentId()
    }

    private inline fun <S : Segment<S>> S.getSegmentId(): Int {
        var cur: S = this
        return cur.id
    }

    fun testInlineFunWithTypeParameter() {
        val s = SemaphoreSegment(0)
        val sref = atomic(s)
        sref.foo(0, s)
    }
}

fun box(): String {
    val testClass = LoopTest()
    testClass.testIntExtensionLoops()
    testClass.testDeclarationWithEqualNames()
    testClass.testInlineFunWithTypeParameter()
    return "OK"
}