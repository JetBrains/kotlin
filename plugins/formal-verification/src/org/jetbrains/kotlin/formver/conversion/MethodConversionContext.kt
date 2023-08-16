package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.MethodSignatureEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.VariableEmbedding

interface MethodConversionContext : ProgramConversionContext {
    val signature: MethodSignatureEmbedding

    fun newAnonVar(type: TypeEmbedding): VariableEmbedding
}