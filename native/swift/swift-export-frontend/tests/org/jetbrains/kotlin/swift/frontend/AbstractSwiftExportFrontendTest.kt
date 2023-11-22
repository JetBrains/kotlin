/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swift.frontend

import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.analysisapi.SirGenerator
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSwiftExportFrontendTest : AbstractAnalysisApiBasedTest() {

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        with(builder) {
            globalDefaults {
                targetPlatform = NativePlatforms.unspecifiedNativePlatform
            }
        }
    }

    override val configurator: AnalysisApiTestConfigurator
        get() = AnalysisApiFirTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                FrontendKind.Fir,
                TestModuleKind.Source,
                AnalysisSessionMode.Normal,
                AnalysisApiMode.Ide
            )
        );

    override fun doTestByModuleStructure(moduleStructure: TestModuleStructure, testServices: TestServices) {
        val ktFiles = moduleStructure.modules
            .flatMap { testServices.ktModuleProvider.getModuleFiles(it).filterIsInstance<KtFile>() }

        val generator = SirGenerator()
        val frontend = SwiftExportFrontend(generator)
        val result = frontend.run(ktFiles)

        testServices.assertions.assertEqualsToTestDataFileSibling(result.swiftSource.joinToString("\n"), extension = ".swift")
        testServices.assertions.assertEqualsToTestDataFileSibling(result.header.joinToString("\n"), extension = ".h")
        testServices.assertions.assertEqualsToTestDataFileSibling(result.kotlinSource.joinToString("\n"), extension = ".bridge")
    }
}