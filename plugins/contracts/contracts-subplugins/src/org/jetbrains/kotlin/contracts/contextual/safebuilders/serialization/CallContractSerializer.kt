/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders.serialization

import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.safebuilders.CallCleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.safebuilders.CallFamily
import org.jetbrains.kotlin.contracts.contextual.safebuilders.CallProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.safebuilders.CallVerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractSerializer
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.subplugin.ContractSubpluginsProtoBuf
import org.jetbrains.kotlin.serialization.ContractSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer

class CallContractSerializer : SubpluginContractSerializer {
    override fun serializeContextProvider(
        pluginId: String,
        provider: ProviderDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextProvider? {
        if (pluginId != CallFamily.id || provider !is CallProviderDeclaration) return null
        return ContractsProtoBuf.ContextProvider.newBuilder().apply {
            providerPluginId = CallFamily.id
        }.build()
    }

    override fun serializeContextVerifier(
        pluginId: String,
        verifier: VerifierDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextVerifier? {
        if (pluginId != CallFamily.id || verifier !is CallVerifierDeclaration) return null
        return ContractsProtoBuf.ContextVerifier.newBuilder().apply {
            verifierPluginId = CallFamily.id
            val kind = ContractSerializer.invocationKindProtobufEnum(verifier.kind)
            setExtension(ContractSubpluginsProtoBuf.verifierCallKind, kind)
        }.build()
    }

    override fun serializeContextCleaner(
        pluginId: String,
        cleaner: CleanerDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextCleaner? {
        if (pluginId != CallFamily.id || cleaner !is CallCleanerDeclaration) return null
        return ContractsProtoBuf.ContextCleaner.newBuilder().apply {
            cleanerPluginId = CallFamily.id
            val kind = ContractSerializer.invocationKindProtobufEnum(cleaner.kind)
            setExtension(ContractSubpluginsProtoBuf.cleanerCallKind, kind)
        }.build()
    }
}