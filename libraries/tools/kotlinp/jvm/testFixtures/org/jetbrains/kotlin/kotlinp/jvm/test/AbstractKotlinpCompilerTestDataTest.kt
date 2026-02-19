/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*

abstract class AbstractKotlinpCompilerTestDataTest : AbstractKotlinpTest<FirOutputArtifact>(FrontendKinds.FIR) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliJvmFacade
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliJvmFacade
    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::BackendCliJvmFacade

    // It would be excessive to dump kotlinp-read declarations for the compiler loadJava test data in addition to the already existing K1
    // and K2 dumps. Instead, we're only checking that the dump is the same after the write-read transformation, just to verify that
    // kotlin-metadata-jvm correctly writes everything that it reads.
    override val compareWithTxt: Boolean
        get() = false

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        with(builder) {
            forTestsMatching("compiler/testData/loadJava/compiledKotlinWithStdlib/*") {
                defaultDirectives {
                    +WITH_STDLIB
                }
            }
        }
    }
}
