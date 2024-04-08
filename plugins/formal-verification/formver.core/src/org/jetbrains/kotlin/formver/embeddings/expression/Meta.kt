/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.debug.NamedBranchingNode
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.embeddings.expression.debug.withDesignation
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Exp

data class WithPosition(override val inner: ExpEmbedding, val source: KtSourceElement) : PassthroughExpEmbedding {
    override fun <R> withPassthroughHook(ctx: LinearizationContext, action: LinearizationContext.() -> R): R =
        ctx.withPosition(source, action)

    override fun ignoringMetaNodes(): ExpEmbedding = inner.ignoringMetaNodes()
    override fun ignoringCastsAndMetaNodes(): ExpEmbedding = inner.ignoringCastsAndMetaNodes()

    // We ignore position information in the debug view.
    override val debugTreeView: TreeView
        get() = inner.debugTreeView
}

fun ExpEmbedding.withPosition(source: KtSourceElement?): ExpEmbedding =
    when {
        // Inner position is more specific anyway
        this is WithPosition -> this
        source == null -> this
        else -> WithPosition(this, source)
    }


/**
 * Represents a subtree in which a different subtree may appear multiple times.
 *
 * This is a complicated construction. There are cases when we want to use a source-level construction multiple times in the target
 * without recomputing it. For example, `f() ?: g()` should be translated to `if (f() != null) f() else g()`, but only one call
 * to `f()` should be produced per usage of `f() ?: g()`. Note that not all sharing should be tread like this: for example, we share
 * the condition node of a `while` loop, but it should be evaluated each time separately.
 *
 * This class contains the context that the sharing happens in, while the `sharedExp` is the result of the expression that is shared.
 *
 * This class should be used via the `share` function below. Do not do anything funny, or it will not work.
 */
data class SharingContext(override val inner: ExpEmbedding) : PassthroughExpEmbedding {
    var sharedExp: Exp? = null

    override fun <R> withPassthroughHook(ctx: LinearizationContext, action: LinearizationContext.() -> R): R =
        ctx.action().also { sharedExp = null }

    // We need a temporary variable here since Kotlin believes sharedExp may be modified at any point.
    fun tryInitShared(f: () -> Exp): Exp = when (val r = sharedExp) {
        null -> f().also { sharedExp = it }
        else -> r
    }

    override fun ignoringMetaNodes() = inner
    override fun ignoringCastsAndMetaNodes() = inner

    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "SharingContext",
            inner.debugTreeView,
            PlaintextLeaf(System.identityHashCode(this).toString()).withDesignation("ctxId")
        )
}

/**
 * Expression that is shared in a `SharedContext`.
 *
 * You should never need to create these explicitly, just use `share` below.
 *
 * This solution has some bugs: if a shared expression references a variable and that variable is modified
 * between the shared parts, the second occurrence may have a different value than the first. We can fix this
 * quite easily by using a fresh variable every time, but that would add unreasonable bloat to many programs.
 * TODO: fix this.
 */
data class Shared(val inner: ExpEmbedding) : StoredResultExpEmbedding, DefaultToBuiltinExpEmbedding {
    private var _context: SharingContext? = null
    val context: SharingContext
        get() = checkNotNull(_context) { "Context of shared used before initialisation is complete." }
    override val type: TypeEmbedding
        get() = inner.type

    override fun toViper(ctx: LinearizationContext): Exp = context.tryInitShared { inner.toViper(ctx) }

    override fun toViperStoringIn(result: VariableEmbedding, ctx: LinearizationContext) {
        context.tryInitShared { inner.toViperStoringIn(result, ctx); result.toLocalVarUse() }
    }

    override fun toViperUnusedResult(ctx: LinearizationContext) {
        context.tryInitShared { inner.toViperUnusedResult(ctx); UnitLit.pureToViper(toBuiltin = false) }
    }

    override fun ignoringMetaNodes() = inner
    override fun ignoringCastsAndMetaNodes() = inner

    fun initContext(ctx: SharingContext) {
        check(_context == null) { "Context of shared initialized twice." }
        _context = ctx
    }

    override val debugTreeView: TreeView
        get() = NamedBranchingNode(
            "Shared",
            inner.debugTreeView,
            PlaintextLeaf(System.identityHashCode(context).toString()).withDesignation("ctxId")
        )
}

fun share(toShare: ExpEmbedding, makeSharingScope: (ExpEmbedding) -> ExpEmbedding): ExpEmbedding {
    val shared = Shared(toShare)
    val context = SharingContext(makeSharingScope(shared))
    shared.initContext(context)
    return context
}
