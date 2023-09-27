/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature

class MethodContextFactory(
    private val signature: FunctionSignature,
    private val paramResolver: ParameterResolver,
    private val parent: MethodConversionContext? = null,
    private val returnPointName: String?
) {
    fun create(
        programCtx: ProgramConversionContext,
        scopeDepth: Int,
    ): MethodConversionContext = MethodConverter(programCtx, signature, paramResolver, scopeDepth, parent, returnPointName)
}