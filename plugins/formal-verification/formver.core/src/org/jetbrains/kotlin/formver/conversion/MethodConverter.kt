/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.names.ReturnLabelName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.Name

/**
 * The symbol resolution data for a single method.
 *
 * Method converters are chained syntactically; the converter of a lambda has the method that the lambda is defined in as a parent.
 * In general, however, a callee inline function does *not* in general have its caller as a parent: this is because an inlined
 * function does not have access to the variables of its caller, so it does not make sense to have symbol resolution pass through it.
 *
 * We're using the term `MethodConverter` here for consistency with the `XConverter` implementing `XConversionContext`.
 * Really, this class doesn't do any conversion itself, it just provides information for the `StmtConverter`
 * to get its work done.
 */
class MethodConverter(
    private val programCtx: ProgramConversionContext,
    override val signature: FunctionSignature,
    private val paramResolver: ParameterResolver,
    scopeDepth: Int,
    private val parent: MethodConversionContext? = null,
    private val returnPointName: String?, // null while converting the body of an inline function
) : MethodConversionContext, ProgramConversionContext by programCtx {
    private var propertyResolver = PropertyNameResolver(scopeDepth)

    override fun <R> withScopeImpl(scopeDepth: Int, action: () -> R): R {
        propertyResolver = propertyResolver.innerScope(scopeDepth)
        val result = action()
        propertyResolver = propertyResolver.parent!!
        return result
    }

    override fun addLoopIdentifier(labelName: String, index: Int) {
        propertyResolver = propertyResolver.addLoopIdentifier(labelName, index)
    }

    override fun resolveLoopIndex(name: String): Int =
        propertyResolver.tryResolveLoopName(name) ?: throw IllegalArgumentException("Loop $name not found in scope.")

    override fun resolveLocalPropertyName(name: Name): MangledName =
        propertyResolver.tryResolveLocalPropertyName(name) ?: parent?.resolveLocalPropertyName(name)
        ?: throw IllegalArgumentException("Property $name not found in scope.")

    override fun registerLocalPropertyName(name: Name) {
        propertyResolver.registerLocalPropertyName(name)
    }

    override fun embedParameter(symbol: FirValueParameterSymbol): ExpEmbedding =
        paramResolver.tryEmbedParameter(symbol) ?: parent?.embedParameter(symbol)
        ?: throw IllegalArgumentException("Parameter $symbol not found in scope.")

    override val resolvedReturnVarName: MangledName = paramResolver.resolvedReturnVarName
    override val resolvedReturnLabelName: ReturnLabelName = paramResolver.resolvedReturnLabelName
    override fun resolveReturnTarget(sourceName: String?): ReturnTarget {
        return if (returnPointName == null || sourceName == null || returnPointName == sourceName) {
            ReturnTarget(returnVar, returnLabel)
        } else {
            parent?.resolveReturnTarget(sourceName) ?: throw IllegalArgumentException("Cannot resolve returnTarget of $sourceName")
        }
    }
}