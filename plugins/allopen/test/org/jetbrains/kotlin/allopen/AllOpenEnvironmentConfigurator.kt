/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen

import org.jetbrains.kotlin.allopen.fir.FirAllOpenExtensionRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

class AllOpenEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        val annotations = AbstractAllOpenDeclarationAttributeAltererExtension.ANNOTATIONS_FOR_TESTS +
                AllOpenPluginNames.SUPPORTED_PRESETS.flatMap { it.value }

        DeclarationAttributeAltererExtension.registerExtension(CliAllOpenDeclarationAttributeAltererExtension(annotations))
        FirExtensionRegistrarAdapter.registerExtension(FirAllOpenExtensionRegistrar(annotations))
    }
}
