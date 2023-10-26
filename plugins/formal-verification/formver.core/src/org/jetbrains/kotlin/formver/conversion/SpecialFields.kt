/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.FieldEmbedding
import org.jetbrains.kotlin.formver.embeddings.IntTypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.names.SpecialName
import org.jetbrains.kotlin.formver.viper.MangledName

class SpecialField(baseName: String, override val type: TypeEmbedding) : FieldEmbedding {
    override val name: MangledName = SpecialName(baseName)
    override val accessPolicy: AccessPolicy = AccessPolicy.ALWAYS_WRITEABLE
    override val includeInShortDump: Boolean = false
}

object SpecialFields {
    val FunctionObjectCallCounterField = SpecialField("function_object_call_counter", IntTypeEmbedding)
    val all = listOf(
        FunctionObjectCallCounterField,
    )
}