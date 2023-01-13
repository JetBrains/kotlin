/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.*
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.resolveIdeDependencies
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.fail

@MppGradlePluginTests
@DisplayName("Multiplatform IDE dependency resolution")
class MppIdeDependencyResolutionIT : KGPBaseTest() {
    @GradleTest
    fun testCommonizedPlatformDependencyResolution(gradleVersion: GradleVersion) {
        with(project("commonizeHierarchically", gradleVersion)) {
            resolveIdeDependencies(":p1") { dependencies ->
                if (task(":commonizeNativeDistribution") == null) fail("Missing :commonizeNativeDistribution task")

                fun Iterable<IdeaKotlinDependency>.filterNativePlatformDependencies() =
                    filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                        .filter { !it.isNativeStdlib }
                        .filter { it.isNativeDistribution }
                        .filter { it.binaryType == IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE }

                val nativeMainDependencies = dependencies["nativeMain"].filterNativePlatformDependencies()
                val nativeTestDependencies = dependencies["nativeTest"].filterNativePlatformDependencies()
                val linuxMainDependencies = dependencies["linuxMain"].filterNativePlatformDependencies()
                val linuxTestDependencies = dependencies["linuxTest"].filterNativePlatformDependencies()

                /* Check test and main receive the same dependencies */
                run {
                    nativeMainDependencies.assertMatches(nativeTestDependencies)
                    linuxMainDependencies.assertMatches(linuxTestDependencies)
                }

                /* Check all dependencies are marked as commonized and commonizer target match */
                run {
                    nativeMainDependencies.plus(linuxMainDependencies).forEach { dependency ->
                        if (!dependency.isCommonized) fail("$dependency is not marked as 'isCommonized'")
                    }

                    val nativeMainTarget = CommonizerTarget(
                        LINUX_X64, LINUX_ARM64, MACOS_X64, MACOS_ARM64, IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64, MINGW_X64, MINGW_X86
                    )

                    nativeMainDependencies.forEach { dependency ->
                        assertEquals(nativeMainTarget.identityString, dependency.klibExtra?.commonizerTarget)
                    }

                    val linuxMainTarget = CommonizerTarget(LINUX_X64, LINUX_ARM64)
                    linuxMainDependencies.forEach { dependency ->
                        assertEquals(linuxMainTarget.identityString, dependency.klibExtra?.commonizerTarget)
                    }
                }

                /* Find posix library */
                run {
                    nativeMainDependencies.assertMatches(
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.posix:.*")),
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.*"))
                    )

                    linuxMainDependencies.assertMatches(
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.posix:.*")),
                        binaryCoordinates(Regex("org\\.jetbrains\\.kotlin\\.native:platform.*"))
                    )
                }
            }
        }
    }

    @GradleTest
    fun testCinterops(gradleVersion: GradleVersion) {
        fun Iterable<IdeaKotlinDependency>.cinteropDependencies() =
            this.filterIsInstance<IdeaKotlinBinaryDependency>().filter {
                it.klibExtra?.isInterop == true && !it.isNativeStdlib && !it.isNativeDistribution
            }

        project(projectName = "cinteropImport", gradleVersion = gradleVersion) {
            build(":dep-with-cinterop:publishAllPublicationsToBuildRepository")

            resolveIdeDependencies("dep-with-cinterop") { dependencies ->
                dependencies["commonMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))
                dependencies["commonTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))
                dependencies["linuxX64Main"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_x64")))
                dependencies["linuxArm64Main"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_arm64")))
                dependencies["linuxX64Test"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_x64")))
                dependencies["linuxArm64Test"].cinteropDependencies().assertMatches(binaryCoordinates(Regex("a:dep.*linux_arm64")))
            }

            resolveIdeDependencies("client-for-binary-dep") { dependencies ->
                dependencies["commonMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))
                dependencies["commonTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))

                // CInterops are currently imported as extra roots of a platform publication, not as separate libraries
                // This is a bit inconsistent with other CInterop dependencies, but correctly represents the published artifacts
                fun assertDependencyOnPublishedProjectCInterop(sourceSetName: String) {
                    val publishedProjectDependencies = dependencies[sourceSetName].filterIsInstance<IdeaKotlinResolvedBinaryDependency>()
                        .filter { it.coordinates?.module?.contains("dep-with-cinterop") == true }

                    val fileNames = publishedProjectDependencies
                        .flatMap { dependency -> dependency.classpath }
                        .map { file -> file.name }
                        .toSet()

                    assert(fileNames == setOf("dep-with-cinterop.klib", "dep-with-cinterop-cinterop-dep.klib")) {
                        """Unexpected cinterop dependencies for the source set :client-for-binary-dep:$sourceSetName.
                            |Expected a project dependency and a cinterop dependency, but instead found:
                            |$fileNames""".trimMargin()
                    }
                }

                assertDependencyOnPublishedProjectCInterop("linuxX64Main")
                assertDependencyOnPublishedProjectCInterop("linuxX64Test")
                assertDependencyOnPublishedProjectCInterop("linuxArm64Main")
                assertDependencyOnPublishedProjectCInterop("linuxArm64Test")
            }

            resolveIdeDependencies("client-for-project-to-project-dep") { dependencies ->
                dependencies["commonMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))
                dependencies["commonTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep.*\\(linux_arm64, linux_x64\\)")))

                dependencies["linuxX64Main"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_x64")))
                dependencies["linuxX64Test"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_x64")))
                dependencies["linuxArm64Main"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_arm64")))
                dependencies["linuxArm64Test"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*linux_arm64")))
            }

            resolveIdeDependencies("client-with-complex-hierarchy") { dependencies ->
                dependencies["commonMain"].cinteropDependencies().assertMatches()
                dependencies["commonTest"].cinteropDependencies().assertMatches()
                dependencies["nativeMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*\\(linux_arm64, linux_x64\\)"))
                )
                dependencies["nativeTest"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*\\(linux_arm64, linux_x64\\)"))
                )

                dependencies["linuxArmMain"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_arm64$")))
                dependencies["linuxArmTest"].cinteropDependencies()
                    .assertMatches(binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_arm64$")))

                dependencies["linuxIntermediateMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$")),
                )
                dependencies["linuxIntermediateTest"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$")),
                )
                dependencies["linuxMain"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$")),
                )
                dependencies["linuxTest"].cinteropDependencies().assertMatches(
                    binaryCoordinates(Regex("a:dep-with-cinterop-cinterop-dep.*:linux_x64$")),
                    binaryCoordinates(Regex("a:client-with-complex-hierarchy-cinterop-w.*:linux_x64$"))
                )
            }
        }
    }
}
