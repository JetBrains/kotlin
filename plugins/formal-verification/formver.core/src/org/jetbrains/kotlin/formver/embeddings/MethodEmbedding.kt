/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.ast.UserMethod

class MethodEmbedding(
    val signature: MethodSignatureEmbedding,
    val preconditions: List<Exp>,
    val postconditions: List<Exp>,
    val body: Stmt.Seqn?,
    val isInline: Boolean,
) : MethodSignatureEmbedding by signature {
    val shouldIncludeInProgram = !isInline || body != null
    val viperMethod = UserMethod(
        name,
        formalArgs.map { it.toLocalVarDecl() },
        returnVar.toLocalVarDecl(),
        preconditions,
        postconditions,
        body
    )
}