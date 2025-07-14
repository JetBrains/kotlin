/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.dataframe.services.DataFrameClasspathProvider
import org.jetbrains.kotlin.fir.dataframe.services.DataFramePluginAnnotationsProvider
import org.jetbrains.kotlin.fir.dataframe.services.ExperimentalExtensionRegistrarConfigurator
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

open class AbstractDataFrameBlackBoxCodegenTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            JvmEnvironmentConfigurationDirectives.JDK_KIND with TestJdkKind.FULL_JDK
            +JvmEnvironmentConfigurationDirectives.WITH_REFLECT
        }
        builder.forTestsMatching("*/csDsl/*") {
            builder.useAdditionalSourceProviders(::SelectionDslUtilsSourceProvider)
        }
        builder.enableLazyResolvePhaseChecking()
        builder.useConfigurators(::DataFramePluginAnnotationsProvider)
        builder.useConfigurators(::ExperimentalExtensionRegistrarConfigurator)
        builder.useCustomRuntimeClasspathProviders(::DataFrameClasspathProvider)
        builder.useAdditionalSourceProviders(::TestUtilsSourceProvider)
    }

    class TestUtilsSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
        companion object {
            const val COMMON_SOURCE_PATH = "plugins/kotlin-dataframe/testData/testUtils.kt"
        }

        override fun produceAdditionalFiles(
            globalDirectives: RegisteredDirectives,
            module: TestModule,
            testModuleStructure: TestModuleStructure,
        ): List<TestFile> {
            return listOf(File(COMMON_SOURCE_PATH).toTestFile())
        }
    }

    class SelectionDslUtilsSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
        companion object {
            const val SELECTION_DSL_UTILS = "plugins/kotlin-dataframe/testData/selectionDslTestUtils.kt"
        }

        override fun produceAdditionalFiles(
            globalDirectives: RegisteredDirectives,
            module: TestModule,
            testModuleStructure: TestModuleStructure,
        ): List<TestFile> {
            return listOf(File(SELECTION_DSL_UTILS).toTestFile())
        }
    }
}

