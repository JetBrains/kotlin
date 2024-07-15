/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.VariableEmbedding
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
) : MethodConversionContext, ProgramConversionContext by programCtx {
    private var propertyResolver = PropertyResolver(scopeDepth)

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

    override fun resolveLocal(name: Name): VariableEmbedding =
        propertyResolver.tryResolveLocalProperty(name) ?: parent?.resolveLocal(name)
        ?: throw IllegalArgumentException("Property $name not found in scope.")

    override fun registerLocalProperty(symbol: FirPropertySymbol) {
        propertyResolver.registerLocalProperty(symbol, embedType(symbol.resolvedReturnType))
    }

    override fun registerLocalVariable(symbol: FirVariableSymbol<*>) {
        propertyResolver.registerLocalVariable(symbol, embedType(symbol.resolvedReturnType))
    }

    override fun resolveParameter(name: Name): ExpEmbedding =
        paramResolver.tryResolveParameter(name) ?: parent?.resolveParameter(name)
        ?: throw IllegalArgumentException("Parameter $name not found in scope.")

    override fun resolveReceiver(): ExpEmbedding? = paramResolver.tryResolveReceiver() ?: parent?.resolveReceiver()

    override val defaultResolvedReturnTarget = paramResolver.defaultResolvedReturnTarget
    override fun resolveNamedReturnTarget(sourceName: String): ReturnTarget? {
        return paramResolver.resolveNamedReturnTarget(sourceName) ?: parent?.resolveNamedReturnTarget(sourceName)
    }
}