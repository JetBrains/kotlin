import kotlinx.atomicfu.*
import kotlin.test.*

class InlineExtensionWithTypeParameterTest {
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

    fun testInlineExtensionWithTypeParameter() {
        val s = SemaphoreSegment(0)
        val sref = atomic(s)
        sref.foo(0, s)
    }
}

fun box(): String {
    val testClass = InlineExtensionWithTypeParameterTest()
    testClass.testInlineExtensionWithTypeParameter()
    return "OK"
}