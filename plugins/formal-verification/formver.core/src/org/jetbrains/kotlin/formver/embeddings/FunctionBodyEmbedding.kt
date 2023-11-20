/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.ReturnTarget
import org.jetbrains.kotlin.formver.embeddings.callables.FullNamedFunctionSignature
import org.jetbrains.kotlin.formver.embeddings.callables.toViperMethod
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Stmt

data class FunctionBodyEmbedding(val viperBody: Stmt.Seqn, val returnTarget: ReturnTarget, val debugExpEmbedding: ExpEmbedding? = null) {
    fun toViperMethod(signature: FullNamedFunctionSignature): Method =
        signature.toViperMethod(viperBody, returnTarget.variable)
}
