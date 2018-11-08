/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.contracts.contextual.extensions.SpecificContractExtension
import org.jetbrains.kotlin.contracts.contextual.model.ContextFamily
import org.jetbrains.kotlin.contracts.contextual.parsing.PsiEffectDeclarationExtractor
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractDeserializer
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractSerializer
import org.jetbrains.kotlin.contracts.parsing.PsiContractVariableParserDispatcher
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext

object ContextualEffectSystem {
    private val cache = mutableMapOf<Project, Collection<SpecificContractExtension>>()
    private fun getExtensionInstances(project: Project): Collection<SpecificContractExtension> {
        val cachedInstances = cache[project]
        if (cachedInstances != null) return cachedInstances

        val instances = SpecificContractExtension.getInstances(project)
        cache[project] = instances
        return instances
    }


    fun getFamilies(project: Project): Collection<ContextFamily> = getExtensionInstances(project).map { it.getFamily() }

    fun getParsers(
        project: Project,
        bindingContext: BindingContext,
        dispatcher: PsiContractVariableParserDispatcher
    ): Collection<PsiEffectDeclarationExtractor> =
        getExtensionInstances(project).map { it.getParser(bindingContext, dispatcher) }

    fun getSerializers(project: Project): Collection<SubpluginContractSerializer> =
        getExtensionInstances(project).map { it.subpluginContractSerializer }

    fun getDeserializer(project: Project, subpluginId: String): SubpluginContractDeserializer? =
        getExtensionInstances(project).filter { it.getFamily().id == subpluginId }.map { it.subpluginContractDeserializer }.firstOrNull()
}

fun KtExpression.declaredFactsInfo(bindingContext: BindingContext): FactsBindingInfo =
    bindingContext[BindingContext.EXTENSION_SLICE, this]
        ?.get(ContractsCommandLineProcessor.PLUGIN_ID) as? FactsBindingInfo
        ?: FactsBindingInfo.EMPTY