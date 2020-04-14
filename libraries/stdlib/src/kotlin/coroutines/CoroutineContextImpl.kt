/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines

import kotlin.coroutines.CoroutineContext.Element
import kotlin.coroutines.CoroutineContext.Key

/**
 * Base class for [CoroutineContext.Element] implementations.
 */
@SinceKotlin("1.3")
public abstract class AbstractCoroutineContextElement(public override val key: Key<*>) : Element

/**
 * Base class for [CoroutineContext.Key] associated with polymorphic [CoroutineContext.Element] implementation.
 * Polymorphic element implementation implies delegating its [get][Element.get] and [minusKey][Element.minusKey]
 * to [getPolymorphicElement] and [minusPolymorphicKey] respectively.
 *
 * Polymorphic elements can be extracted from the coroutine context using both element key and its supertype key.
 * Example of polymorphic elements:
 * ```
 * open class BaseElement : CoroutineContext.Element {
 *     companion object Key : CoroutineContext.Key<BaseElement>
 *     override val key: CoroutineContext.Key<*> get() = Key
 *     // It is important to use getPolymorphicKey and minusPolymorphicKey
 *     override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? = getPolymorphicElement(key)
 *     override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext = minusPolymorphicKey(key)
 * }
 *
 * class DerivedElement : BaseElement() {
 *     companion object Key : AbstractCoroutineContextKey<BaseElement, DerivedElement>(BaseElement, { it as? DerivedElement })
 * }
 * // Now it is possible to query both `BaseElement` and `DerivedElement`
 * someContext[BaseElement] // Returns BaseElement?, non-null both for BaseElement and DerivedElement instances
 * someContext[DerivedElement] // Returns DerivedElement?, non-null only for DerivedElement instance
 * ```
 * @param B base class of a polymorphic element
 * @param baseKey an instance of base key
 * @param E element type associated with the current key
 * @param safeCast a function that can safely cast abstract [CoroutineContext.Element] to the concrete [E] type
 *                 and return the element if it is a subtype of [E] or `null` otherwise.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public abstract class AbstractCoroutineContextKey<B : Element, E : B>(
    baseKey: Key<B>,
    private val safeCast: (element: Element) -> E?
) : Key<E> {
    private val topmostKey: Key<*> = if (baseKey is AbstractCoroutineContextKey<*, *>) baseKey.topmostKey else baseKey

    internal fun tryCast(element: Element): E? = safeCast(element)
    internal fun isSubKey(key: Key<*>): Boolean = key === this || topmostKey === key
}

/**
 * Returns the current element if it is associated with the given [key] in a polymorphic manner or `null` otherwise.
 * This method returns non-null value if either [Element.key] is equal to the given [key] or if the [key] is associated
 * with [Element.key] via [AbstractCoroutineContextKey].
 * See [AbstractCoroutineContextKey] for the example of usage.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public fun <E : Element> Element.getPolymorphicElement(key: Key<E>): E? {
    if (key is AbstractCoroutineContextKey<*, *>) {
        @Suppress("UNCHECKED_CAST")
        return if (key.isSubKey(this.key)) key.tryCast(this) as? E else null
    }
    @Suppress("UNCHECKED_CAST")
    return if (this.key === key) this as E else null
}

/**
 * Returns empty coroutine context if the element is associated with the given [key] in a polymorphic manner
 * or `null` otherwise.
 * This method returns empty context if either [Element.key] is equal to the given [key] or if the [key] is associated
 * with [Element.key] via [AbstractCoroutineContextKey].
 * See [AbstractCoroutineContextKey] for the example of usage.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public fun Element.minusPolymorphicKey(key: Key<*>): CoroutineContext {
    if (key is AbstractCoroutineContextKey<*, *>) {
        return if (key.isSubKey(this.key) && key.tryCast(this) != null) EmptyCoroutineContext else this
    }
    return if (this.key === key) EmptyCoroutineContext else this
}

/**
 * An empty coroutine context.
 */
@SinceKotlin("1.3")
public object EmptyCoroutineContext : CoroutineContext, Serializable {
    private const val serialVersionUID: Long = 0
    private fun readResolve(): Any = EmptyCoroutineContext

    public override fun <E : Element> get(key: Key<E>): E? = null
    public override fun <R> fold(initial: R, operation: (R, Element) -> R): R = initial
    public override fun plus(context: CoroutineContext): CoroutineContext = context
    public override fun minusKey(key: Key<*>): CoroutineContext = this
    public override fun hashCode(): Int = 0
    public override fun toString(): String = "EmptyCoroutineContext"
}

//--------------------- internal impl ---------------------

// this class is not exposed, but is hidden inside implementations
// this is a left-biased list, so that `plus` works naturally
@SinceKotlin("1.3")
internal class CombinedContext(
    private val left: CoroutineContext,
    private val element: Element
) : CoroutineContext, Serializable {

    override fun <E : Element> get(key: Key<E>): E? {
        var cur = this
        while (true) {
            cur.element[key]?.let { return it }
            val next = cur.left
            if (next is CombinedContext) {
                cur = next
            } else {
                return next[key]
            }
        }
    }

    public override fun <R> fold(initial: R, operation: (R, Element) -> R): R =
        operation(left.fold(initial, operation), element)

    public override fun minusKey(key: Key<*>): CoroutineContext {
        element[key]?.let { return left }
        val newLeft = left.minusKey(key)
        return when {
            newLeft === left -> this
            newLeft === EmptyCoroutineContext -> element
            else -> CombinedContext(newLeft, element)
        }
    }

    private fun size(): Int {
        var cur = this
        var size = 2
        while (true) {
            cur = cur.left as? CombinedContext ?: return size
            size++
        }
    }

    private fun contains(element: Element): Boolean =
        get(element.key) == element

    private fun containsAll(context: CombinedContext): Boolean {
        var cur = context
        while (true) {
            if (!contains(cur.element)) return false
            val next = cur.left
            if (next is CombinedContext) {
                cur = next
            } else {
                return contains(next as Element)
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is CombinedContext && other.size() == size() && other.containsAll(this)

    override fun hashCode(): Int = left.hashCode() + element.hashCode()

    override fun toString(): String =
        "[" + fold("") { acc, element ->
            if (acc.isEmpty()) element.toString() else "$acc, $element"
        } + "]"

    private fun writeReplace(): Any {
        val n = size()
        val elements = arrayOfNulls<CoroutineContext>(n)
        var index = 0
        fold(Unit) { _, element -> elements[index++] = element }
        check(index == n)
        @Suppress("UNCHECKED_CAST")
        return Serialized(elements as Array<CoroutineContext>)
    }

    private class Serialized(val elements: Array<CoroutineContext>) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 0L
        }

        private fun readResolve(): Any = elements.fold(EmptyCoroutineContext, CoroutineContext::plus)
    }
}
