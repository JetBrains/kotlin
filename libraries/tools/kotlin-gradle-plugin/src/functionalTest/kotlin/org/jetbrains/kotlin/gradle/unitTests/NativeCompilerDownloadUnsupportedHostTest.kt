/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.utils.setupNativeCompiler
import org.jetbrains.kotlin.gradle.targets.native.toolchain.NoopKotlinNativeProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.withModifiedSystemProperties
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.parallel.Isolated
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Isolated("Modifies system properties")
class NativeCompilerDownloadUnsupportedHostTest {

    @Test
    fun `setupNativeCompiler skips the Kotlin Native distribution download on an unsupported host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            assertFalse(HostManager.hostIsSupported, "the test must simulate a host unsupported by Kotlin/Native")

            val project = buildProjectWithMPP(
                preApplyCode = {
                    // The legacy, non-toolchain distribution setup path: it downloads the
                    // distribution eagerly, unlike the lazy toolchain build service.
                    project.extraProperties.set("kotlin.native.toolchain.enabled", false)
                },
            ) {
                configureRepositoriesForTests()
            }.evaluate()

            assertNull(
                project.nativeProperties.userProvidedNativeHome.orNull,
                "the test must not run with a kotlin.native.home override: the override branch skips the download"
            )
            assertFalse(
                project.nativeProperties.isToolchainEnabled.get(),
                "the test must run the legacy, non-toolchain distribution setup path"
            )

            // Before the fix, this resolved 'kotlin-native-prebuilt-...-linux-aarch64' eagerly,
            // failing the whole build at configuration time even for JVM-only tasks.
            @Suppress("DEPRECATION") // still the default configuration-phase setup in released KGP
            project.setupNativeCompiler(KonanTarget.LINUX_X64)
        }
    }

    @Test
    fun `native compile tasks get a no-op Kotlin Native provider on an unsupported host`() {
        withModifiedSystemProperties("os.name" to "Linux", "os.arch" to "aarch64") {
            val project = buildProjectWithMPP(
                preApplyCode = {
                    project.extraProperties.set("kotlin.native.toolchain.enabled", false)
                },
            ) {
                configureRepositoriesForTests()
                project.multiplatformExtension.linuxX64()
            }.evaluate()

            project.tasks.withType(KotlinNativeCompile::class.java).forEach { compileTask ->
                assertTrue(
                    compileTask.kotlinNativeProvider.get() is NoopKotlinNativeProvider,
                    "native compile tasks must not require a Kotlin/Native distribution on an unsupported host"
                )
            }
        }
    }
}
