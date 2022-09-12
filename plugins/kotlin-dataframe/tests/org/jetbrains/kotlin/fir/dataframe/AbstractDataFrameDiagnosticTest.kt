/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.fir.dataframe.services.BaseTestRunner
import org.jetbrains.kotlin.fir.dataframe.services.commonFirWithPluginFrontendConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest

abstract class AbstractDataFrameDiagnosticTest : BaseTestRunner() {
    override fun TestConfigurationBuilder.configuration() {
        commonFirWithPluginFrontendConfiguration()
    }
}
