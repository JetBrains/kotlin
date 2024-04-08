/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

data object UnitLit : UnitResultExpEmbedding {
    // No operation: we just want to return unit.
    override fun toViperSideEffects(ctx: LinearizationContext) = Unit

    override val debugTreeView: TreeView
        get() = PlaintextLeaf("Unit")
}

data class IntLit(val value: Int) : PureExpEmbedding {
    override val type = IntTypeEmbedding
    override fun toViper(source: KtSourceElement?): Exp =
        RuntimeTypeDomain.intInjection.toRef(
            Exp.IntLit(value, source.asPosition, sourceRole.asInfo),
            pos = source.asPosition,
            info = sourceRole.asInfo
        )

    override val debugName: String
        get() = "Int"

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(PlaintextLeaf(value.toString()))
}

data class BooleanLit(val value: Boolean, override val sourceRole: SourceRole? = null) : PureExpEmbedding {
    override val type = BooleanTypeEmbedding
    override fun toViper(source: KtSourceElement?): Exp =
        RuntimeTypeDomain.boolInjection.toRef(
            Exp.BoolLit(value, source.asPosition, sourceRole.asInfo),
            pos = source.asPosition,
            info = sourceRole.asInfo
        )

    override val debugName: String
        get() = "Boolean"

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(PlaintextLeaf(value.toString()))
}

data object NullLit : PureExpEmbedding {
    override val type = NullableTypeEmbedding(NothingTypeEmbedding)
    override fun toViper(source: KtSourceElement?): Exp =
        RuntimeTypeDomain.nullValue(pos = source.asPosition)

    override val debugName: String
        get() = "Null"

    override val debugExtraSubtrees: List<TreeView>
        get() = listOf(PlaintextLeaf("null"))
}

