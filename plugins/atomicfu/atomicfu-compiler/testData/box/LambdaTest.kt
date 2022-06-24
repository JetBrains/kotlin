import kotlinx.atomicfu.*
import kotlin.test.*

class LambdaTest {
    val a = atomic(0)
    val rs = atomic<String>("bbbb")

    private inline fun inlineLambda(
        arg: Int,
        crossinline block: (Int) -> Unit
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
}

fun box(): String {
    val testClass = LambdaTest()
    testClass.loopInLambda1(34)
    assertEquals(34, testClass.a.value)
    testClass.loopInLambda1(77)
    assertEquals(77, testClass.a.value)
    return "OK"
}
