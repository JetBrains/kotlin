/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.embeddings.FunctionBodyEmbedding
import org.jetbrains.kotlin.formver.viper.ast.Method

interface FunctionEmbedding : CallableEmbedding {
    val viperMethod: Method?
}

/**
 * An embedding of a user-defined function.
 */
class UserFunctionEmbedding(private val callable: RichCallableEmbedding) : FunctionEmbedding,
    CallableEmbedding by callable {
    /**
     * The presence of the body indicates that the function should be verified, as opposed to simply having a declaration available.
     */
    var body: FunctionBodyEmbedding? = null

    override val viperMethod: Method?
        get() = body?.toViperMethod(callable) ?: callable.toViperMethodHeader()
}
