/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder.Companion.JS_JPS_LOG
import java.io.File

abstract class AbstractIncrementalJsJpsTest : AbstractIncrementalJpsTest() {
    private var isICEnabledForJsBackup: Boolean = false

    override fun setUp() {
        super.setUp()
        isICEnabledForJsBackup = IncrementalCompilation.isEnabledForJs()
        IncrementalCompilation.setIsEnabledForJs(true)
    }

    override fun tearDown() {
        IncrementalCompilation.setIsEnabledForJs(isICEnabledForJsBackup)
        super.tearDown()
    }

    override fun configureDependencies() {
        AbstractKotlinJpsBuildTestCase.addKotlinJavaScriptStdlibDependency(myProject)
    }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isJsEnabled = true)

    override fun doTest(testDataPath: String) {
        val buildLogFile = File(testDataPath).resolve(JS_JPS_LOG)
        if (!buildLogFile.exists()) {
            buildLogFile.writeText("JPS JS LOG PLACEHOLDER")
        }
        super.doTest(testDataPath)
    }
}
