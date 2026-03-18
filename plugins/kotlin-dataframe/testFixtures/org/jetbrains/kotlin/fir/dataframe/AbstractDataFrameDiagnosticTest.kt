/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.dataframe.services.DataFrameDirectives
import org.jetbrains.kotlin.fir.dataframe.services.DataFrameEnvironmentConfigurator
import org.jetbrains.kotlin.fir.dataframe.services.TestUtilsSourceProvider
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest

abstract class AbstractDataFrameDiagnosticTest : AbstractKotlinCompilerTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        builder.baseFirDiagnosticTestConfiguration()
        builder.defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
            +FirDiagnosticsDirectives.FIR_DUMP
            +FirDiagnosticsDirectives.EXPLICITLY_GENERATE_PLUGIN_FILES
            FirDiagnosticsDirectives.FIR_PARSER with FirParser.LightTree
            JvmEnvironmentConfigurationDirectives.JDK_KIND with TestJdkKind.FULL_JDK
        }

        builder.useDirectives(DataFrameDirectives)
        builder.useConfigurators(
            ::DataFrameEnvironmentConfigurator
        )
        builder.forTestsNotMatching("schemaInfo.kt|structuralCast.kt|selectDuringTyping.kt|dataSchemaVisibility.kt|localDataFrameReturnType.kt") {
            enableLazyResolvePhaseChecking()
        }
        builder.useAdditionalSourceProviders(::TestUtilsSourceProvider)
    }
}
