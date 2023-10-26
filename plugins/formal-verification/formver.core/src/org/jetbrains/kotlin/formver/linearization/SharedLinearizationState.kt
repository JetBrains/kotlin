/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.linearization

import org.jetbrains.kotlin.formver.conversion.FreshEntityProducer
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding
import org.jetbrains.kotlin.formver.names.AnonymousName

class SharedLinearizationState {
    private val producer = FreshEntityProducer { AnonymousName(it) }
    val assumptionTracker = AssumptionTracker()

    fun freshVar(type: TypeEmbedding) = VariableEmbedding(producer.getFresh(), type)
}