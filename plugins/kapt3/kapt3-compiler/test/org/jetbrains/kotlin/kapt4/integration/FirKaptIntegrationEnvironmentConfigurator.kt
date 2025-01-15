/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4.integration

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.getKtFilesForSourceFiles
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
    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        val kaptOptions = testServices.kaptOptionsProvider[module]
        val firKaptExtension = testServices.firKaptExtensionProvider.createExtension(
            module,
            kaptOptions,
            processorOptions,
            process,
            supportedAnnotations,
            testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project, findViaVfs = true).values.toList()
        )
        FirAnalysisHandlerExtension.registerExtension(project, firKaptExtension)
    }
}

