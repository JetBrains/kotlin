/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlinx.serialization.compiler.fir.FirSerializationExtensionRegistrar

abstract class AbstractSerializationFirMembersTest: AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()

        defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
            +FirDiagnosticsDirectives.FIR_DUMP
        }

        configureForKotlinxSerialization(listOf(getSerializationCoreLibraryJar()!!)) { project ->
            FirExtensionRegistrar.registerExtension(project, FirSerializationExtensionRegistrar())
        }
    }
}