/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTreeVerifierHandler
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.fir2IrStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationExtensionRegistrar
import java.io.File

abstract class AbstractSerializationFirMembersTest: AbstractKotlinCompilerTest() {
    private val librariesList = listOf(getSerializationCoreLibraryJar()!!, getSerializationLibraryJar("kotlinx.serialization.json.Json")!!)

    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()

        defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
            +FirDiagnosticsDirectives.FIR_DUMP
        }

        configureForKotlinxSerialization(librariesList) {
            FirExtensionRegistrarAdapter.registerExtension(FirSerializationExtensionRegistrar())
        }
    }
}

open class AbstractSerializationFirBlackBoxTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {

    private val librariesList = listOf(getSerializationCoreLibraryJar()!!, getSerializationLibraryJar("kotlinx.serialization.json.Json")!!)

    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        configureForKotlinxSerialization(librariesList) {
            FirExtensionRegistrarAdapter.registerExtension(FirSerializationExtensionRegistrar())
        }
        useCustomRuntimeClasspathProviders({ ts ->
                                               object : RuntimeClasspathProvider(ts) {
                                                   override fun runtimeClassPaths(module: TestModule): List<File> {
                                                       return librariesList
                                                   }
                                               }
                                           })
        defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
        }

        fir2IrStep()
        irHandlersStep {
            useHandlers(
                ::IrTextDumpHandler,
                ::IrTreeVerifierHandler,
            )
        }
        facadeStep(::JvmIrBackendFacade)
        jvmArtifactsHandlersStep {
            useHandlers(::JvmBoxRunner)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}
