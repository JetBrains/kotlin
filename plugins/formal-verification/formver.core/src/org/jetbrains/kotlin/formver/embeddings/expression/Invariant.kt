/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.conversion.SpecialFields.FunctionObjectCallCounterField
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.callables.SpecialFunctions
import org.jetbrains.kotlin.formver.linearization.LinearizationContext
import org.jetbrains.kotlin.formver.viper.ast.Exp

data class Old(override val inner: ExpEmbedding) : UnaryDirectResultExpEmbedding {
    override val type: TypeEmbedding = inner.type
    override fun toViper(ctx: LinearizationContext): Exp = Exp.Old(inner.toViper(ctx), ctx.source.asPosition)
    override fun toViperBuiltinType(ctx: LinearizationContext): Exp = Exp.Old(inner.toViperBuiltinType(ctx), ctx.source.asPosition)
}

data class DuplicableCall(override val inner: ExpEmbedding) : UnaryDirectResultExpEmbedding, OnlyToBuiltinTypeExpEmbedding {
    override val type: TypeEmbedding = BooleanTypeEmbedding

    override val sourceRole = SourceRole.ParamFunctionLeakageCheck(
        inner.ignoringCastsAndMetaNodes().sourceRole as? SourceRole.FirSymbolHolder
            ?: error("Parameter of a duplicable function must be a fir symbol.")
    )

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        SpecialFunctions.duplicableFunction(inner.toViper(ctx), pos = ctx.source.asPosition, info = sourceRole.asInfo)
}

data class FunctionObjectCallsPrimitiveAccess(override val inner: ExpEmbedding) : UnaryDirectResultExpEmbedding,
    OnlyToBuiltinTypeExpEmbedding {
    override val type = IntTypeEmbedding

    override fun toViperBuiltinType(ctx: LinearizationContext): Exp =
        Exp.FieldAccess(inner.toViper(ctx), FunctionObjectCallCounterField.toViper(), pos = ctx.source.asPosition)

}

