/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.fir.FirParcelizeExtensionRegistrar
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class ParcelizeComponentRegistrar : CompilerPluginRegistrar() {
    companion object {
        fun registerParcelizeComponents(
            extensionStorage: ExtensionStorage,
            additionalAnnotation: List<String>,
            useFir: Boolean
        ) = with(extensionStorage) {
            val parcelizeAnnotations = ParcelizeNames.PARCELIZE_CLASS_FQ_NAMES.toMutableList()
            additionalAnnotation.mapTo(parcelizeAnnotations) { FqName(it) }
            if (useFir) {
                IrGenerationExtension.registerExtension(ParcelizeFirIrGeneratorExtension(parcelizeAnnotations))
            } else {
                IrGenerationExtension.registerExtension(ParcelizeIrGeneratorExtension(parcelizeAnnotations))
            }
            SyntheticResolveExtension.registerExtension(ParcelizeResolveExtension(parcelizeAnnotations))
            StorageComponentContainerContributor.registerExtension(ParcelizeDeclarationCheckerComponentContainerContributor(parcelizeAnnotations))
            FirExtensionRegistrarAdapter.registerExtension(FirParcelizeExtensionRegistrar(parcelizeAnnotations))
        }
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val additionalAnnotation = configuration.get(ParcelizeConfigurationKeys.ADDITIONAL_ANNOTATION) ?: emptyList()
        registerParcelizeComponents(this, additionalAnnotation, configuration.getBoolean(CommonConfigurationKeys.USE_FIR))
    }

    override val supportsK2: Boolean
        get() = true
}

class ParcelizeDeclarationCheckerComponentContainerContributor(
    private val parcelizeAnnotations: List<FqName>
) : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        if (platform.isJvm()) {
            container.useInstance(ParcelizeDeclarationChecker(parcelizeAnnotations))
            container.useInstance(ParcelizeAnnotationChecker(parcelizeAnnotations))
        }
    }
}
