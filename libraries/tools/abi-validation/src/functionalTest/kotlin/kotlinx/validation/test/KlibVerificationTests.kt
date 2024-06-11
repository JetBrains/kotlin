/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.resolve
import kotlinx.validation.api.test
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal const val BANNED_TARGETS_PROPERTY_NAME = "binary.compatibility.validator.klib.targets.disabled.for.testing"

private fun KlibVerificationTests.checkKlibDump(
    buildResult: BuildResult,
    expectedDumpFileName: String,
    projectName: String = "testproject",
    dumpTask: String = ":apiDump"
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
            arguments.add(":apiCheck")
        }
    }
    private fun BaseKotlinScope.runApiDump() {
        runner {
            arguments.add(":apiDump")
        }
    }
    private fun assertApiCheckPassed(buildResult: BuildResult) {
        buildResult.assertTaskSuccess(":apiCheck")
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
    fun `apiCheck for native targets`() {
        val runner = test {
            baseProjectSetting()

            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")

            abiFile(projectName = "testproject") {
                resolve("/examples/classes/TopLevelDeclarations.klib.dump")
            }

            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck for native targets should fail when a class is not in a dump`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/BuildConfig.kt")
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/Empty.klib.dump")
            }
            runApiCheck()
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output)
                .contains("+final class com.company/BuildConfig { // com.company/BuildConfig|null[0]")
            tasks.filter { it.path.endsWith("ApiCheck") }
                .forEach {
                    assertTaskFailure(it.path)
                }
        }
    }

    @Test
    fun `apiDump should include target-specific sources`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            runApiDump()
        }

        runner.build().apply {
            checkKlibDump(
                this,
                "/examples/classes/AnotherBuildConfigLinuxArm64Extra.klib.dump"
            )
        }
    }

    @Test
    fun `apiDump with native targets along with JVM target`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/base/enableJvmInWithNativePlugin.gradle.kts")
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        runner.build().apply {
            checkKlibDump(this, "/examples/classes/AnotherBuildConfig.klib.dump")

            val jvmApiDump = rootProjectDir.resolve("$API_DIR/testproject.api")
            assertTrue(jvmApiDump.exists(), "No API dump for JVM")

            val jvmExpected = readFileList("/examples/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(jvmApiDump.readText()).isEqualTo(jvmExpected)
        }
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
    fun `apiCheck for native targets using v1 signatures`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/signatures/v1.gradle.kts")
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")

            abiFile(projectName = "testproject") {
                resolve("/examples/classes/TopLevelDeclarations.klib.v1.dump")
            }

            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiDump for native targets should fail when using invalid signature version`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/signatures/invalid.gradle.kts")
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            runApiDump()
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains("Unsupported KLib signature version '100500'")
        }
    }

    @Test
    fun `apiDump should work for Apple-targets`() {
        Assume.assumeTrue(HostManager().isEnabled(KonanTarget.MACOS_ARM64))
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/appleTargets/targets.gradle.kts")
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/TopLevelDeclarations.klib.all.dump")
    }

    @Test
    fun `apiCheck should work for Apple-targets`() {
        Assume.assumeTrue(HostManager().isEnabled(KonanTarget.MACOS_ARM64))
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/appleTargets/targets.gradle.kts")
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/TopLevelDeclarations.klib.all.dump")
            }
            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck should not fail if a target is not supported`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":apiCheck")
            }
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck should ignore unsupported targets by default`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                // note that the regular dump is used, where linuxArm64 is presented
                resolve("/examples/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":apiCheck")
            }
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck should fail for unsupported targets with strict mode turned on`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/unsupported/enforce.gradle.kts")
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                // note that the regular dump is used, where linuxArm64 is presented
                resolve("/examples/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":apiCheck")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiExtractForValidation")
        }
    }

    @Test
    fun `klibDump should infer a dump for unsupported target from similar enough target`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/TopLevelDeclarations.klib.with.linux.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `check sorting for target-specific declarations`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            addToSrcSet("/examples/classes/TopLevelDeclarationsExp.kt")
            addToSrcSet("/examples/classes/TopLevelDeclarationsLinuxOnly.kt", "linuxMain")
            addToSrcSet("/examples/classes/TopLevelDeclarationsMingwOnly.kt", "mingwMain")
            addToSrcSet("/examples/classes/TopLevelDeclarationsAndroidOnly.kt", "androidNativeMain")


            runner {
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/TopLevelDeclarations.klib.diverging.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `infer a dump for a target with custom name`() {
        val runner = test {
            settingsGradleKts {
                resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                resolve("/examples/gradle/base/withNativePluginAndNoTargets.gradle.kts")
            }
            additionalBuildConfig("/examples/gradle/configuration/grouping/clashingTargetNames.gradle.kts")
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfigLinuxArm64.kt", "linuxMain")
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linux")
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/TopLevelDeclarations.klib.with.guessed.linux.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `klibDump should fail when the only target in the project is disabled`() {
        val runner = test {
            settingsGradleKts {
                resolve("/examples/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                resolve("/examples/gradle/base/withNativePluginAndSingleTarget.gradle.kts")
            }
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            addToSrcSet("/examples/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":klibApiDump")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":linuxArm64ApiInfer")
            Assertions.assertThat(output).contains(
                "The target linuxArm64 is not supported by the host compiler " +
                        "and there are no targets similar to linuxArm64 to infer a dump from it."
            )
        }
    }

    @Test
    fun `klibDump if all klib-targets are unavailable`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            runner {
                arguments.add(
                    "-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64,linuxX64,mingwX64," +
                            "androidNativeArm32,androidNativeArm64,androidNativeX64,androidNativeX86"
                )
                arguments.add(":klibApiDump")
            }
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains(
                "is not supported by the host compiler and there are no targets similar to"
            )
        }
    }

    @Test
    fun `klibCheck if all klib-targets are unavailable`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                // note that the regular dump is used, where linuxArm64 is presented
                resolve("/examples/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add(
                    "-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64,linuxX64,mingwX64," +
                            "androidNativeArm32,androidNativeArm64,androidNativeX64,androidNativeX86"
                )
                arguments.add(":klibApiCheck")
            }
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains(
                "KLib ABI dump/validation requires at least one enabled klib target, but none were found."
            )
        }
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
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/AnotherBuildConfig.klib.clash.dump",
            dumpTask = ":klibApiDump"
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
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/AnotherBuildConfig.klib.custom.dump",
            dumpTask = ":klibApiDump"
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
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/examples/classes/AnotherBuildConfigLinux.klib.grouping.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `apiDump should work with web targets`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/nonNativeKlibTargets/targets.gradle.kts")
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfig.klib.web.dump")
    }

    @Test
    fun `apiCheck should work with web targets`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/nonNativeKlibTargets/targets.gradle.kts")
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/AnotherBuildConfig.klib.web.dump")
            }
            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `check dump is updated on added declaration`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            runApiDump()
        }
        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfig.klib.dump")

        // Update the source file by adding a declaration
        val updatedSourceFile = File(this::class.java.getResource(
            "/examples/classes/AnotherBuildConfigModified.kt")!!.toURI()
        )
        val existingSource = runner.projectDir.resolve(
            "src/commonMain/kotlin/AnotherBuildConfig.kt"
        )
        Files.write(existingSource.toPath(), updatedSourceFile.readBytes())

        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfigModified.klib.dump")
    }

    @Test
    fun `check dump is updated on a declaration added to some source sets`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            runApiDump()
        }
        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfig.klib.dump")

        // Update the source file by adding a declaration
        val updatedSourceFile = File(this::class.java.getResource(
            "/examples/classes/AnotherBuildConfigLinuxArm64.kt")!!.toURI()
        )
        val existingSource = runner.projectDir.resolve(
            "src/linuxArm64Main/kotlin/AnotherBuildConfigLinuxArm64.kt"
        )
        existingSource.parentFile.mkdirs()
        Files.write(existingSource.toPath(), updatedSourceFile.readBytes())

        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfigLinuxArm64Extra.klib.dump")
    }

    @Test
    fun `re-validate dump after sources updated`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/AnotherBuildConfig.klib.dump")
            }
            runApiCheck()
        }
        assertApiCheckPassed(runner.build())

        // Update the source file by adding a declaration
        val updatedSourceFile = File(this::class.java.getResource(
            "/examples/classes/AnotherBuildConfigModified.kt")!!.toURI()
        )
        val existingSource = runner.projectDir.resolve(
            "src/commonMain/kotlin/AnotherBuildConfig.kt"
        )
        Files.write(existingSource.toPath(), updatedSourceFile.readBytes())

        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiCheck")
        }
    }

    @Test
    fun `validation should fail on target rename`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt")
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/AnotherBuildConfig.klib.renamedTarget.dump")
            }
            runApiCheck()
        }
        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains(
                "  -// Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, " +
                        "androidNativeX86, linuxArm64.linux, linuxX64, mingwX64]"
            )
        }
    }

    @Test
    fun `apiDump should not fail for empty project`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            runApiDump()
        }

        runner.build().apply {
            assertTaskSkipped(":klibApiDump")
        }
        assertFalse(runner.projectDir.resolve("api").exists())
    }

    @Test
    fun `apiDump should not fail if there is only one target`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt", sourceSet = "linuxX64Main")
            runApiDump()
        }
        checkKlibDump(runner.build(), "/examples/classes/AnotherBuildConfig.klib.linuxX64Only.dump")
    }

    @Test
    fun `apiCheck should not fail for empty project`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/examples/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            runApiCheck()
        }
        runner.build()
    }

    @Test
    fun `apiDump for a project with generated sources only`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/generatedSources/generatedSources.gradle.kts")
            // TODO: enable configuration cache back when we start skipping tasks correctly
            runner(withConfigurationCache = false) {
                arguments.add(":apiDump")
            }
        }
        checkKlibDump(runner.build(), "/examples/classes/GeneratedSources.klib.dump")
    }

    @Test
    fun `apiCheck for a project with generated sources only`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/examples/gradle/configuration/generatedSources/generatedSources.gradle.kts")
            abiFile(projectName = "testproject") {
                resolve("/examples/classes/GeneratedSources.klib.dump")
            }
            // TODO: enable configuration cache back when we start skipping tasks correctly
            runner(withConfigurationCache = false) {
                arguments.add(":apiCheck")
            }
        }
        assertApiCheckPassed(runner.build())
    }
}
