/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.viper.ast.Method

/**
 * A callable embedding that is aware of how it will be called in the resulting Viper code.
 */
interface ViperAwareCallableEmbedding : CallableEmbedding {
    fun toViperMethod(): Method?
}