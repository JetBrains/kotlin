/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.test.analysisApi

import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.expressionTypeProvider.AbstractExpressionTypeTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.assignment.plugin.configurePlugin
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractExpressionTypeTestForAssignment : AbstractExpressionTypeTest() {
    override val configurator: AnalysisApiTestConfigurator =
        AnalysisApiFirTestConfiguratorFactory.createConfigurator(AnalysisApiTestConfiguratorFactoryData())

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.configurePlugin()
    }
}
