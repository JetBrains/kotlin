/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.serialization

import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.serialization.DescriptorSerializer

interface SubpluginContractSerializer {
    fun serializeContextProvider(
        pluginId: String,
        provider: ProviderDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextProvider?

    fun serializeContextVerifier(
        pluginId: String,
        verifier: VerifierDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextVerifier?

    fun serializeContextCleaner(
        pluginId: String,
        cleaner: CleanerDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextCleaner?
}