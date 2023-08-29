/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.viper.ast.Type

class ClassEmbedding(
    val name: ClassName,
    val fields: List<VariableEmbedding>,
) : TypeEmbedding {

    override val type: Type
        get() = Type.Ref
}