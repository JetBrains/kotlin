/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.test.analysisApi

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractSourceLikeGetOrBuildFirTest
import org.jetbrains.kotlin.assignment.plugin.configurePlugin
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder

abstract class AbstractGetOrBuildFirTestForAssignment : AbstractSourceLikeGetOrBuildFirTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.configurePlugin()
    }
}
