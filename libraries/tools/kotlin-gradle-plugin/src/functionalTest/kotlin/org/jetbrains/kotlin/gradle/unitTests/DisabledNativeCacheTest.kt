/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KotlinNativeCacheApi::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.native.DisableNativeCacheSettings
import org.jetbrains.kotlin.gradle.targets.native.appliesTo
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.unitTests.fus.collectedFusConfigurationTimeMetrics
import org.jetbrains.kotlin.gradle.unitTests.fus.enableFusOnCI
import org.jetbrains.kotlin.gradle.unitTests.utils.MockKonanHomeExtension
import org.jetbrains.kotlin.gradle.util.assertContainsDiagnostic
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DisabledNativeCacheTest {
    @JvmField
    @RegisterExtension
    val mockKonan = MockKonanHomeExtension()

    @Test
    fun `test deprecated native cache property`() {
        with(
            buildProjectWithMPP(
                preApplyCode = {
                    project.extraProperties.set("kotlin.native.cacheKind", NativeCacheKind.NONE.name)
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
                    project.extraProperties.set("kotlin.native.cacheKind.linuxX64", NativeCacheKind.NONE.name)
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
        with(mppProjectWithFakeKonan(mockKonan, nativeVersion = disableCacheVersionString)) {
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
        with(mppProjectWithFakeKonan(mockKonan, nativeVersion = disableCacheVersionString)) {
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
        with(mppProjectWithFakeKonan(mockKonan, nativeVersion = disableCacheVersionString, preApplyCode = { enableFusOnCI() })) {
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

            //init task to ensure configuration time metrics are collected
            tasks.named(LINUX_X64_LINK_DEBUG_TASK_NAME, KotlinNativeLink::class.java).get()

            assertNotNull(
                collectedFusConfigurationTimeMetrics.booleanMetrics.entries.singleOrNull {
                    it.key == BooleanMetrics.KOTLIN_NATIVE_CACHE_DISABLED && it.value
                },
                "FUS event is present for disabled native cache"
            )
        }
    }

    @Test
    fun `test disabled version covers its bug-fix releases only`() {
        fun isDisabled(disabledIn: String, nativeVersion: String) =
            DisableNativeCacheSettings(KotlinToolingVersion(disabledIn), "reason", null).appliesTo(KotlinToolingVersion(nativeVersion))

        assertTrue(isDisabled("2.4.0", "2.4.0"))
        assertTrue(isDisabled("2.4.0", "2.4.1"))
        assertTrue(isDisabled("2.4.0", "2.4.10"))
        assertTrue(isDisabled("2.4.0", "2.4.11"))
        assertTrue(isDisabled("2.4.0", "2.4.12"))
        assertTrue(isDisabled("2.4.0", "2.4.12-RC"))
        assertFalse(isDisabled("2.4.0", "2.4.20"))
        assertFalse(isDisabled("2.4.0", "2.4.21"))
        assertFalse(isDisabled("2.4.0", "2.5.0"))

        assertTrue(isDisabled("2.4.20", "2.4.20"))
        assertTrue(isDisabled("2.4.20", "2.4.21"))
        assertTrue(isDisabled("2.4.20", "2.4.22-RC"))
        assertFalse(isDisabled("2.4.20", "2.4.10"))
        assertFalse(isDisabled("2.4.20", "2.4.12"))
        assertFalse(isDisabled("2.4.20", "2.4.255-SNAPSHOT"))
        assertFalse(isDisabled("2.4.20", "3.4.20"))
    }

    @Test
    fun `test native cache is disabled for bug-fix release`() {
        Assumptions.assumeTrue(!HostManager.hostIsMingw) // No cacheable targets on Windows
        assertDebugCacheKindForNativeVersion(
            nativeVersion = currentVersionForDisableCache.withPatch(currentVersionForDisableCache.patch + 1),
            expected = NativeCacheKind.NONE,
        )
    }

    @Test
    fun `test native cache is enabled for another release`() {
        Assumptions.assumeTrue(!HostManager.hostIsMingw) // No cacheable targets on Windows
        assertDebugCacheKindForNativeVersion(
            nativeVersion = currentVersionForDisableCache.withPatch(currentVersionForDisableCache.patch + 20),
            expected = NativeCacheKind.STATIC,
        )
    }

    private fun assertDebugCacheKindForNativeVersion(nativeVersion: String, expected: NativeCacheKind) {
        with(mppProjectWithFakeKonan(mockKonan, nativeVersion = nativeVersion)) {
            kotlin {
                createCacheableTargets().first().binaries.staticLib {
                    disableNativeCache(currentVersionForDisableCache, "Disabled for tests")
                }
            }

            evaluate()

            val linkTask = if (HostManager.hostIsMac) IOS_ARM64_LINK_DEBUG_TASK_NAME else LINUX_X64_LINK_DEBUG_TASK_NAME
            assertEquals(
                expected,
                konanCacheKind(linkTask).get(),
                "Unexpected native cache kind for $linkTask with Kotlin/Native $nativeVersion and $currentVersionForDisableCache"
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

//    cacheableTargets.linux_x64 = \
//    linux_x64
//
//    cacheableTargets.mingw_x64 =
//
//    cacheableTargets.macos_arm64 = \
//    macos_arm64 \
//    ios_simulator_arm64 \
//    ios_arm64
// fixme: KT-89587 Clean up tests after iosX64 target deprecation
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
        )
        HostManager.hostIsLinux -> listOf(
            linuxX64()
        )
        else -> emptyList()
    }
}

/**
 * Selects the appropriate [DisableCacheInKotlinVersion] for the default native version.
 *
 * [KotlinNativeLink.disableCache] matches a [DisableCacheInKotlinVersion] only with its own release and its hotfixes.
 * Snapshot versions (e.g. `2.4.255-SNAPSHOT`) are outside of any release (e.g. `2.4.0`), so the match would fail.
 * To avoid this, the test project's `kotlin.native.version` is set to the exact version string
 * of the selected [DisableCacheInKotlinVersion].
 */
private val currentVersionForDisableCache: DisableCacheInKotlinVersion by lazy {
    val allInstances: List<DisableCacheInKotlinVersion> =
        DisableCacheInKotlinVersion::class.sealedSubclasses.mapNotNull { it.objectInstance }
    val nativeToolingVersion = KotlinToolingVersion(NativeCompilerDownloader.DEFAULT_KONAN_VERSION)

    allInstances.last { releaseVersion ->
        releaseVersion.major < nativeToolingVersion.major ||
                (releaseVersion.major == nativeToolingVersion.major && releaseVersion.minor < nativeToolingVersion.minor) ||
                (releaseVersion.major == nativeToolingVersion.major && releaseVersion.minor == nativeToolingVersion.minor && releaseVersion.patch <= nativeToolingVersion.patch)
    }
}

private val disableCacheVersionString: String
    get() = currentVersionForDisableCache.withPatch()

private fun DisableCacheInKotlinVersion.withPatch(patch: Int = this.patch): String = "$major.$minor.$patch"

private fun ProjectInternal.konanCacheKind(linkTask: String): Provider<NativeCacheKind> =
    tasks.named(linkTask, KotlinNativeLink::class.java).flatMap { it.konanCacheKind }

private fun mppProjectWithFakeKonan(
    fakeKonanRule: MockKonanHomeExtension,
    copyKonanProperties: Boolean = true,
    nativeVersion: String? = null,
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

        if (nativeVersion != null) {
            project.extraProperties.set("kotlin.native.version", nativeVersion)
        }

        preApplyCode()
    },
    projectBuilder = projectBuilder,
    code = code
)
