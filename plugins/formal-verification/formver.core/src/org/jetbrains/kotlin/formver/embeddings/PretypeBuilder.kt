/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.embeddings.callables.CallableSignatureData
import org.jetbrains.kotlin.formver.names.ScopedKotlinName

/**
 * We use "pretype" to refer to types that do not contain information on nullability or
 * other flags.
 */
interface PretypeBuilder {
    /**
     * Turn the builder into a `TypeEmbedding`.
     *
     * We allow this deferral so that the build can be done in any order.
     */
    fun complete(): TypeEmbedding
}

object UnitPretypeBuilder : PretypeBuilder {
    override fun complete(): TypeEmbedding = UnitTypeEmbedding
}

object NothingPretypeBuilder : PretypeBuilder {
    override fun complete(): TypeEmbedding = NothingTypeEmbedding
}

object AnyPretypeBuilder : PretypeBuilder {
    override fun complete(): TypeEmbedding = AnyTypeEmbedding
}

object IntPretypeBuilder : PretypeBuilder {
    override fun complete(): TypeEmbedding = IntTypeEmbedding
}

object BooleanPretypeBuilder : PretypeBuilder {
    override fun complete(): TypeEmbedding = BooleanTypeEmbedding
}

class FunctionPretypeBuilder : PretypeBuilder {
    private val paramTypes = mutableListOf<TypeEmbedding>()
    private var receiverType: TypeEmbedding? = null
    private var returnType: TypeEmbedding? = null

    fun withParam(paramInit: TypeBuilder.() -> PretypeBuilder) {
        paramTypes.add(buildType { paramInit() })
    }

    fun withReceiver(receiverInit: TypeBuilder.() -> PretypeBuilder) {
        require(receiverType == null) { "Receiver already set" }
        receiverType = buildType { receiverInit() }
    }

    fun withReturnType(returnTypeInit: TypeBuilder.() -> PretypeBuilder) {
        require(returnType == null) { "Return type already set" }
        returnType = buildType { returnTypeInit() }
    }

    override fun complete(): TypeEmbedding {
        require(returnType != null) { "Return type not set" }
        return FunctionTypeEmbedding(CallableSignatureData(receiverType, paramTypes, returnType!!))
    }
}

class ClassPretypeBuilder : PretypeBuilder {
    private var className: ScopedKotlinName? = null

    fun withName(name: ScopedKotlinName) {
        require(className == null) { "Class name already set" }
        className = name
    }

    override fun complete(): TypeEmbedding {
        require(className != null) { "Class name not set" }
        return ClassTypeEmbedding(className!!)
    }
}
