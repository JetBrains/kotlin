/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi

import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.allKtFiles
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind
import org.jetbrains.kotlin.sir.SirForeignFunction
import org.jetbrains.kotlin.sir.SirKotlinOrigin
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

open class AbstractKotlinSirContextTest : AbstractKotlinSirContextTestBase() {
    override val configurator: AnalysisApiTestConfigurator
        get() = AnalysisApiFirTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                FrontendKind.Fir,
                TestModuleKind.Source,
                AnalysisSessionMode.Normal,
                AnalysisApiMode.Ide
            )
        );
}

abstract class AbstractKotlinSirContextTestBase : AbstractAnalysisApiBasedTest() {
    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val ktFiles = testServices.allKtFiles()
        val module = buildModule {
            val sirFactory = SirGenerator()
            ktFiles.forEach { file ->
                name = "Test"
                declarations += sirFactory.build(file)
            }
        }

        val actual = buildString {
            module.declarations
                .filterIsInstance<SirForeignFunction>()
                .forEach {
                    val function = it.origin as SirKotlinOrigin.Function
                    appendLine("${function.fqName}")
                }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".sir")
    }
}
