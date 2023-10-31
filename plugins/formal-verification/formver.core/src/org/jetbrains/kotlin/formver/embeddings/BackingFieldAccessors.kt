/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.withResult
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.FieldAccess
import org.jetbrains.kotlin.formver.embeddings.expression.withType
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Stmt

abstract class BackingFieldAccess(val field: FieldEmbedding) {
    fun <RTC : ResultTrackingContext> access(
        receiver: ExpEmbedding,
        ctx: StmtConversionContext<RTC>,
        source: KtSourceElement?,
        action: StmtConversionContext<RTC>.(access: FieldAccess) -> Unit,
    ) {
        val invariant = field.accessInvariantForAccess()?.fillHole(receiver)
        invariant?.let {
            ctx.addStatement(Stmt.Inhale(it.pureToViper(), source.asPosition))
        }
        ctx.action(FieldAccess(receiver, field))
        invariant?.let {
            ctx.addStatement(Stmt.Exhale(it.pureToViper(), source.asPosition))
        }
    }
}

class BackingFieldGetter(field: FieldEmbedding) : BackingFieldAccess(field), GetterEmbedding {
    override fun getValue(
        receiver: ExpEmbedding,
        ctx: StmtConversionContext<ResultTrackingContext>,
        source: KtSourceElement?,
    ): ExpEmbedding =
        ctx.withResult(field.type) {
            access(receiver, this, source) {
                addStatement(Stmt.assign(resultExp.pureToViper(), it.pureToViper(), source.asPosition))
                field.type.provenInvariants().fillHoles(resultExp).forEach { inv ->
                    addStatement(Stmt.Inhale(inv.pureToViper(), source.asPosition))
                }
            }
        }
}

class BackingFieldSetter(field: FieldEmbedding) : BackingFieldAccess(field), SetterEmbedding {
    override fun setValue(
        receiver: ExpEmbedding,
        value: ExpEmbedding,
        ctx: StmtConversionContext<ResultTrackingContext>,
        source: KtSourceElement?,
    ) {
        access(receiver, ctx, source) {
            addStatement(Stmt.assign(it.pureToViper(), value.withType(field.type).pureToViper(), source.asPosition))
        }
    }
}

