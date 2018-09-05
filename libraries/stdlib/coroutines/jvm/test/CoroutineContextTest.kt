/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.kotlin.coroutines

import kotlin.coroutines.*
import kotlin.test.*

class CoroutineContextTest {
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
        val size = context.fold(0) { c, _ -> c + 1}
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