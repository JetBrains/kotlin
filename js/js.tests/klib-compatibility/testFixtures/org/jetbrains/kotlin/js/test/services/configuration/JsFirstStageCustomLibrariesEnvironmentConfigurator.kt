/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.services.configuration

import org.jetbrains.kotlin.js.test.klib.customJsCompilerSettings
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.runtimeClasspathProviders

class JsFirstStageCustomLibrariesEnvironmentConfigurator(testServices: TestServices) : JsFirstStageEnvironmentConfigurator(testServices) {
    override fun getRuntimePathsForModule(module: TestModule, testServices: TestServices): List<String> {
        val result = mutableListOf(
            customJsCompilerSettings.stdlib.absolutePath,
            customJsCompilerSettings.kotlinTest.absolutePath
        )
        val runtimeClasspaths = testServices.runtimeClasspathProviders.flatMap { it.runtimeClassPaths(module) }
        runtimeClasspaths.mapTo(result) { it.absolutePath }
        return result
    }
}