import kotlinx.atomicfu.*
import kotlin.test.*

class LambdaTest {
    private val a = atomic(0)
    private val rs = atomic<String>("bbbb")

    private inline fun <T> inlineLambda(
        arg: T,
        crossinline block: (T) -> Unit
    ) = block(arg)

    fun loopInLambda1(to: Int) = inlineLambda(to) sc@ { arg ->
        a.loop { value ->
            a.compareAndSet(value, arg)
            return@sc
        }
    }

    fun loopInLambda2(to: Int) = inlineLambda(to) { arg1 ->
        inlineLambda(arg1) sc@ { arg2 ->
            a.loop { value ->
                a.compareAndSet(value, arg2)
                return@sc
            }
        }
    }

    fun loopInLambda2Ref(to: String) = inlineLambda(to) { arg1 ->
        inlineLambda(arg1) sc@ { arg2 ->
            rs.loop { value ->
                rs.compareAndSet(value, arg2)
                return@sc
            }
        }
    }

    fun test() {
        loopInLambda1(34)
        assertEquals(34, a.value)
        loopInLambda1(77)
        assertEquals(77, a.value)
        loopInLambda2Ref("bbb")
        assertEquals("bbb", rs.value)
    }
}

@Test
fun box() {
    val testClass = LambdaTest()
    testClass.test()
}
