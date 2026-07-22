/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.yarn

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.internal.KgpBuildConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YarnRootExtensionTest {

    @Test
    fun checkDefaultYarnVersion() {
        val project = ProjectBuilder.builder().build()
        // Apply the KotlinMultiplatformPluginWrapper first so the necessary wiring takes places,
        // consistent with how WasmYarnPlugin is applied in real projects.
        project.plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
        val yarnRootExtension = YarnPlugin.apply(project)
        assertEquals(
            KgpBuildConstants.DEFAULT_YARN_VERSION,
            yarnRootExtension.versionProperty.orNull,
        )
    }
}
