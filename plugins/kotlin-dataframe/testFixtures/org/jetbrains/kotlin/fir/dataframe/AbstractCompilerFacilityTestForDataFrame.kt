/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory.createConfigurator
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractCompilerFacilityTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*
import org.jetbrains.kotlin.fir.dataframe.services.DataFrameClasspathProvider
import org.jetbrains.kotlin.fir.dataframe.services.DataFramePluginAnnotationsProvider
import org.jetbrains.kotlin.fir.dataframe.services.ExperimentalExtensionRegistrarConfigurator
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractCompilerFacilityTestForDataFrame : AbstractCompilerFacilityTest() {

    override val configurator: AnalysisApiTestConfigurator = createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(
            FrontendKind.Fir,
            TestModuleKind.Source,
            AnalysisSessionMode.Normal,
            AnalysisApiMode.Ide
        )
    )

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useConfigurators(::ExperimentalExtensionRegistrarConfigurator)
        builder.useCustomRuntimeClasspathProviders(::DataFrameClasspathProvider)
        builder.useConfigurators(::DataFramePluginAnnotationsProvider)
    }
}