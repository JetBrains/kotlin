/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureFirHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest

abstract class AbstractKotlinpTest : AbstractKotlinCompilerTest() {
    protected open val compareWithTxt: Boolean
        get() = true

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            targetBackend = TargetBackend.JVM_IR
        }

        defaultDirectives {
            ModuleStructureDirectives.MODULE with "test-module"
        }

        builder.useDirectives(KotlinpTestDirectives)

        commonConfigurationForJvmTest(FrontendKinds.FIR, ::FirCliJvmFacade, ::Fir2IrCliJvmFacade, ::BackendCliJvmFacade)

        // We need PSI at least until scripts are supported in the LightTree (KT-60127).
        configureFirParser(FirParser.Psi)

        configureFirHandlersStep {
            commonFirHandlersForCodegenTest()
        }

        configureJvmArtifactsHandlersStep {
            useHandlers({ CompareMetadataHandler(it, compareWithTxt = compareWithTxt, verbose = true) })
        }
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}

object KotlinpTestDirectives : SimpleDirectivesContainer() {
    val NO_READ_WRITE_COMPARE by directive("Don't check that metadata after a write-read transformation is equal to the original metadata")
}
