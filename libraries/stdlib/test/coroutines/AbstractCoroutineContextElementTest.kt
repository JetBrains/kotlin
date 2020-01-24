/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.coroutines

import kotlin.coroutines.*
import kotlin.test.*

class AbstractCoroutineContextElementTest {

    private val CoroutineContext.size get() = fold(0) { acc, _ -> acc + 1 }

    abstract class Base : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Base>

        override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? = getPolymorphicElement(key)
        override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext = minusPolymorphicKey(key)
    }

    class DerivedWithoutKey : Base() {
        // No custom key
    }

    open class DerivedWithKey : Base() {
        companion object Key : AbstractCoroutineContextKey<Base, DerivedWithKey>(Base, { it as? DerivedWithKey })
    }

    class SubDerivedWithKey : DerivedWithKey() {
        companion object Key : AbstractCoroutineContextKey<Base, SubDerivedWithKey>(Base, { it as? SubDerivedWithKey })
    }

    class SubDerivedWithKeyAndDifferentBase : DerivedWithKey() {
        // Note how different base class is used
        companion object Key :
            AbstractCoroutineContextKey<DerivedWithKey, SubDerivedWithKeyAndDifferentBase>(
                DerivedWithKey,
                { it as? SubDerivedWithKeyAndDifferentBase })
    }

    object IrrelevantElement : AbstractCoroutineContextElement(Key) {
        object Key : CoroutineContext.Key<IrrelevantElement>
    }

    @Test
    fun testDerivedWithoutKey() {
        val derivedWithoutKey = DerivedWithoutKey()
        assertSame(Base.Key, derivedWithoutKey.key)
        testDerivedWithoutKey(EmptyCoroutineContext, derivedWithoutKey) // Single element
        testDerivedWithoutKey(IrrelevantElement, derivedWithoutKey) // Combined context
    }

    @Test
    fun testDerivedWithoutKeyOverridesDerived() {
        val context = DerivedWithKey() + DerivedWithoutKey()
        assertEquals(1, context.size)
        assertTrue(context[Base] is DerivedWithoutKey)
        assertNull(context[DerivedWithKey])
        assertEquals(EmptyCoroutineContext, context.minusKey(Base))
        assertSame(context, context.minusKey(DerivedWithKey))
    }

    private fun testDerivedWithoutKey(context: CoroutineContext, element: CoroutineContext.Element) {
        val ctx = context + element
        assertEquals(context.size + 1, ctx.size)
        assertSame(element, ctx[Base]!!)
        assertNull(ctx[DerivedWithKey])
        assertEquals(context, ctx.minusKey(Base))
        assertSame(ctx, ctx.minusKey(DerivedWithKey))
    }

    @Test
    fun testDerivedWithKey() {
        val derivedWithKey = DerivedWithKey()
        assertSame(Base.Key, derivedWithKey.key)
        testDerivedWithKey(EmptyCoroutineContext, derivedWithKey) // Single element
        testDerivedWithKey(IrrelevantElement, derivedWithKey) // Combined context
    }

    private fun testDerivedWithKey(context: CoroutineContext, element: CoroutineContext.Element) {
        val ctx = context + element
        assertEquals(context.size + 1, ctx.size)
        assertSame(element, ctx[Base]!!)
        assertSame(element, ctx[DerivedWithKey]!!)
        assertEquals(context, ctx.minusKey(Base))
        assertEquals(context, ctx.minusKey(DerivedWithKey))
    }

    @Test
    fun testSubDerivedWithKey() {
        val subDerivedWithKey = SubDerivedWithKey()
        assertSame(Base.Key, subDerivedWithKey.key)
        testSubDerivedWithKey(EmptyCoroutineContext, subDerivedWithKey)
        testSubDerivedWithKey(IrrelevantElement, subDerivedWithKey)
    }

    private fun testSubDerivedWithKey(context: CoroutineContext, element: CoroutineContext.Element) {
        val ctx = context + element
        assertEquals(context.size + 1, ctx.size)
        assertSame(element, ctx[Base]!!)
        assertSame(element, ctx[DerivedWithKey]!!)
        assertSame(element, ctx[SubDerivedWithKey]!!)
        assertNull(ctx[SubDerivedWithKeyAndDifferentBase])
        assertEquals(context, ctx.minusKey(Base))
        assertEquals(context, ctx.minusKey(DerivedWithKey))
        assertEquals(context, ctx.minusKey(SubDerivedWithKey))
        assertSame(ctx, ctx.minusKey(SubDerivedWithKeyAndDifferentBase))
    }

    @Test
    fun testSubDerivedWithKeyAndDifferentBase() {
        val subDerivedWithKeyAndDifferentBase = SubDerivedWithKeyAndDifferentBase()
        assertSame(Base.Key, subDerivedWithKeyAndDifferentBase.key)
        testSubDerivedWithKeyAndDifferentBase(EmptyCoroutineContext, subDerivedWithKeyAndDifferentBase)
        testSubDerivedWithKeyAndDifferentBase(IrrelevantElement, subDerivedWithKeyAndDifferentBase)
    }

    private fun testSubDerivedWithKeyAndDifferentBase(context: CoroutineContext, element: CoroutineContext.Element) {
        val ctx = context + element
        assertEquals(context.size + 1, ctx.size)
        assertSame(element, ctx[Base]!!)
        assertSame(element, ctx[DerivedWithKey]!!)
        assertSame(element, ctx[SubDerivedWithKeyAndDifferentBase]!!)
        assertNull(ctx[SubDerivedWithKey])
        assertEquals(context, ctx.minusKey(Base))
        assertEquals(context, ctx.minusKey(DerivedWithKey))
        assertEquals(context, ctx.minusKey(SubDerivedWithKeyAndDifferentBase))
        assertSame(ctx, ctx.minusKey(SubDerivedWithKey))
    }

    @Test
    fun testDerivedWithKeyOverridesDerived() {
        val context = DerivedWithoutKey() + DerivedWithKey()
        assertEquals(1, context.size)
        assertTrue { context[Base] is DerivedWithKey }
        assertTrue { context[DerivedWithKey] is DerivedWithKey }
        assertEquals(EmptyCoroutineContext, context.minusKey(Base))
        assertEquals(EmptyCoroutineContext, context.minusKey(DerivedWithKey))
    }

    @Test
    fun testSubDerivedOverrides() {
        val key = SubDerivedWithKeyAndDifferentBase
        testSubDerivedOverrides<SubDerivedWithKeyAndDifferentBase>(DerivedWithoutKey() + SubDerivedWithKeyAndDifferentBase(), key)
        testSubDerivedOverrides<SubDerivedWithKeyAndDifferentBase>(DerivedWithKey() + SubDerivedWithKeyAndDifferentBase(), key)
        testSubDerivedOverrides<SubDerivedWithKeyAndDifferentBase>(SubDerivedWithKeyAndDifferentBase() + SubDerivedWithKeyAndDifferentBase(), key)
    }

    @Test
    fun testSubDerivedWithDifferentBaseOverrides() {
        val key = SubDerivedWithKey
        testSubDerivedOverrides<SubDerivedWithKey>(DerivedWithoutKey() + SubDerivedWithKey(), key)
        testSubDerivedOverrides<SubDerivedWithKey>(DerivedWithKey() + SubDerivedWithKey(), key)
        testSubDerivedOverrides<SubDerivedWithKey>(SubDerivedWithKeyAndDifferentBase() + SubDerivedWithKey(), key)
    }

    private inline fun <reified T : CoroutineContext.Element> testSubDerivedOverrides(context: CoroutineContext, key: CoroutineContext.Key<T>) {
        assertEquals(1, context.size)
        assertTrue { context[Base] is DerivedWithKey }
        assertTrue { context[DerivedWithKey] is DerivedWithKey }
        assertTrue { context[DerivedWithKey] is T }
        assertTrue { context[key] is T }
        assertEquals(EmptyCoroutineContext, context.minusKey(Base))
        assertEquals(EmptyCoroutineContext, context.minusKey(DerivedWithKey))
    }
}
