/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.yarn

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.internal.KgpBuildConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WasmYarnRootExtensionTest {

    @Test
    fun checkDefaultYarnVersion() {
        val project = ProjectBuilder.builder().build()
        val yarnRootExtension = WasmYarnPlugin.apply(project)
        assertEquals(
            KgpBuildConstants.DEFAULT_YARN_VERSION,
            yarnRootExtension.versionProperty.orNull,
        )
    }
}
