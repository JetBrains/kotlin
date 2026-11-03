/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertTrue

class KT89689DevServerWatchesProcessedResourcesTest {

    @Test
    fun `wasm dev server watches processed resources`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.wasmJs {
            browser()
            binaries.executable()
        }
        project.evaluate()

        val devServer = (project.tasks.getByName("wasmJsBrowserDevelopmentRun") as KotlinWebpack)
            .devServerProperty.get()
        val processedResources = devServer.statics.single {
            "processedResources/wasmJs/main" in it.directory
        }

        assertTrue(
            processedResources.watch,
            "The Wasm processed resources directory must be watched so changed resources reload the browser"
        )
    }

    @Test
    fun `dev server watches processed resources`() {
        val project = buildProjectWithMPP()
        project.multiplatformExtension.js {
            browser()
            binaries.executable()
        }
        project.evaluate()

        val devServer = (project.tasks.getByName("jsBrowserDevelopmentRun") as KotlinWebpack)
            .devServerProperty.get()
        val processedResources = devServer.statics.single {
            "processedResources/js/main" in it.directory
        }

        assertTrue(
            processedResources.watch,
            "The processed resources directory must be watched so changed resources reload the browser"
        )
    }
}
