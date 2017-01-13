/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.coroutines

/**
 * Base class for [CoroutineContextElement] implementations.
 */
@SinceKotlin("1.1")
public abstract class AbstractCoroutineContextElement : CoroutineContextElement {
    @Suppress("UNCHECKED_CAST")
    public override operator fun <E : CoroutineContextElement> get(key: CoroutineContextKey<E>): E? =
            if (this.contextKey == key) this as E else null

    public override fun <R> fold(initial: R, operation: (R, CoroutineContextElement) -> R): R =
            operation(initial, this)

    public override operator fun plus(context: CoroutineContext): CoroutineContext =
            plusImpl(context)

    public override fun minusKey(key: CoroutineContextKey<*>): CoroutineContext =
            if (this.contextKey == key) EmptyCoroutineContext else this
}

/**
 * An empty coroutine context.
 */
@SinceKotlin("1.1")
public object EmptyCoroutineContext : CoroutineContext {
    public override fun <E : CoroutineContextElement> get(key: CoroutineContextKey<E>): E? = null
    public override fun <R> fold(initial: R, operation: (R, CoroutineContextElement) -> R): R = initial
    public override fun plus(context: CoroutineContext): CoroutineContext = context
    public override fun minusKey(key: CoroutineContextKey<*>): CoroutineContext = this
    public override fun hashCode(): Int = 0
    public override fun toString(): String = "EmptyCoroutineContext"
}

//--------------------- private impl ---------------------

// this class is not exposed, but is hidden inside implementations
// this is a left-biased list, so that `plus` works naturally
private class CombinedContext(val left: CoroutineContext, val element: CoroutineContextElement) : CoroutineContext {
    override fun <E : CoroutineContextElement> get(key: CoroutineContextKey<E>): E? {
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

    public override fun <R> fold(initial: R, operation: (R, CoroutineContextElement) -> R): R =
            operation(left.fold(initial, operation), element)

    public override operator fun plus(context: CoroutineContext): CoroutineContext =
            plusImpl(context)

    public override fun minusKey(key: CoroutineContextKey<*>): CoroutineContext {
        element[key]?.let { return left }
        val newLeft = left.minusKey(key)
        return when (newLeft) {
            left -> this
            EmptyCoroutineContext -> element
            else -> CombinedContext(newLeft, element)
        }
    }

    private fun size(): Int =
            if (left is CombinedContext) left.size() + 1 else 2

    private fun contains(element: CoroutineContextElement): Boolean =
            get(element.contextKey) == element

    private fun containsAll(context: CombinedContext): Boolean {
        var cur = context
        while (true) {
            if (!contains(cur.element)) return false
            val next = cur.left
            if (next is CombinedContext) {
                cur = next
            } else {
                return contains(next as CoroutineContextElement)
            }
        }
    }

    override fun equals(other: Any?): Boolean =
            this === other || other is CombinedContext && other.size() == size() && other.containsAll(this)

    override fun hashCode(): Int = left.hashCode() + element.hashCode()

    override fun toString(): String =
            "[" + fold("") { acc, element ->
                if (acc.isEmpty()) element.toString() else acc + ", " + element
            } + "]"
}

private fun CoroutineContext.plusImpl(context: CoroutineContext): CoroutineContext =
        if (context == EmptyCoroutineContext) this else // fast path -- avoid lambda creation
            context.fold(this) { acc, element ->
                val removed = acc.minusKey(element.contextKey)
                if (removed == EmptyCoroutineContext) element else {
                    // make sure interceptor is always last in the context (and thus is fast to get when present)
                    val interceptor = removed[ContinuationInterceptor]
                    if (interceptor == null) CombinedContext(removed, element) else {
                        val left = removed.minusKey(ContinuationInterceptor)
                        if (left == EmptyCoroutineContext) CombinedContext(element, interceptor) else
                            CombinedContext(CombinedContext(left, element), interceptor)
                    }
                }
            }

