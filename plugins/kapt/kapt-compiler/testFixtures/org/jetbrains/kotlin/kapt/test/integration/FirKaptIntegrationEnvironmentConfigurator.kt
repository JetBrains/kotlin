/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test.integration

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.skipBodies
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt.test.kaptOptionsProvider
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceFileProvider
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class FirKaptIntegrationEnvironmentConfigurator(
    testServices: TestServices,
    private val processorOptions: Map<String, String>,
    private val supportedAnnotations: List<String>,
    private val process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment, FirKaptExtensionForTests) -> Unit
) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        val kaptOptions = testServices.kaptOptionsProvider[module]
        val firKaptExtension = testServices.firKaptExtensionProvider.createExtension(
            module,
            kaptOptions,
            processorOptions,
            process,
            supportedAnnotations,
            module.files.map(testServices.sourceFileProvider::getOrCreateRealFileForSourceFile),
        )
        FirAnalysisHandlerExtension.registerExtension(firKaptExtension)

        // `FirKaptAnalysisHandlerExtension` checks the `skipBodies` flag to detect if this is the analysis that is run from within the
        // extension itself, to prevent endless recursion. If the configuration contains this flag from the beginning, the extension is not
        // applied at all.
        configuration.skipBodies = false
    }
}
