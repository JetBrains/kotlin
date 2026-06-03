/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import java.util.IdentityHashMap
import kotlin.reflect.KClass

/**
 * Per-file index of IR elements grouped by `::class`.
 */
internal class IrElementIndex {
    private val buckets = mutableMapOf<KClass<out IrElement>, IrIndexBucket>()
    private var indexedTypes: Set<KClass<out IrElement>> = emptySet()

    /** Resets prior state. */
    fun buildFor(irFile: IrFile, vararg types: KClass<out IrElement>) {
        clear()
        indexedTypes = types.toHashSet()
        for (klass in indexedTypes) {
            buckets[klass] = IrIndexBucket()
        }
        walkAndRegisterDescendants(irFile, initialScope = null)
    }

    fun clear() {
        buckets.clear()
        indexedTypes = emptySet()
    }

    inline fun <reified T : IrElement> forEach(noinline action: (T) -> Unit) = snapshotForEach(T::class, action)
    inline fun <reified T : IrElement> any(noinline predicate: (T) -> Boolean): Boolean = snapshotAny(T::class, predicate)
    inline fun <reified T : IrElement> isEmpty(): Boolean = bucketSize(T::class) == 0

    fun scopeOf(element: IrElement): IrSymbol? {
        for (bucket in buckets.values) {
            val elementInfo = bucket.infoOf[element]
            if (elementInfo != null) return elementInfo.scope
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : IrElement> snapshotForEach(klass: KClass<T>, action: (T) -> Unit) =
            buckets[klass]?.snapshotForEach { action(it as T) }

    @Suppress("UNCHECKED_CAST")
    internal fun <T : IrElement> snapshotAny(klass: KClass<T>, predicate: (T) -> Boolean): Boolean =
            buckets[klass]?.snapshotAny { predicate(it as T) } ?: false

    internal fun bucketSize(klass: KClass<out IrElement>): Int = buckets[klass]?.items?.size ?: 0

    fun spliceIfNeeded(
            old: IrElement,
            new: IrElement,
            reindexOldSubtree: Boolean = true,
            reindexNewSubtree: Boolean = true,
    ) {
        if (old === new) return
        val (parent, scope) = lookupParentScope(old) ?: return
        // `IrElementTransformerVoid` breaks the visit-funnel at `visitDeclaration`, `visitExpression`,
        // and `visitBody` — overriding only `visitElement` would miss typed children (IrCall etc.)
        // and (worse) recurse into them via the default implementations.
        parent.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitElement(element: IrElement): IrElement =
                    if (element === old) new else element

            override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement =
                    if (declaration === old) new as IrStatement else declaration

            override fun visitExpression(expression: IrExpression): IrExpression =
                    if (expression === old) new as IrExpression else expression

            override fun visitBody(body: IrBody): IrBody =
                    if (body === old) new as IrBody else body
        })

        bucketFor(new)?.add(new, parent, scope)

        if (reindexOldSubtree) unregisterSubtreeOf(old)
        if (reindexNewSubtree) walkAndRegisterDescendants(new, initialScope = scope)
    }

    private fun bucketFor(element: IrElement) =
            indexedTypes.firstOrNull { it.isInstance(element) }
                    ?.let { buckets[it] }

    private fun walkAndRegisterDescendants(root: IrElement, initialScope: IrSymbol?) {
        val parentStack = ArrayDeque<IrElement>().apply { add(root) }
        val scopeStack = ArrayDeque<IrSymbol>()
        if (initialScope != null) scopeStack.addLast(initialScope)
        root.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                bucketFor(element)?.add(element, parentStack.last(), scopeStack.lastOrNull())
                val pushedScope = scopeOwnerSymbolOf(element)?.also { scopeStack.addLast(it) }
                parentStack.addLast(element)
                element.acceptChildrenVoid(this)
                parentStack.removeLast()
                if (pushedScope != null) scopeStack.removeLast()
            }
        })
    }

    private fun lookupParentScope(element: IrElement): Pair<IrElement, IrSymbol?>? {
        for (bucket in buckets.values) {
            val elementInfo = bucket.infoOf[element]
            if (elementInfo != null) return elementInfo.parent to elementInfo.scope
        }
        return null
    }

    private fun unregisterInternal(element: IrElement) {
        for (bucket in buckets.values) bucket.remove(element)
    }

    private fun unregisterSubtreeOf(root: IrElement) {
        root.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (bucketFor(element) != null) unregisterInternal(element)
                element.acceptChildrenVoid(this)
            }
        })
    }

    /** Mirrors the scope-owner set tracked by `IrBuildingTransformer`. */
    private fun scopeOwnerSymbolOf(element: IrElement): IrSymbol? = when (element) {
        is IrFunction -> element.symbol
        is IrField -> element.symbol
        is IrAnonymousInitializer -> element.symbol
        is IrEnumEntry -> element.symbol
        is IrScript -> element.symbol
        else -> null
    }

    private data class ElementInfo(
            var bucketPosition: Int,
            val parent: IrElement,
            val scope: IrSymbol?,
    )

    /** Swap-remove index bucket with [ArrayList] used to speed up iteration. */
    private class IrIndexBucket {
        val items = mutableListOf<IrElement>()
        val infoOf = IdentityHashMap<IrElement, ElementInfo>()

        fun add(element: IrElement, parent: IrElement, scope: IrSymbol?) {
            if (infoOf.containsKey(element)) return
            infoOf[element] = ElementInfo(items.size, parent, scope)
            items.add(element)
        }

        fun remove(element: IrElement) {
            val info = infoOf.remove(element) ?: return
            val lastIdx = items.size - 1
            val pos = info.bucketPosition
            if (pos != lastIdx) {
                val last = items[lastIdx]
                items[pos] = last
                infoOf[last]!!.bucketPosition = pos
            }
            items.removeAt(lastIdx)
        }

        fun snapshotForEach(action: (IrElement) -> Unit) = items.toList().filter { infoOf.containsKey(it) }.forEach(action)
        fun snapshotAny(predicate: (IrElement) -> Boolean): Boolean = items.toList().any { infoOf.containsKey(it) && predicate(it) }
    }
}
