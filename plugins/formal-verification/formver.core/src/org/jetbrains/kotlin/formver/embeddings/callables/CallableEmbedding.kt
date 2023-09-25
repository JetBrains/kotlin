/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.conversion.withResult
import org.jetbrains.kotlin.formver.embeddings.BooleanTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Stmt

/**
 * Kotlin entity that can be called.
 *
 * Should be used exclusively through `insertCall` below.
 */
interface CallableEmbedding : CallableSignature {
    /**
     * Indicates whether the function *call* can throw.
     *
     * Note that this is generally false for everything inlined: the inlined body may throw,
     * but the call itself typically can't.
     */
    val canThrow: Boolean

    fun insertCallImpl(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding
}

fun CallableEmbedding.insertCall(args: List<ExpEmbedding>, ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding {
    // If this method call can throw, then instead of obtaining a result, we can jump to any currently
    // active catch label. From the point of view of Viper, the method call never happened.
    // This is not entirely accurate, since this means that we did not relinquish any permissions to
    // this method call; in our current implementation that does not matter, as we never pass permissions.
    if (canThrow) {
        for (label in ctx.activeCatchLabels) {
            ctx.withResult(BooleanTypeEmbedding) {
                val block = withNewScopeToBlock {
                    addStatement(label.toGoto())
                }
                addStatement(Stmt.If(resultExp.toViper(), block, Stmt.Seqn()))
            }
        }
    }
    return args.zip(formalArgTypes)
        .map { (arg, type) -> arg.withType(type) }
        .let { insertCallImpl(it, ctx) }
        .withType(returnType)
}