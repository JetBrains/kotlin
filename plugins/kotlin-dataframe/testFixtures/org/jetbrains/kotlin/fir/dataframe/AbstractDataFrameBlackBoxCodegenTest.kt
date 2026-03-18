/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.fir.dataframe.services.DataFrameRuntimeClasspathProvider
import org.jetbrains.kotlin.fir.dataframe.services.DataFrameDirectives
import org.jetbrains.kotlin.fir.dataframe.services.DataFrameEnvironmentConfigurator
import org.jetbrains.kotlin.fir.dataframe.services.TestUtilsSourceProvider
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.configureModernJavaTest
import org.jetbrains.kotlin.test.configuration.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

open class AbstractDataFrameBlackBoxCodegenTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.defaultDirectives {
            JvmEnvironmentConfigurationDirectives.JDK_KIND with TestJdkKind.FULL_JDK
            +JvmEnvironmentConfigurationDirectives.WITH_REFLECT
        }
        builder.forTestsMatching("*/toDataFrame_javarecord.kt") {
            defaultDirectives {
                -JvmEnvironmentConfigurationDirectives.JDK_KIND
            }
            configureModernJavaTest(TestJdkKind.FULL_JDK_17, JvmTarget.JVM_17)
        }
        builder.forTestsMatching("*/csDsl/*") {
            builder.useAdditionalSourceProviders(::SelectionDslUtilsSourceProvider)
        }
        builder.enableLazyResolvePhaseChecking()
        builder.useDirectives(DataFrameDirectives)
        builder.useConfigurators(::DataFrameEnvironmentConfigurator)
        builder.useCustomRuntimeClasspathProviders(::DataFrameRuntimeClasspathProvider)
        builder.useAdditionalSourceProviders(::TestUtilsSourceProvider)
    }

    class SelectionDslUtilsSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
        companion object {
            const val SELECTION_DSL_UTILS = "selectionDslTestUtils.kt"
        }

        override fun produceAdditionalFiles(
            globalDirectives: RegisteredDirectives,
            module: TestModule,
            testModuleStructure: TestModuleStructure,
        ): List<TestFile> {
            val classLoader = this::class.java.classLoader
            return listOf(classLoader.getResource(SELECTION_DSL_UTILS)!!.toTestFile())
        }
    }
}

