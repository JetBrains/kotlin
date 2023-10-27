/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.UnitLit
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.embeddings.withType
import org.jetbrains.kotlin.formver.linearization.pureToViper
import org.jetbrains.kotlin.formver.viper.ast.Stmt

interface ResultTrackingContext {
    /** Expression with the result of the computation.
     */
    val resultExp: ExpEmbedding
    fun capture(exp: ExpEmbedding)
}

object NoopResultTracker : ResultTrackingContext {
    override val resultExp = UnitLit
    override fun capture(exp: ExpEmbedding) {
        // do nothing
    }
}

abstract class VarResultTrackingContext(val resultVar: VariableEmbedding) : ResultTrackingContext {
    override val resultExp: VariableEmbedding = resultVar
}

interface ResultTrackerFactory<RTC : ResultTrackingContext> {
    fun build(ctx: StmtConversionContext<RTC>): RTC
}

object NoopResultTrackerFactory : ResultTrackerFactory<NoopResultTracker> {
    override fun build(ctx: StmtConversionContext<NoopResultTracker>): NoopResultTracker = NoopResultTracker
}

class VarResultTrackerFactory(val resultVar: VariableEmbedding) : ResultTrackerFactory<VarResultTrackingContext> {
    override fun build(ctx: StmtConversionContext<VarResultTrackingContext>): VarResultTrackingContext =
        object : VarResultTrackingContext(resultVar) {
            override fun capture(exp: ExpEmbedding) {
                ctx.addStatement(Stmt.assign(resultExp.pureToViper(), exp.withType(resultVar.type).pureToViper()))
            }
        }
}
