/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.serialization

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.contracts.contextual.ContextualEffectSystem
import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.resolution.*
import org.jetbrains.kotlin.contracts.description.ExtensionEffectDeclaration
import org.jetbrains.kotlin.contracts.description.expressions.FunctionReference
import org.jetbrains.kotlin.contracts.description.expressions.VariableReference
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf.ContextualEffectType.*
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite

fun deserializeEffect(
    rawProto: ProtoBuf.Effect,
    project: Project,
    functionDescriptor: FunctionDescriptor,
    deserializationWorker: ContractDeserializerImpl.ContractDeserializationWorker
): ExtensionEffectDeclaration? {
    val proto = ProtoBuf.Effect.parseFrom(rawProto.toByteArray(), extensionRegistry)
    if (!validProto(proto)) return null

    val subpluginDeserializer = ContextualEffectSystem.getDeserializer(project, proto.subpluginId) ?: return null
    val references = proto.references.map { deserializationWorker.extractExpression(it) ?: return null }

    fun extractProvider(): ProviderDeclaration? =
        subpluginDeserializer.deserializeContextProvider(proto.provider, deserializationWorker, references)

    fun extractVerifier(): VerifierDeclaration? =
        subpluginDeserializer.deserializeContextVerifier(proto.verifier, deserializationWorker, references)

    fun extractCleaner(): CleanerDeclaration? =
        subpluginDeserializer.deserializeContextCleaner(proto.cleaner, deserializationWorker, references)

    fun extractFunctionOwner(): FunctionReference? = deserializationWorker.extractFunction(proto.owner)

    fun extractVariableOwner(): VariableReference? = deserializationWorker.extractVariable(proto.owner)

    val effectType = proto.contextualEffectType
    return when (effectType) {
        CONTEXT_PROVIDER -> ContextProviderEffectDeclaration(
            extractProvider() ?: return null,
            references,
            extractFunctionOwner() ?: return null
        )
        CONTEXT_VERIFIER -> ContextVerifierEffectDeclaration(
            extractVerifier() ?: return null,
            references,
            extractFunctionOwner() ?: return null
        )
        CONTEXT_CLEANER -> ContextCleanerEffectDeclaration(
            extractCleaner() ?: return null,
            references,
            extractFunctionOwner() ?: return null
        )
        LAMBDA_CONTEXT_PROVIDER -> LambdaContextProviderEffectDeclaration(
            extractProvider() ?: return null,
            references,
            extractVariableOwner() ?: return null
        )
        LAMBDA_CONTEXT_VERIFIER -> LambdaContextVerifierEffectDeclaration(
            extractVerifier() ?: return null,
            references,
            extractVariableOwner() ?: return null
        )

        LAMBDA_CONTEXT_CLEANER -> LambdaContextCleanerEffectDeclaration(
            extractCleaner() ?: return null,
            references,
            extractVariableOwner() ?: return null
        )

        else -> null
    }
}


private fun ProtoBuf.Effect.hasContextualEffectType(): Boolean = hasExtension(ContractsProtoBuf.contextualEffectType)
private fun ProtoBuf.Effect.hasSubpluginId(): Boolean = hasExtension(ContractsProtoBuf.subpluginId)
private fun ProtoBuf.Effect.hasOwner(): Boolean = hasExtension(ContractsProtoBuf.owner)
private fun ProtoBuf.Effect.hasProvider(): Boolean = hasExtension(ContractsProtoBuf.provider)
private fun ProtoBuf.Effect.hasVerifier(): Boolean = hasExtension(ContractsProtoBuf.verifier)
private fun ProtoBuf.Effect.hasCleaner(): Boolean = hasExtension(ContractsProtoBuf.cleaner)

val ProtoBuf.Effect.contextualEffectType: ContractsProtoBuf.ContextualEffectType
    get() = getExtension(ContractsProtoBuf.contextualEffectType)
val ProtoBuf.Effect.subpluginId: String
    get() = getExtension(ContractsProtoBuf.subpluginId)
val ProtoBuf.Effect.owner: ProtoBuf.Expression
    get() = getExtension(ContractsProtoBuf.owner)
val ProtoBuf.Effect.references: List<ProtoBuf.Expression>
    get() = getExtension(ContractsProtoBuf.references)
val ProtoBuf.Effect.provider: ContractsProtoBuf.ContextProvider
    get() = getExtension(ContractsProtoBuf.provider)
val ProtoBuf.Effect.verifier: ContractsProtoBuf.ContextVerifier
    get() = getExtension(ContractsProtoBuf.verifier)
val ProtoBuf.Effect.cleaner: ContractsProtoBuf.ContextCleaner
    get() = getExtension(ContractsProtoBuf.cleaner)

private fun validProto(proto: ProtoBuf.Effect): Boolean = with(proto) {
    hasContextualEffectType() && hasSubpluginId() && hasOwner() &&
            (hasProvider() || hasVerifier() || hasCleaner())
}

private val extensionRegistry = ExtensionRegistryLite.newInstance().apply {
    ContractsProtoBuf.registerAllExtensions(this)
}