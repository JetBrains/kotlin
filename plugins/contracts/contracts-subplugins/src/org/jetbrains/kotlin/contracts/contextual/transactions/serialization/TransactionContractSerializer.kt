/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions.serialization

import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractSerializer
import org.jetbrains.kotlin.contracts.contextual.transactions.*
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.subplugin.ContractSubpluginsProtoBuf
import org.jetbrains.kotlin.serialization.DescriptorSerializer

class TransactionContractSerializer : SubpluginContractSerializer {
    override fun serializeContextProvider(
        pluginId: String,
        provider: ProviderDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextProvider? {
        if (pluginId != TransactionFamily.id || provider !is TransactionProviderDeclaration) return null
        return ContractsProtoBuf.ContextProvider.newBuilder().apply {
            providerPluginId = TransactionFamily.id
        }.build()
    }

    override fun serializeContextVerifier(
        pluginId: String,
        verifier: VerifierDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextVerifier? {
        if (pluginId != TransactionFamily.id || verifier !is TransactionVerifierDeclaration) return null
        return ContractsProtoBuf.ContextVerifier.newBuilder().apply {
            verifierPluginId = TransactionFamily.id
            val verifierType = when (verifier) {
                is ClosedTransactionVerifierDeclaration -> 1
                is OpenedTransactionVerifierDeclaration -> 2
            }
            setExtension(ContractSubpluginsProtoBuf.transactionVerifierType, verifierType)
        }.build()
    }

    override fun serializeContextCleaner(
        pluginId: String,
        cleaner: CleanerDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextCleaner? {
        if (pluginId != TransactionFamily.id || cleaner !is TransactionCleanerDeclaration) return null
        return ContractsProtoBuf.ContextCleaner.newBuilder().apply {
            cleanerPluginId = TransactionFamily.id
        }.build()
    }
}