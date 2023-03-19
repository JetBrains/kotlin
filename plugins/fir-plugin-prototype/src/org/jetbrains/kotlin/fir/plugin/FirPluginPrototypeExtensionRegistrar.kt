/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.plugin.generators.*
import org.jetbrains.kotlin.fir.plugin.types.FirNumberSignAttributeExtension
import org.jetbrains.kotlin.fir.plugin.types.ComposableLikeFunctionTypeKindExtension
import org.jetbrains.kotlin.ir.plugin.GeneratedDeclarationsIrBodyFiller

class FirPluginPrototypeExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::AllOpenStatusTransformer
        +::AllPublicVisibilityTransformer
        +::SomeAdditionalSupertypeGenerator
        +::SupertypeWithArgumentGenerator
        +::PluginAdditionalCheckers
        +::FirNumberSignAttributeExtension
        +::AlgebraReceiverInjector
        +::ComposableLikeFunctionTypeKindExtension

        // Declaration generators
        +::TopLevelDeclarationsGenerator
        +::ExternalClassGenerator
        +::AdditionalMembersGenerator
        +::CompanionGenerator
        +::MembersOfSerializerGenerator

        +::AllPropertiesConstructorMetadataProvider
    }
}

class FirPluginPrototypeComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(FirPluginPrototypeExtensionRegistrar())
        IrGenerationExtension.registerExtension(GeneratedDeclarationsIrBodyFiller())
    }
}
