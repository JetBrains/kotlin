/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.utils

import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseSet
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File

fun createTestPhaseConfig(testServices: TestServices, module: TestModule): PhaseConfig {
    val debugMode = DebugMode.fromSystemProperty("kotlin.js.debugMode")
    return if (debugMode >= DebugMode.SUPER_DEBUG) {
        val dumpOutputDir = File(
            JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices),
            JsEnvironmentConfigurator.getKlibArtifactSimpleName(testServices, module.name) + "-irdump"
        )
        PhaseConfig(
            toDumpStateAfter = PhaseSet.ALL,
            dumpToDirectory = dumpOutputDir.path,
        )
    } else {
        PhaseConfig()
    }
}
