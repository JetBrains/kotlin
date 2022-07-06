/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test.integration

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class KaptIntegrationEnvironmentConfigurator(
    testServices: TestServices,
    private val processorOptions: Map<String, String>,
    private val supportedAnnotations: List<String>,
    private val process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment, Kapt3ExtensionForTests) -> Unit
) : EnvironmentConfigurator(testServices) {
    override fun legacyRegisterCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        val kaptOptions = testServices.kaptOptionsProvider[module]
        val kapt3Extension = testServices.kapt3ExtensionProvider.createExtension(
            module,
            kaptOptions,
            processorOptions,
            process,
            supportedAnnotations,
            configuration
        )
        AnalysisHandlerExtension.registerExtension(project, kapt3Extension)
    }
}

