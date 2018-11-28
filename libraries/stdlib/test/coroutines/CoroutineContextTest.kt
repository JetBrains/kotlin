/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.coroutines

import kotlin.test.*
import kotlin.coroutines.*

class CoroutineContextTest {
    data class CtxA(val i: Int) : AbstractCoroutineContextElement(CtxA) {
        companion object Key : CoroutineContext.Key<CtxA>
    }

    data class CtxB(val i: Int) : AbstractCoroutineContextElement(CtxB) {
        companion object Key : CoroutineContext.Key<CtxB>
    }

    data class CtxC(val i: Int) : AbstractCoroutineContextElement(CtxC) {
        companion object Key : CoroutineContext.Key<CtxC>
    }

    object Disp1 : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = continuation
        override fun toString(): String = "Disp1"
    }

    object Disp2 : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = continuation
        override fun toString(): String = "Disp2"
    }

    @Test
    fun testGetPlusFold() {
        var ctx: CoroutineContext = EmptyCoroutineContext
        assertContents(ctx)
        assertEquals("EmptyCoroutineContext", ctx.toString())

        ctx += CtxA(1)
        assertContents(ctx, CtxA(1))
        assertEquals("CtxA(i=1)", ctx.toString())
        assertEquals(CtxA(1), ctx[CtxA])
        assertEquals(null, ctx[CtxB])
        assertEquals(null, ctx[CtxC])

        ctx += CtxB(2)
        assertContents(ctx, CtxA(1), CtxB(2))
        assertEquals("[CtxA(i=1), CtxB(i=2)]", ctx.toString())
        assertEquals(CtxA(1), ctx[CtxA])
        assertEquals(CtxB(2), ctx[CtxB])
        assertEquals(null, ctx[CtxC])

        ctx += CtxC(3)
        assertContents(ctx, CtxA(1), CtxB(2), CtxC(3))
        assertEquals("[CtxA(i=1), CtxB(i=2), CtxC(i=3)]", ctx.toString())
        assertEquals(CtxA(1), ctx[CtxA])
        assertEquals(CtxB(2), ctx[CtxB])
        assertEquals(CtxC(3), ctx[CtxC])

        ctx += CtxB(4)
        assertContents(ctx, CtxA(1), CtxC(3), CtxB(4))
        assertEquals("[CtxA(i=1), CtxC(i=3), CtxB(i=4)]", ctx.toString())
        assertEquals(CtxA(1), ctx[CtxA])
        assertEquals(CtxB(4), ctx[CtxB])
        assertEquals(CtxC(3), ctx[CtxC])

        ctx += CtxA(5)
        assertContents(ctx, CtxC(3), CtxB(4), CtxA(5))
        assertEquals("[CtxC(i=3), CtxB(i=4), CtxA(i=5)]", ctx.toString())
        assertEquals(CtxA(5), ctx[CtxA])
        assertEquals(CtxB(4), ctx[CtxB])
        assertEquals(CtxC(3), ctx[CtxC])
    }

    @Test
    fun testMinusKey() {
        var ctx: CoroutineContext = CtxA(1) + CtxB(2) + CtxC(3)
        assertContents(ctx, CtxA(1), CtxB(2), CtxC(3))
        assertEquals("[CtxA(i=1), CtxB(i=2), CtxC(i=3)]", ctx.toString())

        ctx = ctx.minusKey(CtxA)
        assertContents(ctx, CtxB(2), CtxC(3))
        assertEquals("[CtxB(i=2), CtxC(i=3)]", ctx.toString())
        assertEquals(null, ctx[CtxA])
        assertEquals(CtxB(2), ctx[CtxB])
        assertEquals(CtxC(3), ctx[CtxC])

        ctx = ctx.minusKey(CtxC)
        assertContents(ctx, CtxB(2))
        assertEquals("CtxB(i=2)", ctx.toString())
        assertEquals(null, ctx[CtxA])
        assertEquals(CtxB(2), ctx[CtxB])
        assertEquals(null, ctx[CtxC])

        ctx = ctx.minusKey(CtxC)
        assertContents(ctx, CtxB(2))
        assertEquals("CtxB(i=2)", ctx.toString())
        assertEquals(null, ctx[CtxA])
        assertEquals(CtxB(2), ctx[CtxB])
        assertEquals(null, ctx[CtxC])

        ctx = ctx.minusKey(CtxB)
        assertContents(ctx)
        assertEquals("EmptyCoroutineContext", ctx.toString())
        assertEquals(null, ctx[CtxA])
        assertEquals(null, ctx[CtxB])
        assertEquals(null, ctx[CtxC])

        assertEquals(EmptyCoroutineContext, ctx)
    }

    @Test
    fun testPlusCombined() {
        val ctx1 = CtxA(1) + CtxB(2)
        val ctx2 = CtxB(3) + CtxC(4)
        val ctx = ctx1 + ctx2
        assertContents(ctx, CtxA(1), CtxB(3), CtxC(4))
        assertEquals("[CtxA(i=1), CtxB(i=3), CtxC(i=4)]", ctx.toString())
        assertEquals(CtxA(1), ctx[CtxA])
        assertEquals(CtxB(3), ctx[CtxB])
        assertEquals(CtxC(4), ctx[CtxC])
    }

    @Test
    fun testLastDispatcher() {
        var ctx: CoroutineContext = EmptyCoroutineContext
        assertContents(ctx)
        ctx += CtxA(1)
        assertContents(ctx, CtxA(1))
        ctx += Disp1
        assertContents(ctx, CtxA(1), Disp1)
        ctx += CtxA(2)
        assertContents(ctx, CtxA(2), Disp1)
        ctx += CtxB(3)
        assertContents(ctx, CtxA(2), CtxB(3), Disp1)
        ctx += Disp2
        assertContents(ctx, CtxA(2), CtxB(3), Disp2)
        ctx += (CtxB(4) + CtxC(5))
        assertContents(ctx, CtxA(2), CtxB(4), CtxC(5), Disp2)
    }

    @Test
    fun testEquals() {
        val ctx1 = CtxA(1) + CtxB(2) + CtxC(3)
        val ctx2 = CtxB(2) + CtxC(3) + CtxA(1) // same
        val ctx3 = CtxC(3) + CtxA(1) + CtxB(2) // same
        val ctx4 = CtxA(1) + CtxB(2) + CtxC(4) // different
        assertEquals(ctx1, ctx2)
        assertEquals(ctx1, ctx3)
        assertEquals(ctx2, ctx3)
        assertNotEquals(ctx1, ctx4)
        assertNotEquals(ctx2, ctx4)
        assertNotEquals(ctx3, ctx4)
    }

    private fun assertContents(ctx: CoroutineContext, vararg elements: CoroutineContext.Element) {
        val set = ctx.fold(setOf<CoroutineContext>()) { a, b -> a + b }
        assertEquals(listOf(*elements), set.toList())
        for (elem in elements)
            assertTrue(ctx[elem.key] == elem)
    }


    @Test
    fun testBasicOperations() {
        checkContents(EmptyCoroutineContext)
        val de1 = DataElement(1)
        val de2 = DataElement(2)
        checkContents(de1, de1)
        checkContents(de2, de2)
        checkContents(de1 + de2, de2)
        checkContents(de2 + de1, de1)
        val oe3 = OtherElement(3)
        val oe4 = OtherElement(4)
        val de1oe3 = de1 + oe3
        checkContents(de1oe3, de1, oe3)
        checkContents(de1oe3 + de2, de2, oe3)
        checkContents(de1oe3 + oe4, de1, oe4)
        checkContents(de1oe3.minusKey(DataElement), oe3)
        checkContents(de1oe3.minusKey(OtherElement), de1)
    }

    @Test
    fun testWrapperEquality() {
        val we1 = WrapperElement("1")
        val we2 = WrapperElement("2")
        checkContents(we1, we1)
        checkContents(we2, we2)
        val we1we2 = we1 + we2
        checkContents(we1we2, we1, we2)
        checkContents(we1we2.minusKey(WrapperKey("1")), we2)
        checkContents(we1we2.minusKey(WrapperKey("2")), we1)
    }

    @Test
    fun testInterceptor() {
        val de1 = DataElement(1)
        val oe2 = OtherElement(2)
        val we3 = WrapperElement("3")
        val cci = CustomContinuationInterceptor()
        // Make sure it works properly with any position of ContinuationInterceptor in the context
        checkContentsAndRemoves(de1 + oe2 + we3 + cci, de1, oe2, we3, cci)
        checkContentsAndRemoves(de1 + oe2 + cci + we3, de1, oe2, we3, cci)
        checkContentsAndRemoves(de1 + cci + oe2 + we3, de1, oe2, we3, cci)
        checkContentsAndRemoves(cci + de1 + oe2 + we3, de1, oe2, we3, cci)
    }

    private fun checkContentsAndRemoves(context: CoroutineContext, vararg es: CoroutineContext.Element) {
        checkContents(context, *es)
        for (e in es) {
            checkContents(context.minusKey(e.key), *(es.toSet() - e).toTypedArray())
        }
    }

    private fun checkContents(context: CoroutineContext, vararg es: CoroutineContext.Element) {
        val size = context.fold(0) { c, _ -> c + 1 }
        assertEquals(es.size, size)
        for (e in es) {
            val key = e.key
            assertSame(e, context[key])
        }
        val set = mutableSetOf<CoroutineContext.Element>()
        context.fold(set) { s, e -> s.apply { add(e) } }
        assertEquals(set, es.toSet())
        when (es.size) {
            0 -> assertSame(context, EmptyCoroutineContext)
            1 -> assertSame(context, es[0])
        }
    }

    class DataElement(val data: Int) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<DataElement>
    }

    class OtherElement(val data: Int) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<DataElement>
    }

    data class WrapperKey(val key: String) : CoroutineContext.Key<WrapperElement>

    class WrapperElement(key: String) : CoroutineContext.Element {
        override val key = WrapperKey(key)
    }

    class CustomContinuationInterceptor : ContinuationInterceptor {
        override val key: CoroutineContext.Key<*>
            get() = ContinuationInterceptor

        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            continuation
    }
}
