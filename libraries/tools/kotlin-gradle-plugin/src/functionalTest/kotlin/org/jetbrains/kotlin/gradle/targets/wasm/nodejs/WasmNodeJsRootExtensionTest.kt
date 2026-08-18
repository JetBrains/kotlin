/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.allDependenciesInternal
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WasmNodeJsRootExtensionTest {

    @Test
    fun `verify npm dependencies versions can be overridden`() {
        val project = buildProjectWithMPP {
            multiplatformExtension.apply {
                wasmJs {
                    browser()
                }
            }
        }

        project.evaluate()

        val wasmNodeJsRootExtension = project.extensions.getByType(WasmNodeJsRootExtension::class.java)
        val allDeps = wasmNodeJsRootExtension.versions.allDependenciesInternal(project.objects, project.providers)

        fun getWebpack() =
            allDeps.firstOrNull { it.name.orNull == "webpack" }

        assertNotEquals("CUSTOM-VERSION", getWebpack()?.resolvedVersion?.orNull)

        // users can override the default versions _after_ KGP is evaluated
        wasmNodeJsRootExtension.versions.apply {
            webpack.version = "CUSTOM-VERSION"
        }

        assertEquals("CUSTOM-VERSION", getWebpack()?.resolvedVersion?.orNull, "custom version overrides resolvedVersion")
        assertEquals("CUSTOM-VERSION", getWebpack()?.requestedVersion?.orNull, "custom version overrides requestedVersion")
    }
}
