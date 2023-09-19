/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding

/**
 * This embedding represents a signature of a callable object without name information.
 */

interface CallableSignature {
    val receiverType: TypeEmbedding?
    val paramTypes: List<TypeEmbedding>
    val returnType: TypeEmbedding

    /**
     * The flattened structure of the callable parameters: in case the callable has a receiver
     * it becomes the first argument of the function.
     *
     * `Foo.(Int) -> Int --> (Foo, Int) -> Int`
     */
    val formalArgTypes: List<TypeEmbedding>
        get() = listOfNotNull(receiverType) + paramTypes
}

/**
 * An instance of `CallableSignature` that is guaranteed to be `data`.
 */
data class CallableSignatureData(
    override val receiverType: TypeEmbedding?,
    override val paramTypes: List<TypeEmbedding>,
    override val returnType: TypeEmbedding,
) : CallableSignature

val CallableSignature.asData: CallableSignatureData
    get() = CallableSignatureData(receiverType, paramTypes, returnType)