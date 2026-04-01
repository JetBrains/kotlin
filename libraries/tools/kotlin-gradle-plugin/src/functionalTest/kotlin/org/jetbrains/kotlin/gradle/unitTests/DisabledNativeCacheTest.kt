/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")
@file:OptIn(KotlinNativeCacheApi::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.extra
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.unitTests.fus.collectedFusConfigurationTimeMetrics
import org.jetbrains.kotlin.gradle.unitTests.fus.enableFusOnCI
import org.jetbrains.kotlin.gradle.unitTests.utils.MockKonanHomeExtension
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DisabledNativeCacheTest {
    @JvmField
    @RegisterExtension
    val mockKonan = MockKonanHomeExtension()

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
    fun `test deprecated native cache property for linuxX64 target`() {
        with(
            buildProjectWithMPP(
                preApplyCode = {
                    project.extra.set("kotlin.native.cacheKind.linuxX64", NativeCacheKind.NONE.name)
                }
            )
        ) {
            kotlin {
                linuxX64().binaries.staticLib()
            }

            evaluate()

            assertContainsDiagnostic(KotlinToolingDiagnostics.DeprecatedWarningGradleProperties)
        }
    }

    @Test
    fun `test native cache is enabled by default`() {
        Assumptions.assumeTrue(!HostManager.hostIsMingw) // No cacheable targets on Windows
        with(mppProjectWithFakeKonan(mockKonan)) {
            kotlin {
                createCacheableTargets().forEach { target ->
                    target.binaries.staticLib()
                }
            }

            evaluate()

            if (HostManager.hostIsMac) {
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(MAC_ARM64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be enabled on macOS Arm64 DEBUG target"
                )
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(MAC_ARM64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be enabled on macOS Arm64 RELEASE target"
                )
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(IOS_ARM64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be enabled on iOS Arm64 DEBUG target"
                )
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(IOS_ARM64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be enabled on iOS Arm64 RELEASE target"
                )
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(IOS_SIMULATOR_ARM64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be enabled on iOS Simulator Arm64 DEBUG target"
                )
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(IOS_SIMULATOR_ARM64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be enabled on iOS Simulator Arm64 RELEASE target"
                )
            } else if (HostManager.hostIsLinux) {
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(LINUX_X64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be enabled on Linux x64 DEBUG target"
                )
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(LINUX_X64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be enabled on Linux x64 RELEASE target"
                )
            }
        }
    }

    @Test
    fun `test native cache is disabled`() {
        Assumptions.assumeTrue(!HostManager.hostIsMingw) // No cacheable targets on Windows
        with(mppProjectWithFakeKonan(mockKonan)) {
            kotlin {
                createCacheableTargets().forEach { target ->
                    target.binaries.staticLib {
                        disableNativeCache(
                            currentVersionForDisableCache,
                            "Disabled for tests",
                            URI("https://kotlinlang.org")
                        )
                    }
                }
            }

            evaluate()

            if (HostManager.hostIsMac) {
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(MAC_ARM64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be disabled on macOS Arm64 DEBUG target for version ${getKotlinPluginVersion()}"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(MAC_ARM64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be disabled on macOS Arm64 RELEASE target for version ${getKotlinPluginVersion()}"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(IOS_ARM64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be disabled on iOS Arm64 DEBUG target for version ${getKotlinPluginVersion()}"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(IOS_ARM64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be disabled on iOS Arm64 RELEASE target for version ${getKotlinPluginVersion()}"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(IOS_SIMULATOR_ARM64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be disabled on iOS Simulator Arm64 DEBUG target for version ${getKotlinPluginVersion()}"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(IOS_SIMULATOR_ARM64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be disabled on iOS Simulator Arm64 RELEASE target for version ${getKotlinPluginVersion()}"
                )
            } else if (HostManager.hostIsLinux) {
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(LINUX_X64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be disabled on Linux x64 DEBUG target for version ${getKotlinPluginVersion()}"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(LINUX_X64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be disabled on Linux x64 RELEASE target for version ${getKotlinPluginVersion()}"
                )
            }
        }
    }

    @Test
    fun `test native cache is disabled for particular buildType`() {
        Assumptions.assumeTrue(!HostManager.hostIsMingw) // No cacheable targets on Windows
        with(mppProjectWithFakeKonan(mockKonan)) {
            kotlin {
                val target = if (HostManager.hostIsMac) macosArm64() else linuxX64()
                target.binaries.staticLib {
                    if (buildType == NativeBuildType.DEBUG) {
                        disableNativeCache(
                            currentVersionForDisableCache,
                            "Disabled for tests for DEBUG only",
                            URI("https://kotlinlang.org")
                        )
                    }
                }
            }

            evaluate()

            if (HostManager.hostIsMac) {
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(MAC_ARM64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be enabled on macOS Arm64 RELEASE target"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(MAC_ARM64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be disabled on macOS Arm64 DEBUG target"
                )
            } else if (HostManager.hostIsLinux) {
                assertEquals(
                    NativeCacheKind.STATIC,
                    konanCacheKind(LINUX_X64_LINK_RELEASE_TASK_NAME).get(),
                    "Native cache should be enabled on Linux x64 RELEASE target"
                )
                assertEquals(
                    NativeCacheKind.NONE,
                    konanCacheKind(LINUX_X64_LINK_DEBUG_TASK_NAME).get(),
                    "Native cache should be disabled on Linux x64 DEBUG target"
                )
            }
        }
    }

    @Test
    fun `test disable native cache FUS event present`() {
        with(mppProjectWithFakeKonan(mockKonan, preApplyCode = { enableFusOnCI() })) {
            kotlin {
                linuxX64().binaries.staticLib {
                    disableNativeCache(
                        currentVersionForDisableCache,
                        "Disabled for tests",
                        URI("https://kotlinlang.org")
                    )
                }
            }

            evaluate()

            assertNotNull(
                collectedFusConfigurationTimeMetrics.booleanMetrics.entries.singleOrNull {
                    it.key == BooleanMetrics.KOTLIN_NATIVE_CACHE_DISABLED && it.value
                },
                "FUS event is present for disabled native cache"
            )
        }
    }

    @Test
    fun `test disable native cache FUS event not present`() {
        with(mppProjectWithFakeKonan(mockKonan, preApplyCode = { enableFusOnCI() })) {
            kotlin {
                linuxX64()
            }

            evaluate()

            assertNotNull(
                collectedFusConfigurationTimeMetrics.booleanMetrics.keys.none {
                    it.name == BooleanMetrics.KOTLIN_NATIVE_CACHE_DISABLED.name
                },
                "FUS event is not present for disabled native cache"
            )
        }
    }

    companion object {
        private const val MAC_ARM64_LINK_DEBUG_TASK_NAME = "linkDebugStaticMacosArm64"
        private const val MAC_ARM64_LINK_RELEASE_TASK_NAME = "linkReleaseStaticMacosArm64"
        private const val LINUX_X64_LINK_DEBUG_TASK_NAME = "linkDebugStaticLinuxX64"
        private const val LINUX_X64_LINK_RELEASE_TASK_NAME = "linkReleaseStaticLinuxX64"
        private const val IOS_ARM64_LINK_DEBUG_TASK_NAME = "linkDebugStaticIosArm64"
        private const val IOS_ARM64_LINK_RELEASE_TASK_NAME = "linkReleaseStaticIosArm64"
        private const val IOS_SIMULATOR_ARM64_LINK_DEBUG_TASK_NAME = "linkDebugStaticIosSimulatorArm64"
        private const val IOS_SIMULATOR_ARM64_LINK_RELEASE_TASK_NAME = "linkReleaseStaticIosSimulatorArm64"
    }
}

//    cacheableTargets.macos_x64 = \
//    macos_x64 \
//    ios_x64 \
//    ios_arm64
//
//    cacheableTargets.linux_x64 = \
//    linux_x64
//
//    cacheableTargets.mingw_x64 =
//
//    cacheableTargets.macos_arm64 = \
//    macos_arm64 \
//    ios_simulator_arm64 \
//    ios_arm64
@Suppress("DEPRECATION")
private fun KotlinMultiplatformExtension.createCacheableTargets(): List<KotlinNativeTarget> {
    val isArm64 = HostManager.hostArch() == "aarch64"

    return when {
        HostManager.hostIsMac && isArm64 -> listOf(
            iosArm64(),
            iosSimulatorArm64(),
            macosArm64()
        )
        HostManager.hostIsMac && !isArm64 -> listOf(
            iosArm64(),
            iosX64(),
            macosX64()
        )
        HostManager.hostIsLinux -> listOf(
            linuxX64()
        )
        else -> emptyList()
    }
}

private val ProjectInternal.currentVersionForDisableCache: DisableCacheInKotlinVersion
    get() {
        val allInstances: List<DisableCacheInKotlinVersion> =
            DisableCacheInKotlinVersion::class.sealedSubclasses.mapNotNull { it.objectInstance }
        val kotlinNativeVersion = nativeProperties.kotlinNativeVersion.get()
        val nativeToolingVersion = KotlinToolingVersion(kotlinNativeVersion)

        return allInstances.last { releaseVersion ->
            releaseVersion.major < nativeToolingVersion.major ||
                    (releaseVersion.major == nativeToolingVersion.major && releaseVersion.minor < nativeToolingVersion.minor) ||
                    (releaseVersion.major == nativeToolingVersion.major && releaseVersion.minor == nativeToolingVersion.minor && releaseVersion.patch <= nativeToolingVersion.patch)
        }
    }

private fun ProjectInternal.konanCacheKind(linkTask: String): Provider<NativeCacheKind> =
    tasks.named(linkTask, KotlinNativeLink::class.java).flatMap { it.konanCacheKind }

private fun mppProjectWithFakeKonan(
    fakeKonanRule: MockKonanHomeExtension,
    copyKonanProperties: Boolean = true,
    projectBuilder: ProjectBuilder.() -> Unit = { },
    preApplyCode: Project.() -> Unit = {},
    code: Project.() -> Unit = {},
) = buildProjectWithMPP(
    preApplyCode = {
        fakeKonanRule.setup(copyKonanProperties)

        project.extraProperties.set(
            NativeProperties.NATIVE_HOME.name,
            fakeKonanRule.konanHome.absolutePath
        )

        preApplyCode()
    },
    projectBuilder = projectBuilder,
    code = code
)
