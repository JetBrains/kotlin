/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.exceptions.serialization

import org.jetbrains.kotlin.contracts.contextual.exceptions.ExceptionFamily
import org.jetbrains.kotlin.contracts.contextual.exceptions.ExceptionProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.exceptions.ExceptionVerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractSerializer
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.subplugin.ContractSubpluginsProtoBuf
import org.jetbrains.kotlin.serialization.DescriptorSerializer

class ExceptionContractSerializer : SubpluginContractSerializer {
    override fun serializeContextProvider(
        pluginId: String,
        provider: ProviderDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextProvider? {
        if (pluginId != ExceptionFamily.id || provider !is ExceptionProviderDeclaration) return null
        return ContractsProtoBuf.ContextProvider.newBuilder().apply {
            providerPluginId = ExceptionFamily.id
            val exceptionType = descriptorSerializer.type(provider.exceptionType).build()
            setExtension(ContractSubpluginsProtoBuf.providerExceptionType, exceptionType)
        }.build()
    }

    override fun serializeContextVerifier(
        pluginId: String,
        verifier: VerifierDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextVerifier? {
        if (pluginId != ExceptionFamily.id || verifier !is ExceptionVerifierDeclaration) return null
        return ContractsProtoBuf.ContextVerifier.newBuilder().apply {
            verifierPluginId = ExceptionFamily.id
            val exceptionType = descriptorSerializer.type(verifier.exceptionType).build()
            setExtension(ContractSubpluginsProtoBuf.verifierExceptionType, exceptionType)
        }.build()
    }

    override fun serializeContextCleaner(
        pluginId: String,
        cleaner: CleanerDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextCleaner? {
        return null
    }
}