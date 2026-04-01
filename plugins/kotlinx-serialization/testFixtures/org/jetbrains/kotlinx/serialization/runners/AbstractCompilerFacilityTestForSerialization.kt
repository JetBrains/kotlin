/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.runners

import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractCompilerFacilityTest
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlinx.serialization.configureForKotlinxSerialization

abstract class AbstractCompilerFacilityTestForSerialization : AbstractCompilerFacilityTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData()
    )

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.configureForKotlinxSerialization()
    }
}
