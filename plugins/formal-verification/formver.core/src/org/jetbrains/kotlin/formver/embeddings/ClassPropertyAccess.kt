/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.withResult
import org.jetbrains.kotlin.formver.embeddings.callables.CallableEmbedding
import org.jetbrains.kotlin.formver.embeddings.callables.insertCall
import org.jetbrains.kotlin.formver.viper.ast.Stmt

// We assume that thanks to the checks done by the Kotlin compiler, a property with a
// missing getter or setter will never be accessed.
class ClassPropertyAccess(val receiver: ExpEmbedding, val getter: GetterEmbedding?, val setter: SetterEmbedding?) :
    PropertyAccessEmbedding {
    override fun getValue(ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding = getter!!.getValue(receiver, ctx)

    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>) {
        setter!!.setValue(receiver, value, ctx)
    }
}

abstract class BackingFieldAccess(val field: FieldEmbedding) {
    fun <RTC : ResultTrackingContext> access(
        receiver: ExpEmbedding,
        ctx: StmtConversionContext<RTC>,
        action: StmtConversionContext<RTC>.(access: FieldAccess) -> Unit,
    ) {
        val fieldAccess = FieldAccess(receiver, field)
        val accPred = fieldAccess.getAccessPredicate()
        ctx.addStatement(Stmt.Inhale(accPred))
        ctx.action(fieldAccess)
        ctx.addStatement(Stmt.Exhale(accPred))
    }
}

class BackingFieldGetter(field: FieldEmbedding) : BackingFieldAccess(field), GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        ctx.withResult(field.type) {
            access(receiver, this) {
                addStatement(Stmt.assign(resultExp.toViper(), it.toViper()))
                field.type.provenInvariants(resultExp.toViper()).forEach { inv ->
                    addStatement(Stmt.Inhale(inv))
                }
            }
        }
}

class BackingFieldSetter(field: FieldEmbedding) : BackingFieldAccess(field), SetterEmbedding {
    override fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>) {
        access(receiver, ctx) {
            addStatement(Stmt.assign(it.toViper(), value.withType(field.type).toViper()))
        }
    }
}

class CustomGetter(val getterMethod: CallableEmbedding) : GetterEmbedding {
    override fun getValue(receiver: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding =
        getterMethod.insertCall(listOf(receiver), ctx)
}

class CustomSetter(val setterMethod: CallableEmbedding) : SetterEmbedding {
    override fun setValue(receiver: ExpEmbedding, value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>) {
        setterMethod.insertCall(listOf(receiver, value), ctx)
    }
}
