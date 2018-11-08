/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.dslmarker.serialization

import org.jetbrains.kotlin.contracts.contextual.dslmarker.DslMarkerFamily
import org.jetbrains.kotlin.contracts.contextual.dslmarker.DslMarkerProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.dslmarker.DslMarkerVerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.contextual.serialization.SubpluginContractSerializer
import org.jetbrains.kotlin.metadata.extension.contracts.ContractsProtoBuf
import org.jetbrains.kotlin.serialization.DescriptorSerializer

class DslMarkerSerializer : SubpluginContractSerializer {
    override fun serializeContextProvider(
        pluginId: String,
        provider: ProviderDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextProvider? {
        if (pluginId != DslMarkerFamily.id || provider !is DslMarkerProviderDeclaration) return null
        return ContractsProtoBuf.ContextProvider.newBuilder().apply {
            providerPluginId = DslMarkerFamily.id
        }.build()
    }

    override fun serializeContextVerifier(
        pluginId: String,
        verifier: VerifierDeclaration,
        descriptorSerializer: DescriptorSerializer
    ): ContractsProtoBuf.ContextVerifier? {
        if (pluginId != DslMarkerFamily.id || verifier !is DslMarkerVerifierDeclaration) return null
        return ContractsProtoBuf.ContextVerifier.newBuilder().apply {
            verifierPluginId = DslMarkerFamily.id
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