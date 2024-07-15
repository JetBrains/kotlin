/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.callables.FunctionSignature
import org.jetbrains.kotlin.formver.embeddings.expression.ExpEmbedding
import org.jetbrains.kotlin.formver.names.embedParameterName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/**
 * Name resolver for parameters and return values and labels.
 *
 * Since parameter names may map to lambda embeddings, we use `embed` for those for consistency.
 */
interface ParameterResolver {
    fun tryResolveParameter(name: Name): ExpEmbedding?
    fun tryResolveReceiver(): ExpEmbedding?

    val sourceName: String?
    val defaultResolvedReturnTarget: ReturnTarget
}

fun ParameterResolver.resolveNamedReturnTarget(returnPointName: String): ReturnTarget? =
    (returnPointName == sourceName).ifTrue { defaultResolvedReturnTarget }

class RootParameterResolver(
    val ctx: ProgramConversionContext,
    signature: FunctionSignature,
    override val sourceName: String?,
    override val defaultResolvedReturnTarget: ReturnTarget,
) : ParameterResolver {
    private val parameters = signature.params.associateBy { it.name }
    private val receiver = signature.receiver
    override fun tryResolveParameter(name: Name): ExpEmbedding? = parameters[name.embedParameterName()]
    override fun tryResolveReceiver() = receiver
}

class InlineParameterResolver(
    private val substitutions: Map<Name, ExpEmbedding>,
    override val sourceName: String?,
    override val defaultResolvedReturnTarget: ReturnTarget,
) : ParameterResolver {
    override fun tryResolveParameter(name: Name): ExpEmbedding? = substitutions[name]
    override fun tryResolveReceiver(): ExpEmbedding? = substitutions[SpecialNames.THIS]
}