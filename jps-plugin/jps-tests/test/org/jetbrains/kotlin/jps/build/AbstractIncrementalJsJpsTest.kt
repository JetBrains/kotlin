/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder.Companion.JS_JPS_LOG
import org.jetbrains.kotlin.jps.model.JpsKotlinFacetModuleExtension
import org.jetbrains.kotlin.platform.js.JsPlatforms
import java.io.File

abstract class AbstractIncrementalJsJpsTest : AbstractIncrementalJpsTest() {
    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isJsEnabled = true)

    override fun doTest(testDataPath: String) {
        val buildLogFile = File(testDataPath).resolve(JS_JPS_LOG)
        if (!buildLogFile.exists()) {
            buildLogFile.writeText("JPS JS LOG PLACEHOLDER")
        }
        super.doTest(testDataPath)
    }

    override fun overrideModuleSettings() {
        myProject.modules.forEach {
            val facet = KotlinFacetSettings()
            facet.compilerArguments = K2JSCompilerArguments()
            facet.targetPlatform = JsPlatforms.defaultJsPlatform

            it.container.setChild(
                JpsKotlinFacetModuleExtension.KIND,
                JpsKotlinFacetModuleExtension(facet)
            )
        }
    }
}
