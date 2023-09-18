/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding

/**
 * This embedding represents a signature of a callable object.
 * In case the method has a receiver it becomes the first argument of the function.
 * Example: Foo.bar(x: Int) --> Foo$bar(this: Foo, x: Int)
 */
interface CallableSignature {
    val receiverType: TypeEmbedding?
    val paramTypes: List<TypeEmbedding>
    val returnType: TypeEmbedding

    val formalArgTypes: List<TypeEmbedding>
        get() = listOfNotNull(receiverType) + paramTypes
}
