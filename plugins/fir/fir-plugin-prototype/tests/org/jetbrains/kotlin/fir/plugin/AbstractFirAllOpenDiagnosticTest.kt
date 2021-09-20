/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File

abstract class AbstractFirAllOpenDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration(frontendFacade = facade)
        defaultDirectives {
            +ENABLE_PLUGIN_PHASES
            +FIR_DUMP
        }

        useConfigurators(::PluginAnnotationsProvider)
    }

    private val facade: Constructor<FirFrontendFacade>
        get() = { testServices ->
            FirFrontendFacade(testServices) {
                it.registerExtensions(FirAllOpenComponentRegistrar().configure())
            }
        }

    class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
        companion object {
            const val ANNOTATIONS_JAR =
                "plugins/fir/fir-plugin-prototype/plugin-annotations/build/libs/plugin-annotations-1.6.255-SNAPSHOT.jar"
        }

        override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
            val jar = File(ANNOTATIONS_JAR)
            testServices.assertions.assertTrue(jar.exists()) { "Jar with annotations does not exist. Please run :plugins:fir:fir-plugin-prototype:plugin-annotations:jar" }
            configuration.addJvmClasspathRoot(jar)
        }
    }
}
