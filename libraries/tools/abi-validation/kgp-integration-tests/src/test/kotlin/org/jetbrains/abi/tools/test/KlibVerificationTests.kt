/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.abi.tools.test.api.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertTrue

private fun KlibVerificationTests.checkKlibDump(
    buildResult: BuildResult,
    expectedDumpFileName: String,
    projectName: String = "testproject",
    dumpTask: String = ":updateKotlinAbi"
) {
    buildResult.assertTaskSuccess(dumpTask)

    val generatedDump = rootProjectAbiDump(projectName)
    assertTrue(generatedDump.exists(), "There are no dumps generated for KLibs")

    val expected = readFileList(expectedDumpFileName)

    Assertions.assertThat(generatedDump.readText()).isEqualTo(expected)
}

internal class KlibVerificationTests : BaseKotlinGradleTest() {
    private fun BaseKotlinScope.baseProjectSetting() {
        settingsGradleKts {
            resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
        }
        buildGradleKts {
            resolve("/examples/gradle/base/withNativePlugin.gradle.kts")
        }
    }

    private fun BaseKotlinScope.additionalBuildConfig(config: String) {
        buildGradleKts {
            resolve(config)
        }
    }

    private fun BaseKotlinScope.addToSrcSet(pathTestFile: String, sourceSet: String = "commonMain") {
        val fileName = Paths.get(pathTestFile).fileName.toString()
        kotlin(fileName, sourceSet) {
            resolve(pathTestFile)
        }
    }

    private fun BaseKotlinScope.runApiCheck() {
        runner {
            arguments.add(":checkKotlinAbi")
        }
    }

    private fun BaseKotlinScope.runApiDump() {
        runner {
            arguments.add(":updateKotlinAbi")
        }
    }

    private fun assertApiCheckPassed(buildResult: BuildResult) {
        buildResult.assertTaskSuccess(":checkKotlinAbi")
    }

    @Test
    fun `apiDump for native targets`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/TopLevelDeclarations.klib.with.linux.dump")
    }

    @Test
    fun testExcludeFileLevelMembers() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/ignoredMembers/ignoreFileMembers.gradle.kts")

            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")

            abiFile(projectName = "testproject") {
                resolve("/examples/classes/TopLevelDeclarations.klib.exclude.val.dump")
            }

            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun testIncludeFileLevelMembers() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/ignoredMembers/includeFileMembers.gradle.kts")

            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")

            abiFile(projectName = "testproject") {
                resolve("/examples/classes/TopLevelDeclarations.klib.include.val.dump")
            }

            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiDump should ignore a class listed in ignoredClasses`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            addToSrcSet("/examples/classes/BuildConfig.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfig.klib.dump")
    }

    @Test
    fun `apiDump should succeed if a class listed in ignoredClasses is not found`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfig.klib.dump")
    }

    @Test
    fun `apiDump should ignore all entities from a package listed in ingoredPackages`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/ignoredPackages/oneValidPackage.gradle.kts")
            addToSrcSet("/examples/classes/BuildConfig.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            addToSrcSet("/examples/classes/SubPackage.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfig.klib.dump")
    }

    @Test
    fun `apiDump should ignore all entities annotated with non-public markers`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/nonPublicMarkers/klib.gradle.kts")
            addToSrcSet("/examples/classes/HiddenDeclarations.kt")
            addToSrcSet("/examples/classes/NonPublicMarkers.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/HiddenDeclarations.klib.dump")
    }

    @Test
    fun `apiDump should not dump subclasses excluded via ignoredClasses`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/ignoreSubclasses/ignore.gradle.kts")
            addToSrcSet("/examples/classes/Subclasses.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/Subclasses.klib.dump")
    }

    @Test
    fun `target name clashing with a group name`() {
        val runner = test {
            settingsGradleKts {
                resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                resolve("/examples/gradle/base/withNativePluginAndNoTargets.gradle.kts")
                resolve("/examples/gradle/configuration/grouping/clashingTargetNames.gradle.kts")
            }
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            kotlin("AnotherBuildConfigLinuxX64.kt", "linuxMain") {
                resolve("/examples/classes/AnotherBuildConfigLinuxArm64.kt")
            }
            runner {
                arguments.add(":updateKotlinAbi")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/AnotherBuildConfig.klib.clash.dump",
            dumpTask = ":updateKotlinAbi"
        )
    }

    @Test
    fun `target name grouping with custom target names`() {
        val runner = test {
            settingsGradleKts {
                resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                resolve("/examples/gradle/base/withNativePluginAndNoTargets.gradle.kts")
                resolve("/examples/gradle/configuration/grouping/customTargetNames.gradle.kts")
            }
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            runner {
                arguments.add(":updateKotlinAbi")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/AnotherBuildConfig.klib.custom.dump",
            dumpTask = ":updateKotlinAbi"
        )
    }

    @Test
    fun `target name grouping`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            kotlin("AnotherBuildConfigLinuxX64.kt", "linuxX64Main") {
                resolve("/examples/classes/AnotherBuildConfigLinuxArm64.kt")
            }
            runner {
                arguments.add(":updateKotlinAbi")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/AnotherBuildConfigLinux.klib.grouping.dump",
            dumpTask = ":updateKotlinAbi"
        )
    }

}
