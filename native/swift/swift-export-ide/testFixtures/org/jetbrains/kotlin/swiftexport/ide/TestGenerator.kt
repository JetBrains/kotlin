/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.ide

import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.tests.analysis.api.dsl.FrontendConfiguratorTestModel

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "native/swift/swift-export-ide/testData") {
            testClass<AbstractSymbolToSirTest>(
                suiteTestClassName = "SwiftExportInIdeTestGenerated",
            ) {
                model(recursive = false)
                val data = AnalysisApiTestConfiguratorFactoryData()
                method(FrontendConfiguratorTestModel(AnalysisApiFirTestConfiguratorFactory::class, data))
            }
        }
    }
}
