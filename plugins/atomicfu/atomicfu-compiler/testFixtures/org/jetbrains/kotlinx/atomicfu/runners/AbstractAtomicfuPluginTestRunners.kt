/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.runners

import org.jetbrains.kotlin.konan.test.irText.AbstractLightTreeNativeIrTextTest
import org.jetbrains.kotlin.test.backend.handlers.SMAPDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.frontend.fir.FirFailingTestSuppressor
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractFirPsiDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import java.io.File

open class AbstractAtomicfuJvmIrTest : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJvmArtifactsHandlersStep { useHandlers(::SMAPDumpHandler) }
        builder.configureForKotlinxAtomicfu()
    }
}

open class AbstractAtomicfuJvmFirLightTreeTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureJvmArtifactsHandlersStep { useHandlers(::SMAPDumpHandler) }
        builder.configureForKotlinxAtomicfu()
    }
}

abstract class AbstractAtomicfuFirCheckerTest : AbstractFirPsiDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureForKotlinxAtomicfu()
            useAfterAnalysisCheckers(::FirFailingTestSuppressor)
        }
    }
}

// TODO temporarily disabled generation of FIR dumping tests, see: KT-79199
open class AbstractAtomicfuNativeIrTextTest : AbstractLightTreeNativeIrTextTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(
                ::AtomicfuExtensionRegistrarConfigurator,
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
            )
            defaultDirectives {
                +WITH_PLATFORM_LIBS
            }

            useCustomRuntimeClasspathProviders(
                {
                    object : RuntimeClasspathProvider(it) {
                        override fun runtimeClassPaths(module: TestModule): List<File> {
                            val str = System.getProperty("atomicfuNative.classpath")?.split(File.pathSeparator) ?: emptyList()
                            val compilerPluginClasspath = System.getProperty("atomicfu.compiler.plugin")?.split(File.pathSeparator) ?: emptyList()
                            return (str + compilerPluginClasspath).map { File(it) }
                        }
                    }
                }
            )
        }
    }
}
