/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(KotlinNativeCacheApi::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals

class DisabledNativeCacheTest {

    @Test
    fun `test deprecated native cache property`() {
        with(
            buildProjectWithMPP(
                preApplyCode = {
                    project.extra.set("kotlin.native.cacheKind", NativeCacheKind.NONE.name)
                }
            )
        ) {
            kotlin {
                listOf(linuxX64(), mingwX64(), macosArm64()).forEach { target ->
                    target.binaries.staticLib()
                }
            }

            evaluate()

            assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties)
        }
    }

    @Test
    fun `test native cache is disabled`() {
        with(buildProjectWithMPP()) {
            kotlin {
                listOf(linuxX64(), mingwX64(), macosArm64()).forEach { target ->
                    target.binaries.staticLib {
                        disableNativeCache(
                            currentVersionForDisableCache,
                            "Disabled in Kotlin 2.3.0 for tests",
                            URI("https://kotlinlang.org")
                        )
                    }
                }
            }

            evaluate()

            assertContainsDiagnostic(KotlinToolingDiagnostics.NativeCacheDisabledDiagnostic)

            val konanLinuxCacheKind = konanCacheKind(LINUX_X64_LINK_TASK_NAME)
            val konanMacCacheKind = konanCacheKind(MAC_ARM64_LINK_TASK_NAME)
            val konanWinCacheKind = konanCacheKind(WIN_X64_LINK_TASK_NAME)

            assertEquals(
                NativeCacheKind.NONE,
                konanLinuxCacheKind.get(),
                "Native cache should be disabled on Linux target for version ${getKotlinPluginVersion()}"
            )

            assertEquals(
                NativeCacheKind.NONE,
                konanMacCacheKind.get(),
                "Native cache should be disabled on macOS target for version ${getKotlinPluginVersion()}"
            )

            assertEquals(
                NativeCacheKind.NONE,
                konanWinCacheKind.get(),
                "Native cache should be disabled on Windows target for version ${getKotlinPluginVersion()}"
            )
        }
    }

    companion object {
        private const val MAC_ARM64_LINK_TASK_NAME = "linkDebugStaticMacosArm64"
        private const val WIN_X64_LINK_TASK_NAME = "linkDebugStaticMingwX64"
        private const val LINUX_X64_LINK_TASK_NAME = "linkDebugStaticLinuxX64"
    }
}

private val ProjectInternal.currentVersionForDisableCache
    get() = KotlinToolingVersion(getKotlinPluginVersion())

private fun ProjectInternal.konanCacheKind(linkTask: String): Provider<NativeCacheKind> =
    tasks.named(linkTask, KotlinNativeLink::class.java).flatMap { it.konanCacheKind }

