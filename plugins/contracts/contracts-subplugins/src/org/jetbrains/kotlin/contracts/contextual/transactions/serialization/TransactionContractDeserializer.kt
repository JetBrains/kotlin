/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions.serialization

import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractDeserializer
import org.jetbrains.kotlin.contracts.contextual.serialization.getExtendedProto
import org.jetbrains.kotlin.contracts.contextual.transactions.ClosedTransactionVerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.transactions.OpenedTransactionVerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.transactions.TransactionCleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.transactions.TransactionProviderDeclaration
import org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.metadata.extension.contracts.subplugin.ContractSubpluginsProtoBuf

class TransactionContractDeserializer : SubpluginContractDeserializer {
    override fun deserializeContextProvider(
        proto: ContractsProtoBuf.ContextProvider,
        worker: ContractDeserializerImpl.ContractDeserializationWorker,
        references: List<ContractDescriptionValue>
    ): ProviderDeclaration? = TransactionProviderDeclaration(references)

    override fun deserializeContextVerifier(
        proto: ContractsProtoBuf.ContextVerifier,
        worker: ContractDeserializerImpl.ContractDeserializationWorker,
        references: List<ContractDescriptionValue>
    ): VerifierDeclaration? {
        val extendedProto = getExtendedProto(proto)
        if (!extendedProto.hasExtension(ContractSubpluginsProtoBuf.transactionVerifierType)) return null
        return when (extendedProto.getExtension(ContractSubpluginsProtoBuf.transactionVerifierType)) {
            1 -> ClosedTransactionVerifierDeclaration(references)
            2 -> OpenedTransactionVerifierDeclaration(references)
            else -> null
        }
    }

    override fun deserializeContextCleaner(
        proto: ContractsProtoBuf.ContextCleaner,
        worker: ContractDeserializerImpl.ContractDeserializationWorker,
        references: List<ContractDescriptionValue>
    ): CleanerDeclaration? = TransactionCleanerDeclaration(references)
}