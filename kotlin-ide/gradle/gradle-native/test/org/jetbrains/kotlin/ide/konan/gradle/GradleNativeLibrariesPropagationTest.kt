/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.gradle

import com.intellij.openapi.roots.DependencyScope
import org.jetbrains.kotlin.gradle.ModuleInfo
import org.jetbrains.kotlin.gradle.checkProjectStructure
import org.jetbrains.kotlin.idea.configuration.externalCompilerVersion
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Test
import org.junit.runners.Parameterized

class GradleNativeLibrariesPropagationTest : TestCaseWithFakeKotlinNative() {

    override fun getExternalSystemConfigFileName() = GradleConstants.KOTLIN_DSL_SCRIPT_NAME

    override fun testDataDirName() = "nativeLibraries"

    private val testedTargets = setOf("ios_arm64", "ios_x64", "watchos_arm32", "watchos_x86")

    @Test
    fun testCommonIOS() {
        configureProject()
        importProject()

        checkProjectStructure(
            myProject,
            projectPath,
            exhaustiveModuleList = false,
            exhaustiveSourceSourceRootList = false,
            exhaustiveDependencyList = false,
            exhaustiveTestsList = false
        ) {

            // No platform libraries should be propagated to commonMain since we have a JVM target.
            module("project_commonMain") {
                noPlatformLibrary("Foundation")
                noPlatformLibrary("CFNetwork")
                noPlatformLibrary("WatchKit")
            }

            module("project_appleMain") {
                // Common iOS/watchOS libraries are propagated.
                hasPlatformLibrary("Foundation", "watchos_arm32")

                // iOS- and watchOS-specific libraries are not propagated.
                noPlatformLibrary("CFNetwork")
                noPlatformLibrary("WatchKit")
            }

            module("project_iosMain") {
                // iOS libraries are propagated.
                hasPlatformLibrary("Foundation", "ios_arm64")
                hasPlatformLibrary("CFNetwork", "ios_arm64")

                // WatchKit is unavailable for iOS and shouldn't be propagated.
                noPlatformLibrary("WatchKit")
            }
        }
    }

    @Test
    fun testCommonIOSWithDisabledPropagation() {
        configureProject()
        importProject()

        // No dependencies should be propagated.
        checkProjectStructure(
            myProject,
            projectPath,
            exhaustiveModuleList = false,
            exhaustiveSourceSourceRootList = false,
            exhaustiveDependencyList = false,
            exhaustiveTestsList = false
        ) {

            module("project_commonMain") {
                noPlatformLibrary("Foundation")
                noPlatformLibrary("CFNetwork")
                noPlatformLibrary("WatchKit")
            }

            module("project_appleMain") {
                noPlatformLibrary("Foundation")
                noPlatformLibrary("CFNetwork")
                noPlatformLibrary("WatchKit")
            }

            module("project_iosMain") {
                noPlatformLibrary("Foundation")
                noPlatformLibrary("CFNetwork")
                noPlatformLibrary("WatchKit")
            }
        }
    }

    private val ModuleInfo.kotlinVersion: String
        get() = requireNotNull(module.externalCompilerVersion) { "External compiler version should not be null" }

    private fun ModuleInfo.noPlatformLibrary(libraryName: String, targets: Collection<String> = testedTargets) {
        targets.forEach { target ->
            assertNoLibraryDepForModule(module.name, "Kotlin/Native $kotlinVersion - $libraryName [$target]")
        }
    }

    private fun ModuleInfo.hasPlatformLibrary(libraryName: String, target: String) {
        libraryDependency("Kotlin/Native $kotlinVersion - $libraryName [$target]", DependencyScope.PROVIDED)
        noPlatformLibrary(libraryName, testedTargets - target)
    }

    companion object {
        @Parameterized.Parameters(name = "{index}: with Gradle-{0}")
        @Throws(Throwable::class)
        @JvmStatic
        fun data() = listOf(arrayOf("4.10.2"))
    }
}
