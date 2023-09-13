/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.viper.ast.Stmt

class LocalPropertyAccess(val variable: VariableEmbedding) : PropertyAccessEmbedding {
    override fun getValue(ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding = variable

    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>) {
        ctx.addStatement(Stmt.assign(variable.toViper(), value.withType(variable.type).toViper()))
    }
}