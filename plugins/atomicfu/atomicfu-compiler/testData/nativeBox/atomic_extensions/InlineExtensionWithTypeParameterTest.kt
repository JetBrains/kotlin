// TODO(KT-65977): reenable these tests with caches
//IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
//IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
import kotlinx.atomicfu.*
import kotlin.test.*

class InlineExtensionWithTypeParameterTest {
    abstract class Segment<S : Segment<S>>(val id: Int)
    class SemaphoreSegment(id: Int) : Segment<SemaphoreSegment>(id)

    private inline fun <S : Segment<S>> AtomicRef<S>.foo(
        id: Int,
        startFrom: S
    ): Int {
        lazySet(startFrom)
        return value.getSegmentId()
    }

    private inline fun <S : Segment<S>> S.getSegmentId(): Int {
        var cur: S = this
        return cur.id
    }

    private val sref = atomic(SemaphoreSegment(0))

    fun testInlineExtensionWithTypeParameter() {
        val s = SemaphoreSegment(77)
        assertEquals(77, sref.foo(0, s))
    }
}

@Test
fun box() {
    val testClass = InlineExtensionWithTypeParameterTest()
    testClass.testInlineExtensionWithTypeParameter()
}
