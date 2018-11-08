/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.contracts.contextual.cfg.ContractsControlFlowInformationProvider
import org.jetbrains.kotlin.contracts.contextual.parsing.PsiContextualContractsParserDispather
import org.jetbrains.kotlin.contracts.contextual.resolution.ContextualEffectReducer
import org.jetbrains.kotlin.contracts.contextual.resolution.ContractEffectDeclarationInterpreter
import org.jetbrains.kotlin.contracts.contextual.resolution.resolveContextualContracts
import org.jetbrains.kotlin.contracts.contextual.resolution.substituteContextualEffect
import org.jetbrains.kotlin.contracts.contextual.serialization.deserializeEffect
import org.jetbrains.kotlin.contracts.description.ExtensionEffectDeclaration
import org.jetbrains.kotlin.contracts.interpretation.ContractInterpretationDispatcher
import org.jetbrains.kotlin.contracts.interpretation.EffectDeclarationInterpreter
import org.jetbrains.kotlin.contracts.model.ExtensionEffect
import org.jetbrains.kotlin.contracts.model.functors.ExtensionSubstitutor
import org.jetbrains.kotlin.contracts.model.visitors.ExtensionReducerConstructor
import org.jetbrains.kotlin.contracts.parsing.ContractCallContext
import org.jetbrains.kotlin.contracts.parsing.ContractParsingDiagnosticsCollector
import org.jetbrains.kotlin.contracts.parsing.ExtensionParserDispatcher
import org.jetbrains.kotlin.contracts.parsing.PsiContractParserDispatcher
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.extensions.ContractsExtension
import org.jetbrains.kotlin.extensions.ContractsInfoForInvocation
import org.jetbrains.kotlin.extensions.ExtensionBindingContextData
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class ContractsImplementationExtension : ContractsExtension {
    override val id: String = ContractsCommandLineProcessor.PLUGIN_ID

    override fun getPsiParserDispatcher(
        collector: ContractParsingDiagnosticsCollector,
        callContext: ContractCallContext,
        dispatcher: PsiContractParserDispatcher
    ): ExtensionParserDispatcher =
        PsiContextualContractsParserDispather(collector, callContext, dispatcher)

    override fun getEffectDeclarationInterpreterConstructor(): (ContractInterpretationDispatcher) -> EffectDeclarationInterpreter =
        ::ContractEffectDeclarationInterpreter

    override fun getExtensionReducerConstructor(): ExtensionReducerConstructor = ::ContextualEffectReducer

    override fun getExtensionSubstitutor(): ExtensionSubstitutor = ::substituteContextualEffect

    override fun emptyBindingContextData(): ExtensionBindingContextData = FactsBindingInfo.EMPTY

    override fun collectDefiniteInvocations(
        effect: ExtensionEffect,
        resolvedCall: ResolvedCall<*>,
        bindingContext: BindingContext
    ): ContractsInfoForInvocation? =
        resolveContextualContracts(effect, resolvedCall, bindingContext)

    override fun analyzeFunction(
        function: KtFunction,
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        diagnosticSink: DiagnosticSink
    ) {
        val provider = ContractsControlFlowInformationProvider(
            function,
            pseudocode,
            bindingContext,
            diagnosticSink
        )
        provider.analyze()
    }

    override fun deserializeExtensionEffect(
        proto: ProtoBuf.Effect,
        project: Project,
        functionDescriptor: FunctionDescriptor,
        contractDeserializationWorker: ContractDeserializerImpl.ContractDeserializationWorker
    ): ExtensionEffectDeclaration? = deserializeEffect(proto, project, functionDescriptor, contractDeserializationWorker)
}

