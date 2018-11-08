/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.serialization

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.contracts.contextual.ContextualEffectSystem
import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.resolution.*
import org.jetbrains.kotlin.contracts.description.ContractDescription
import org.jetbrains.kotlin.contracts.description.ExtensionEffectDeclaration
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.serialization.ContractSerializer
import org.jetbrains.kotlin.serialization.ContractSerializerExtension
import org.jetbrains.kotlin.serialization.DescriptorSerializer

class ExtensionContractSerializer : ContractSerializerExtension {
    override fun serializeExtensionEffect(
        builder: ProtoBuf.Effect.Builder,
        effectDeclaration: ExtensionEffectDeclaration,
        contractDescription: ContractDescription,
        project: Project,
        contractSerializerWorker: ContractSerializer.ContractSerializerWorker,
        descriptorSerializer: DescriptorSerializer
    ): Boolean {
        val declaration = effectDeclaration as? ContextualEffectDeclaration<*, *, *> ?: return false

        val subpluginId: String = declaration.factory.family.id
        val effectType = when (declaration) {
            is ContextProviderEffectDeclaration -> ContractsProtoBuf.ContextualEffectType.CONTEXT_PROVIDER
            is ContextVerifierEffectDeclaration -> ContractsProtoBuf.ContextualEffectType.CONTEXT_VERIFIER
            is ContextCleanerEffectDeclaration -> ContractsProtoBuf.ContextualEffectType.CONTEXT_CLEANER
            is LambdaContextProviderEffectDeclaration -> ContractsProtoBuf.ContextualEffectType.LAMBDA_CONTEXT_PROVIDER
            is LambdaContextVerifierEffectDeclaration -> ContractsProtoBuf.ContextualEffectType.LAMBDA_CONTEXT_VERIFIER
            is LambdaContextCleanerEffectDeclaration -> ContractsProtoBuf.ContextualEffectType.LAMBDA_CONTEXT_CLEANER
            else -> return false
        }

        val contextEntityDeclaration = declaration.factory
        val subpluginSerializers = ContextualEffectSystem.getSerializers(project)

        when (contextEntityDeclaration) {
            is ProviderDeclaration -> {
                val serializedEntity = subpluginSerializers.asSequence()
                    .mapNotNull { it.serializeContextProvider(subpluginId, contextEntityDeclaration, descriptorSerializer) }
                    .firstOrNull() ?: return false
                builder.setExtension(ContractsProtoBuf.provider, serializedEntity)
            }
            is VerifierDeclaration -> {
                val serializedEntity = subpluginSerializers.asSequence()
                    .mapNotNull { it.serializeContextVerifier(subpluginId, contextEntityDeclaration, descriptorSerializer) }
                    .firstOrNull() ?: return false
                builder.setExtension(ContractsProtoBuf.verifier, serializedEntity)
            }
            is CleanerDeclaration -> {
                val serializedEntity = subpluginSerializers.asSequence()
                    .mapNotNull { it.serializeContextCleaner(subpluginId, contextEntityDeclaration, descriptorSerializer) }
                    .firstOrNull() ?: return false
                builder.setExtension(ContractsProtoBuf.cleaner, serializedEntity)
            }
        }

        builder.setExtension(ContractsProtoBuf.contextualEffectType, effectType)
        builder.setExtension(ContractsProtoBuf.subpluginId, subpluginId)

        val owner = contractSerializerWorker.contractExpressionProto(declaration.owner, contractDescription).build()
        builder.setExtension(ContractsProtoBuf.owner, owner)

        val references = declaration.references.map { contractSerializerWorker.contractExpressionProto(it, contractDescription).build() }
        builder.setExtension(ContractsProtoBuf.references, references)

        return true
    }
}