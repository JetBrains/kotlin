/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.dataframe.services.DataFrameDirectives
import org.jetbrains.kotlin.fir.dataframe.services.DataFrameDirectives.ENABLE_DATAFRAME
import org.jetbrains.kotlin.fir.dataframe.services.DataFrameEnvironmentConfigurator
import org.jetbrains.kotlin.fir.dataframe.services.TestUtilsSourceProvider
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.EXPLICITLY_GENERATE_PLUGIN_FILES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_DUMP
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.runners.AbstractPhasedJvmDiagnosticLightTreeTest

abstract class AbstractDataFrameDiagnosticTest : AbstractPhasedJvmDiagnosticLightTreeTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +ENABLE_PLUGIN_PHASES
                +FIR_DUMP
                +EXPLICITLY_GENERATE_PLUGIN_FILES
                FIR_PARSER with FirParser.LightTree
                JDK_KIND with TestJdkKind.FULL_JDK
                +ENABLE_DATAFRAME
            }

            useDirectives(DataFrameDirectives)
            useConfigurators(
                ::DataFrameEnvironmentConfigurator
            )
            forTestsNotMatching(
                listOf(
                    "schemaInfo.kt",
                    "structuralCast.kt",
                    "selectDuringTyping.kt",
                    "dataSchemaVisibility.kt",
                    "localDataFrameReturnType.kt",
                ).joinToString("|"),
            ) {
                enableLazyResolvePhaseChecking()
            }
            useAdditionalSourceProviders(::TestUtilsSourceProvider)
        }
    }
}
