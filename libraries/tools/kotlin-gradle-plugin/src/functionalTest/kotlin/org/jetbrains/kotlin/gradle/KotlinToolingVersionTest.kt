/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class KotlinToolingVersionTest {

    @Test
    fun `test - kotlinToolingVersion is cached in project`() {
        val project = ProjectBuilder.builder().build()
        assertSame(project.kotlinToolingVersion, project.kotlinToolingVersion)
    }

    @Test
    fun `test - kotlinToolingVersion matches kotlinPluginVersion string`() {
        val project = ProjectBuilder.builder().build()
        assertEquals(project.getKotlinPluginVersion(), project.kotlinToolingVersion.toString())
    }
}
