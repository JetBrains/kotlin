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
import org.jetbrains.kotlin.parcelize.fir.FirParcelizeExtensionRegistrar
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class ParcelizeComponentRegistrar : CompilerPluginRegistrar() {
    companion object {
        fun registerParcelizeComponents(extensionStorage: ExtensionStorage, useFir: Boolean) = with(extensionStorage) {
            if (useFir) {
                IrGenerationExtension.registerExtension(ParcelizeFirIrGeneratorExtension())
            } else {
                IrGenerationExtension.registerExtension(ParcelizeIrGeneratorExtension())
            }
            SyntheticResolveExtension.registerExtension(ParcelizeResolveExtension())
            StorageComponentContainerContributor.registerExtension(ParcelizeDeclarationCheckerComponentContainerContributor())
            FirExtensionRegistrarAdapter.registerExtension(FirParcelizeExtensionRegistrar())
        }
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        registerParcelizeComponents(this, configuration.getBoolean(CommonConfigurationKeys.USE_FIR))
    }

    override val supportsK2: Boolean
        get() = true
}

class ParcelizeDeclarationCheckerComponentContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        if (platform.isJvm()) {
            container.useInstance(ParcelizeDeclarationChecker())
            container.useInstance(ParcelizeAnnotationChecker())
        }
    }
}
