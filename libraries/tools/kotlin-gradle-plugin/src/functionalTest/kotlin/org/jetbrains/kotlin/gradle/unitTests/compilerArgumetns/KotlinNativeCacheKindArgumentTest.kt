/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests.compilerArgumetns

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule

class KotlinNativeCacheKindArgumentTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test
    fun testDefaultCacheKindForLinuxX64() {
        val project = createProject()

        project.evaluate()

        project.assertCacheKind(
            if (SystemUtils.IS_OS_LINUX) NativeCacheKind.STATIC
            else NativeCacheKind.NONE
        )
    }

    @Test
    fun testCacheKindNoneForLinuxX64() {
        val project = createProject(cacheKind = NativeCacheKind.NONE)

        project.evaluate()

        project.assertCacheKind(NativeCacheKind.NONE)
    }

    @Test
    fun testCacheKindDynamicForLinuxX64() {
        val project = createProject(cacheKind = NativeCacheKind.DYNAMIC)

        project.evaluate()

        project.assertCacheKind(NativeCacheKind.DYNAMIC)
    }

    @Test
    fun testCacheKindHeaderForLinuxX64() {
        val project = createProject(cacheKind = NativeCacheKind.HEADER)

        project.evaluate()

        project.assertCacheKind(NativeCacheKind.HEADER)
    }

    @Test
    fun testCacheKindDynamicForSpecificallyLinuxX64Target() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                project.extra.set("kotlin.native.cacheKind.linuxX64", NativeCacheKind.DYNAMIC.name)
            }
        ) {
            multiplatformExtension.linuxX64()
        }

        project.evaluate()

        project.assertCacheKind(NativeCacheKind.DYNAMIC)
    }

    private fun createProject(
        cacheKind: NativeCacheKind? = null,
    ) = buildProjectWithMPP(
        preApplyCode = {
            if (cacheKind != null) {
                project.extra.set("kotlin.native.cacheKind", cacheKind.name)
            }
        }
    ) {
        project.multiplatformExtension.linuxX64()
    }

    private fun Project.assertCacheKind(expectedCacheKind: NativeCacheKind) {
        val linkTask = tasks.named(
            LINK_TASK_NAME,
            KotlinNativeLink::class.java
        ).get()

        assertEquals(
            expectedCacheKind,
            linkTask.konanCacheKind.get(),
        )
    }

    companion object {
        private const val LINK_TASK_NAME = "linkDebugTestLinuxX64"
    }
}