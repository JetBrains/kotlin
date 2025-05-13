/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.internal.os.OperatingSystem
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerDependent
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizedOutputLibraries
import org.jetbrains.kotlin.gradle.targets.native.internal.from
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.dumpKlibMetadataSignatures
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.reportSourceSetCommonizerDependencies
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.fail


@DisplayName("K/N tests for commonizer")
@NativeGradlePluginTests
open class CommonizerIT : KGPBaseTest() {
    companion object {
        private const val commonizerOutput = "Preparing commonized Kotlin/Native libraries"
    }

    @DisplayName("Commonize native distribution with Ios Linux and Windows")
    @GradleTest
    fun testCommonizeNativeDistributionWithIosLinuxWindows(gradleVersion: GradleVersion) {
        nativeProject("commonizeNativeDistributionWithIosLinuxWindows", gradleVersion) {

            build(":p1:cleanNativeDistributionCommonization")

            build("commonize", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=false") {
                assertTasksExecuted(":p1:commonizeNativeDistribution")
                assertOutputContains(commonizerOutput)
            }

            build("commonize", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=true") {
                assertTasksUpToDate(":p1:commonizeNativeDistribution")
                assertNativeDistributionCommonizationCacheHit()
                assertOutputContains("Native Distribution Commonization: All available targets are commonized already")
                assertOutputContains("Native Distribution Commonization: Lock acquired")
                assertOutputContains("Native Distribution Commonization: Lock released")
                assertOutputDoesNotContain(commonizerOutput)
            }

            build("commonize", "-Pkotlin.mpp.enableNativeDistributionCommonizationCache=false") {
                assertTasksExecuted(":p1:commonizeNativeDistribution")
                assertOutputContains("Native Distribution Commonization: Cache disabled")
                assertOutputContains(commonizerOutput)
            }
        }
    }

    @DisplayName("Clean commonized native distribution")
    @GradleTest
    fun testCleanNativeDistributionCommonization(gradleVersion: GradleVersion, @TempDir konanData: Path) {
        nativeProject("commonizeNativeDistributionWithIosLinuxWindows", gradleVersion) {
            val buildOptions = defaultBuildOptions.withBundledKotlinNative().copy(
                konanDataDir = konanData
            )

            fun commonizationResultFilesCount() = konanData
                .walk()
                .filter { it.contains(Path(KONAN_DISTRIBUTION_COMMONIZED_LIBS_DIR)) }
                .count()

            build(":p1:cleanNativeDistributionCommonization", buildOptions = buildOptions) {
                assertTasksExecuted(":p1:cleanNativeDistributionCommonization")
            }
            build(":p1:commonizeNativeDistribution", buildOptions = buildOptions) {
                assertTasksExecuted(":p1:commonizeNativeDistribution")
                if (commonizationResultFilesCount() == 0) fail("Expected some files after executing commonizeNativeDistribution")
            }

            build(":p1:cleanNativeDistributionCommonization", buildOptions = buildOptions) {
                assertTasksExecuted(":p1:cleanNativeDistributionCommonization")
                if (commonizationResultFilesCount() != 1) fail("Expected only .lock file after cleaning")
            }
        }
    }

    @DisplayName("Commonize Curl Interop UP-TO-DATE check")
    @GradleTest
    fun testCommonizeCurlInteropUTDCheck(gradleVersion: GradleVersion) {
        nativeProject(
            "commonizeCurlInterop",
            gradleVersion,
            // with CC enabled on Gradle 7 `CInteropCommonizerTask.getAllInteropGroupsForUpToDateCheck` nested input
            // always fails the up-to-date check
            buildOptions = defaultBuildOptions.disableConfigurationCacheForGradle7(gradleVersion),
        ) {

            configureCommonizerTargets()

            build(":commonize") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
            }

            build(":commonize") {
                assertNativeDistributionCommonizationCacheHit()
                assertTasksUpToDate(":cinteropCurlTargetA")
                assertTasksUpToDate(":cinteropCurlTargetB")
                assertTasksUpToDate(":commonizeCInterop")
            }

            buildGradleKts.replaceText("curl", "curl2")
            build(":commonize") {
                assertNativeDistributionCommonizationCacheHit()
                assertTasksExecuted(":cinteropCurl2TargetA")
                assertTasksExecuted(":cinteropCurl2TargetB")
                assertTasksExecuted(":commonizeCInterop")
            }

            buildGradleKts.modify {
                it.lineSequence().filter { "curl" !in it }.joinToString("\n")
            }
            build(":commonize") {
                assertNativeDistributionCommonizationCacheHit()
                assertTasksAreNotInTaskGraph(
                    ":cinteropCurlTargetA",
                    ":cinteropCurlTargetB",
                )
            }
        }
    }

    @DisplayName("Commonize Curl Interop feature flag")
    @GradleTest
    fun testCommonizeCurlInteropFeatureFlag(gradleVersion: GradleVersion) {
        nativeProject("commonizeCurlInterop", gradleVersion) {

            configureCommonizerTargets()

            // Remove feature flag from gradle.properties
            gradleProperties.modify {
                it.lineSequence().filter { "enableCInteropCommonization" !in it }.joinToString("\n")
            }

            build(":commonize") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksAreNotInTaskGraph(
                    ":cinteropCurlTargetA",
                    ":cinteropCurlTargetB",
                    ":commonizeCInterop",
                )
            }

            build(":commonize", "-Pkotlin.mpp.enableCInteropCommonization=true") {
                assertNativeDistributionCommonizationCacheHit()
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
            }

            build(":commonize", "-Pkotlin.mpp.enableCInteropCommonization=false") {
                assertNativeDistributionCommonizationCacheHit()
                assertTasksAreNotInTaskGraph(
                    ":cinteropCurlTargetA",
                    ":cinteropCurlTargetB",
                    ":commonizeCInterop",
                )
            }
        }
    }

    @DisplayName("Commonize Curl Interop copy CommonizeCInterop for Ide")
    @GradleTest
    fun testCommonizeCurlInteropcopyCommonizeCInteropForIde(
        gradleVersion: GradleVersion,
    ) {
        nativeProject("commonizeCurlInterop", gradleVersion) {

            configureCommonizerTargets()

            val commonizerIdeOutput: Path = projectPersistentCache.resolve("metadata").resolve("commonizer")

            val expectedOutputDirectoryForBuild = projectPath.resolve("build/classes/kotlin/commonizer")

            build(":copyCommonizeCInteropForIde") {
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")

                assertDirectoryExists(commonizerIdeOutput, "Missing output directory for IDE")
                assertDirectoryExists(expectedOutputDirectoryForBuild, "Missing output directory for build")
                assertEqualDirectories(expectedOutputDirectoryForBuild.toFile(), commonizerIdeOutput.toFile(), false)
            }

            build(":clean") {
                assertDirectoryExists(commonizerIdeOutput, "Expected ide output directory to survive cleaning")
                assertFileNotExists(expectedOutputDirectoryForBuild, "Expected output directory for build to be cleaned")
            }
        }
    }

    @DisplayName("Commonize Curl Interop compilation")
    @GradleTest
    fun testCommonizeCurlInteropCopyCompilation(gradleVersion: GradleVersion) {
        nativeProject("commonizeCurlInterop", gradleVersion) {

            configureCommonizerTargets()

            build(":compileNativeMainKotlinMetadata") {
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
            }

            // targetA will be macos
            build(":compileTestKotlinTargetA")

            //targetB will be linuxArm64
            build(":compileTestKotlinTargetB")
        }
    }

    @DisplayName("Commonize SQL Lite Interop")
    @GradleTest
    fun testCommonizeSQLiteInterop(gradleVersion: GradleVersion) {
        nativeProject("commonizeSQLiteInterop", gradleVersion) {

            configureCommonizerTargets()

            build(":commonize") {
                assertTasksExecuted(":cinteropSqliteTargetA")
                assertTasksExecuted(":cinteropSqliteTargetB")
                assertTasksExecuted(":commonizeCInterop")
            }
        }
    }

    @DisplayName("Commonize SQL Lite and Curl Interop")
    @GradleTest
    fun testCommonizeSQLiteAndCurlInterop(gradleVersion: GradleVersion) {
        nativeProject("commonizeSQLiteAndCurlInterop", gradleVersion) {

            configureCommonizerTargets()

            build(":commonize") {
                assertTasksExecuted(":cinteropSqliteTargetA")
                assertTasksExecuted(":cinteropSqliteTargetB")
                assertTasksExecuted(":cinteropCurlTargetA")
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")
            }

            build(":compileNativeMainKotlinMetadata") {
                assertTasksUpToDate(":cinteropSqliteTargetA")
                assertTasksUpToDate(":cinteropSqliteTargetB")
                assertTasksUpToDate(":cinteropCurlTargetA")
                assertTasksUpToDate(":cinteropCurlTargetB")
                assertTasksUpToDate(":commonizeCInterop")
            }
        }
    }

    @DisplayName("Commonize Interop using posix APIs")
    @GradleTest
    fun testCommonizeInteropUsingPosixAPIs(gradleVersion: GradleVersion) {
        nativeProject("commonizeInteropUsingPosixApis", gradleVersion) {

            configureCommonizerTargets()

            build(":commonizeCInterop") {
                assertTasksExecuted(":cinteropWithPosixTargetA")
                assertTasksExecuted(":cinteropWithPosixTargetB")
                assertTasksExecuted(":commonizeNativeDistribution")
                assertTasksExecuted(":commonizeCInterop")
            }

            build(":compileNativeMainKotlinMetadata") {
                assertTasksUpToDate(":cinteropWithPosixTargetA")
                assertTasksUpToDate(":cinteropWithPosixTargetB")
                assertNativeDistributionCommonizationCacheHit()
                assertTasksUpToDate(":commonizeCInterop")
            }
        }
    }

    @DisplayName("KT-46234 intermediate source set with only one native target")
    @TestMetadata("commonize-kt-46234-singleNativeTarget")
    @GradleTest
    fun testIntermediateSourceSetWithOnlyOneNativeTarget(gradleVersion: GradleVersion) {
        testSingleNativePlatform("commonize-kt-46234-singleNativeTarget", gradleVersion)
    }

    @DisplayName("KT-46142 standalone native source set")
    @TestMetadata("commonize-kt-46142-singleNativeTarget")
    @GradleTest
    fun testStandaloneNativeSourceSet(gradleVersion: GradleVersion) {
        testSingleNativePlatform("commonize-kt-46142-singleNativeTarget", gradleVersion)
    }

    @DisplayName("KT-46248 single supported native target dependency propagation")
    @TestMetadata("commonize-kt-46248-singleNativeTargetPropagation")
    @GradleTest
    fun testSingleSupportedNativeTargetDependencyPropagation(gradleVersion: GradleVersion) {
        val posixDependencyRegex = Regex(""".*Dependency:.*[pP]osix""")
        val dummyCInteropDependencyRegex = Regex(""".*Dependency:.*cinterop-dummy.*""")
        nativeProject(
            "commonize-kt-46248-singleNativeTargetPropagation",
            gradleVersion,
            buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()
        ) {
            build(":p1:listNativeMainDependencies") {
                assertOutputContains(posixDependencyRegex)
                assertOutputContains(dummyCInteropDependencyRegex)
            }

            build(":p1:listNativeMainParentDependencies") {
                assertOutputContains(posixDependencyRegex)
                assertOutputContains(dummyCInteropDependencyRegex)
            }

            build(":p1:listCommonMainDependencies") {
                assertOutputDoesNotContain(posixDependencyRegex)
                assertOutputDoesNotContain(dummyCInteropDependencyRegex)
            }

            build("assemble") {
                assertTasksExecuted(":p1:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":p1:compileKotlinNativePlatform")
            }
        }
    }

    @DisplayName("KT-46248 single supported native target dependency propagation - cinterop")
    @TestMetadata("commonize-kt-47523-singleNativeTargetPropagation-cinterop")
    @GradleTest
    fun testSingleSupportedNativeTargetDependencyPropagationCInterop(gradleVersion: GradleVersion) {
        val nativeMainContainsCInteropDependencyRegex = Regex(""".*Dependency:.*cinterop-nativePlatformCinterop-dummy.*""")
        nativeProject("commonize-kt-47523-singleNativeTargetPropagation-cinterop", gradleVersion) {
            build("listNativePlatformMainDependencies") {
                assertOutputDoesNotContain(nativeMainContainsCInteropDependencyRegex)
            }

            build("listNativeMainDependencies") {
                assertOutputContains(nativeMainContainsCInteropDependencyRegex)
            }

            build("listCommonMainDependencies") {
                assertOutputContains(nativeMainContainsCInteropDependencyRegex)
            }

            build("assemble")
        }
    }

    @DisplayName("KT-48856 single native target dependency propagation - test source set - cinterop")
    @GradleTest
    fun testSingleSupportedNativeTargetDependencyPropagationTestSourceSetCInterop(gradleVersion: GradleVersion) {
        val nativeMainContainsCInteropDependencyRegex = Regex(""".*Dependency:.*platformCinterop-sampleInterop.*""")
        nativeProject("commonize-kt-48856-singleNativeTargetPropagation-testSourceSet", gradleVersion) {
            build("listNativeTestDependencies") {
                assertOutputContains(
                    nativeMainContainsCInteropDependencyRegex,
                    "Expected sourceSet 'nativeTest' to list cinterop dependency"
                )
            }

            build("assemble")
        }
    }

    @DisplayName("KT-46856 filename too long - all native targets configured")
    @GradleTest
    fun testFilenameTooLongAllNativeTargetsConfigured(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-46856-all-targets", gradleVersion) {
            build(":commonize")
        }
    }

    @DisplayName("Multiple cinterops with test source sets and compilations - test source sets depending on main")
    @GradleTest
    fun testMultipleCinteropsWithTestSourceSetsAndCompilationsTestSourceSetsDependingOnMain(gradleVersion: GradleVersion) {
        `test multiple cinterops with test source sets and compilations`(gradleVersion, true)
    }

    @DisplayName("Multiple cinterops with test source sets and compilations")
    @GradleTest
    fun testMultipleCinteropsWithTestSourceSetsAndCompilations(gradleVersion: GradleVersion) {
        `test multiple cinterops with test source sets and compilations`(gradleVersion, false)
    }

    @DisplayName("KT-49735 two kotlin targets with same konanTarget")
    @GradleTest
    fun testTwoKotlinTargetsWithSameKonanTarget(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-49735-twoKotlinTargets-oneKonanTarget", gradleVersion) {
            build(":assemble", "-Pkotlin.internal.suppressGradlePluginErrors=KotlinTargetAlreadyDeclaredError") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
            }
        }
    }

    @DisplayName("KT-48118 c-interops available in commonMain")
    @GradleTest
    fun testCInteropsAvailableInCommonMain(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-48118-c-interop-in-common-main", gradleVersion) {
            reportSourceSetCommonizerDependencies {
                val upperMain = getCommonizerDependencies("upperMain")
                val konanDataDirProperty = buildOptions.konanDataDir
                    ?: error("konanDataDir must not be null in this test. Please set a custom konanDataDir property.")
                upperMain.withoutNativeDistributionDependencies(konanDataDirProperty).assertDependencyFilesMatches(".*cinterop-dummy")
                upperMain.onlyNativeDistributionDependencies(konanDataDirProperty).assertNotEmpty()

                val commonMain = getCommonizerDependencies("commonMain")
                commonMain.withoutNativeDistributionDependencies(konanDataDirProperty).assertDependencyFilesMatches(".*cinterop-dummy")
                commonMain.onlyNativeDistributionDependencies(konanDataDirProperty).assertNotEmpty()
            }

            build(":compileCommonMainKotlinMetadata")

            build(":compileUpperMainKotlinMetadata")
        }
    }

    @DisplayName("KT-47641 commonizing c-interops does not depend on any source compilation")
    @GradleTest
    fun testCInteropsDoesNotDependOnAnySourceCompilation(gradleVersion: GradleVersion) {
        nativeProject(
            "commonize-kt-47641-cinterops-compilation-dependency",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)
        ) {
            build("commonizeCInterop") {
                assertTasksExecuted(":p1:commonizeCInterop")
                assertTasksExecuted(":p2:commonizeCInterop")

                assertAnyTaskHasBeenExecuted(findTasksByPattern(":p1:cinteropTestWithPosix.*".toRegex()))
                assertAnyTaskHasBeenExecuted(findTasksByPattern(":p2:cinteropTestWithPosix.*".toRegex()))
                assertAnyTaskHasBeenExecuted(findTasksByPattern(":p2:cinteropTestWithPosixP2.*".toRegex()))

                /* Make sure that we correctly reference any compile tasks in this test (test is useless otherwise) */
                assertOutputContains("Register task :p1.*compile.*".toRegex())
                assertOutputContains("Register task :p2.*compile.*".toRegex())

                /* CInterops *shall not* require any compilation */
                assertOutputDoesNotContain("(Executing actions for task|Executing task) ':p0.*compile.*'".toRegex())
                assertOutputDoesNotContain("(Executing actions for task|Executing task) ':p1.*compile.*'".toRegex())
                assertOutputDoesNotContain("(Executing actions for task|Executing task) ':p2.*compile.*'".toRegex())
            }
        }
    }

    @DisplayName("KT-48138 commonizing c-interops when nativeTest and nativeMain have different targets")
    @GradleTest
    fun testCommonizingCInteropsWhenNativeTestAndNativeMainHaveDifferentTargets(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-48138-nativeMain-nativeTest-different-targets", gradleVersion) {
            reportSourceSetCommonizerDependencies {
                val nativeMain = getCommonizerDependencies("nativeMain")
                val konanDataDirProperty = buildOptions.konanDataDir
                    ?: error("konanDataDir must not be null in this test. Please set a custom konanDataDir property.")
                nativeMain.withoutNativeDistributionDependencies(konanDataDirProperty).assertDependencyFilesMatches(".*cinterop-dummy")
                nativeMain.onlyNativeDistributionDependencies(konanDataDirProperty).assertNotEmpty()
                nativeMain.assertTargetOnAllDependencies(CommonizerTarget(LINUX_X64, LINUX_ARM64, MINGW_X64))

                val nativeTest = getCommonizerDependencies("nativeTest")
                nativeTest.onlyNativeDistributionDependencies(konanDataDirProperty).assertNotEmpty()
                nativeTest.withoutNativeDistributionDependencies(konanDataDirProperty).assertDependencyFilesMatches(".*cinterop-dummy")
                nativeTest.assertTargetOnAllDependencies(CommonizerTarget(LINUX_X64, LINUX_ARM64))
            }
        }
    }

    @DisplayName("KT-50847 missing cinterop in supported target")
    @GradleTest
    fun testMissingCinteropInSupportedTarget(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-50847-cinterop-missing-in-supported-target", gradleVersion) {
            build(":compileCommonMainKotlinMetadata", "-PdisableTargetNumber=1") {
                assertTasksSkipped(":cinteropSimpleTarget1")
                assertTasksExecuted(":cinteropSimpleTarget2")
                assertTasksExecuted(":commonizeCInterop")
            }

            build(":compileCommonMainKotlinMetadata", "-PdisableTargetNumber=2") {
                assertTasksSkipped(":cinteropSimpleTarget2")
                assertTasksExecuted(":cinteropSimpleTarget1")
                assertTasksExecuted(":commonizeCInterop")
            }
        }
    }

    @DisplayName("KT-51517 commonization with transitive cinterop")
    @GradleTest
    fun testCommonizationWithTransitiveCinterop(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-51517-transitive-cinterop", gradleVersion) {
            build(":app:assemble") {
                assertTasksExecuted(":lib:transformCommonMainCInteropDependenciesMetadata")
                assertTasksExecuted(":lib:commonizeCInterop")
                assertTasksExecuted(":lib:compileCommonMainKotlinMetadata")
                assertTasksExecuted(":app:commonizeCInterop")
                assertTasksExecuted(":app:compileNativeMainKotlinMetadata")
            }
        }
    }

    @DisplayName("KT-57796 commonization with two cinterop commonizer groups`")
    @GradleTest
    fun testCommonizationWithTwoCInteropCommonizerGroups(gradleVersion: GradleVersion) {
        nativeProject("commonize-kt-57796-twoCInteropCommonizerGroups", gradleVersion) {
            build(":app:commonizeCInterop") {
                assertTasksExecuted(
                    ":lib:commonizeCInterop",
                    ":app:commonizeCInterop",
                )
                assertTasksAreNotInTaskGraph(
                    ":lib:transformCommonMainCInteropDependenciesMetadata",
                    ":app:transformCommonMainCInteropDependenciesMetadata",
                )
            }
        }
    }

    @DisplayName("KT-57796 commonization with two cinterop commonizer groups`")
    @GradleTest
    fun testCommonizationWithLibraryContainingTwoRoots(gradleVersion: GradleVersion) {
        project(
            "commonize-kt-56729-consume-library-with-two-roots",
            gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion)
        ) {
            build("publish")

            build(":consumer:assemble") {
                assertTasksExecuted(":consumer:compileCommonMainKotlinMetadata")
                assertOutputDoesNotContain("Duplicated libraries:")
                assertOutputDoesNotContain("w: duplicate library name")
            }
        }
    }

    @DisplayName("KT-58223 test commonized libraries for IDE can be stored in different data dir")
    @GradleTest
    fun testCommonizedLibrariesForIDECanBeStoredInDifferentDir(
        gradleVersion: GradleVersion,
    ) {
        nativeProject("commonizeCurlInterop", gradleVersion) {
            configureCommonizerTargets()

            val commonizerIdeOutput: Path = projectPersistentCache.resolve("metadata").resolve("commonizer")

            build(":copyCommonizeCInteropForIde") {
                assertTasksExecuted(":cinteropCurlTargetB")
                assertTasksExecuted(":commonizeCInterop")

                assertDirectoryExists(commonizerIdeOutput, "Missing output directory for IDE")
            }

            build(":clean") {
                assertDirectoryExists(commonizerIdeOutput, "Expected ide output directory to survive cleaning")
            }
        }
    }

    @DisplayName("KT-74403 test that commonization of unsupported targets should not fail")
    @OsCondition(supportedOn = [OS.LINUX], enabledOnCI = [OS.LINUX]) // Mac supports all known targets
    @GradleTest
    fun testCommonizationOfUnsupportedTargetsShouldNotFail(gradleVersion: GradleVersion, @TempDir konanDataDir: Path) {

        nativeProject("emptyKts", gradleVersion, defaultBuildOptions.copy(konanDataDir = konanDataDir)) {
            addKgpToBuildScriptCompilationClasspath()

            val app = project("emptyKts", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        macosX64()
                        macosArm64()
                    }
                }
            }
            val lib = project("emptyKts", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        macosX64()
                        macosArm64()
                    }
                }
            }

            include(app, "app")
            include(lib, "lib")

            build(
                ":app:commonizeNativeDistribution",
                ":lib:commonizeNativeDistribution",
            ) {
                assertTasksExecuted(":app:commonizeNativeDistribution")
                assertTasksExecuted(":lib:commonizeNativeDistribution")
            }
        }
    }

    @DisplayName("KT-74442: commonization should work for all known targets despite `kotlin.native.enableKlibsCrossCompilation` state")
    @TestMetadata("emptyKts")
    @GradleTest
    fun testCommonizationWorksForAllTargetsDespiteCrossCompilationFlagState(gradleVersion: GradleVersion) {
        nativeProject("emptyKts", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    linuxArm64()
                    linuxX64()
                    macosArm64()
                    macosX64()
                }
            }

            kotlinSourcesDir("nativeMain").createDirectories().resolve("OnlyLinuxClass.kt").writeText(
                //language=kotlin
                """
                |fun main() {
                |    platform.linux.epoll_create(0)
                |}
                """.trimMargin()
            )

            buildAndFail(
                ":compileNativeMainKotlinMetadata",
                buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()
            ) {
                assertTasksFailed(":compileNativeMainKotlinMetadata")
                assertOutputContains("Unresolved reference 'linux'")
            }

            buildAndFail(":compileNativeMainKotlinMetadata") {
                assertTasksFailed(":compileNativeMainKotlinMetadata")
                assertOutputContains("Unresolved reference 'linux'")
            }
        }
    }

    @DisplayName("KT-74442: commonization of non-platform CInterop should work only for suppoted targets")
    @TestMetadata("emptyKts")
    @OsCondition(enabledOnCI = [OS.LINUX, OS.WINDOWS, OS.MAC])
    @GradleTest
    fun testCommonizationOfNonPlatformShouldWorkOnlyForSupportedTargets(gradleVersion: GradleVersion) {
        nativeProject("emptyKts", gradleVersion) {
            addKgpToBuildScriptCompilationClasspath()
            buildScriptInjection {
                project.applyMultiplatform {
                    fun KotlinNativeTarget.addCInterop(name: String = "nonplatform") {
                        compilations.getByName("main") {
                            it.cinterops.create(name)
                        }
                    }
                    linuxX64().addCInterop()
                    linuxArm64().addCInterop()
                    macosX64().addCInterop()
                    macosArm64().addCInterop()
                }
            }

            gradleProperties.appendText("\nkotlin.mpp.enableCInteropCommonization=true")

            projectPath.resolve("src/nativeInterop/cinterop").createDirectories().resolve("nonplatform.def").writeText(
                """
                |---
                |
                |#ifdef __linux
                |    void linux_only_api(void) {}
                |#endif
                |
                |#ifdef __APPLE__
                |    void apply_only_api(void) {}
                |#endif
                |
                |void sqlite_optimistically_commonized_api(void) {}    
                """.trimMargin()
            )

            if (HostManager.hostIsMac) {
                build(
                    "commonizeCInterop", buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()
                ) {
                    checkCommonizedMetadataBuildOnMac()
                }
                build("commonizeCInterop") {
                    checkCommonizedMetadataBuildOnMac()
                }
            } else {
                build(
                    "commonizeCInterop", buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()
                ) {
                    checkCommonizedMetadataBuildOnNonMac()
                }

                build("commonizeCInterop") {
                    checkCommonizedMetadataBuildOnNonMac()
                }
            }
        }
    }

    private fun TestProject.checkCommonizedMetadataBuildOnNonMac() {
        assertEquals(
            """
            |nonplatform/linux_only_api|linux_only_api(){}[1]
            |nonplatform/sqlite_optimistically_commonized_api|sqlite_optimistically_commonized_api(){}[1]
            |
            """.trimMargin(),
            dumpKlibMetadataSignatures(
                nonPlatformCinteropsClasspath("linuxMain").single(),
            )
        )

        // In non-platform cinterops we currently commonize APIs optimistically,
        // and we did so in platform cinterops before 2.1.0,
        // because before then we didn't even have platform klibs for Apple targets in non-Apple Kotlin Native distributions (see KT-74442).
        // Now we commonize platform cinterops correctly, and we continue commonizing non-platform cinterops optimistically.
        // So, it is expected to not fail here because non-platform cinterop now works only for supported targets.
        // The behavior of these checks must change when KT-74073 is done.
        assertEquals(
            """
            |nonplatform/linux_only_api|linux_only_api(){}[1]
            |nonplatform/sqlite_optimistically_commonized_api|sqlite_optimistically_commonized_api(){}[1]
            |
            """.trimMargin(),
            dumpKlibMetadataSignatures(
                nonPlatformCinteropsClasspath("nativeMain").single()
            )
        )
    }

    private fun TestProject.nonPlatformCinteropsClasspath(sourceSetName: String): Set<java.io.File> {
        return buildScriptReturn {
            project.future {
                val cinteropCommonizerTask = project.commonizeCInteropTask()
                cinteropCommonizerTask!!.get().commonizedOutputLibraries(
                    CInteropCommonizerDependent.from(
                        kotlinMultiplatform.sourceSets.getByName(sourceSetName)
                    )!!
                ).files
            }.getOrThrow()
        }.buildAndReturn()
    }

    private fun TestProject.checkCommonizedMetadataBuildOnMac() {
        assertEquals(
            """
            |nonplatform/linux_only_api|linux_only_api(){}[1]
            |nonplatform/sqlite_optimistically_commonized_api|sqlite_optimistically_commonized_api(){}[1]
            |
            """.trimMargin(),
            dumpKlibMetadataSignatures(
                nonPlatformCinteropsClasspath("linuxMain").single(),
            )
        )
        assertEquals(
            """
            |nonplatform/apply_only_api|apply_only_api(){}[1]
            |nonplatform/sqlite_optimistically_commonized_api|sqlite_optimistically_commonized_api(){}[1]
            |
            """.trimMargin(),
            dumpKlibMetadataSignatures(
                nonPlatformCinteropsClasspath("appleMain").single(),
            )
        )
        assertEquals(
            """
            |nonplatform/sqlite_optimistically_commonized_api|sqlite_optimistically_commonized_api(){}[1]
            |
            """.trimMargin(),
            dumpKlibMetadataSignatures(
                nonPlatformCinteropsClasspath("nativeMain").single(),
            )
        )
    }

    private fun `test multiple cinterops with test source sets and compilations`(
        gradleVersion: GradleVersion,
        testSourceSetsDependingOnMain: Boolean,
    ) {
        val buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()

        nativeProject(
            "commonizeMultipleCInteropsWithTests",
            gradleVersion,
            buildOptions = buildOptions
        ) {

            val isMac = HostManager.hostIsMac

            fun BuildResult.assertTestSourceSetsDependingOnMainParameter() {
                val message = "testSourceSetsDependingOnMain is set"
                if (testSourceSetsDependingOnMain) assertOutputContains(message) else assertOutputDoesNotContain(message)
            }

            val testSourceSetsDependingOnMainParameterOption = defaultBuildOptions.copy(
                freeArgs = listOf("-PtestSourceSetsDependingOnMain=$testSourceSetsDependingOnMain"),
                nativeOptions = defaultBuildOptions.nativeOptions.copy(
                    disableKlibsCrossCompilation = true
                )
            )

            reportSourceSetCommonizerDependencies(options = testSourceSetsDependingOnMainParameterOption) {
                it.assertTestSourceSetsDependingOnMainParameter()

                /* this source sets are also shared with a jvm target */
                getCommonizerDependencies("commonMain").assertEmpty()
                getCommonizerDependencies("commonTest").assertEmpty()

                val konanDataDirProperty = buildOptions.konanDataDir
                    ?: error("konanDataDir must not be null in this test. Please set a custom konanDataDir property.")
                getCommonizerDependencies("nativeMain").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                    assertDependencyFilesMatches(".*nativeHelper")
                    assertTargetOnAllDependencies(
                        CommonizerTarget(IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64, LINUX_X64, LINUX_ARM64, MACOS_X64, MINGW_X64)
                    )
                }

                getCommonizerDependencies("nativeTest").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                    assertDependencyFilesMatches(".*nativeHelper", ".*nativeTestHelper")
                    assertTargetOnAllDependencies(
                        CommonizerTarget(IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64, LINUX_X64, LINUX_ARM64, MACOS_X64, MINGW_X64)
                    )
                }

                getCommonizerDependencies("unixMain").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                    assertDependencyFilesMatches(".*nativeHelper", ".*unixHelper")
                    assertTargetOnAllDependencies(
                        CommonizerTarget(
                            IOS_X64,
                            IOS_ARM64,
                            IOS_SIMULATOR_ARM64,
                            LINUX_X64,
                            LINUX_ARM64,
                            MACOS_X64
                        )
                    )
                }

                getCommonizerDependencies("unixTest").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                    assertDependencyFilesMatches(".*nativeHelper", ".*unixHelper", ".*nativeTestHelper")
                    assertTargetOnAllDependencies(
                        CommonizerTarget(
                            IOS_X64,
                            IOS_ARM64,
                            IOS_SIMULATOR_ARM64,
                            LINUX_X64,
                            LINUX_ARM64,
                            MACOS_X64
                        )
                    )
                }

                getCommonizerDependencies("linuxMain").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                    assertDependencyFilesMatches(".*nativeHelper", ".*unixHelper")
                    assertTargetOnAllDependencies(CommonizerTarget(LINUX_X64, LINUX_ARM64))
                }

                getCommonizerDependencies("linuxTest").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                    assertDependencyFilesMatches(".*nativeHelper", ".*unixHelper", ".*nativeTestHelper")
                    assertTargetOnAllDependencies(CommonizerTarget(LINUX_X64, LINUX_ARM64))
                }

                getCommonizerDependencies("linuxX64Main").assertEmpty()
                getCommonizerDependencies("linuxArm64Main").assertEmpty()
                getCommonizerDependencies("linuxX64Test").assertEmpty()
                getCommonizerDependencies("linuxArm64Test").assertEmpty()

                if (isMac) {
                    getCommonizerDependencies("appleMain").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                        assertDependencyFilesMatches(".*nativeHelper", ".*unixHelper", ".*appleHelper")
                        assertTargetOnAllDependencies(CommonizerTarget(IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64, MACOS_X64))
                    }

                    getCommonizerDependencies("appleTest").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                        assertDependencyFilesMatches(".*nativeHelper", ".*unixHelper", ".*appleHelper", ".*nativeTestHelper")
                        assertTargetOnAllDependencies(CommonizerTarget(IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64, MACOS_X64))
                    }

                    getCommonizerDependencies("iosMain").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                        assertDependencyFilesMatches(".*nativeHelper", ".*unixHelper", ".*appleHelper")
                        assertTargetOnAllDependencies(CommonizerTarget(IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64))
                    }

                    getCommonizerDependencies("iosTest").withoutNativeDistributionDependencies(konanDataDirProperty).apply {
                        assertDependencyFilesMatches(
                            ".*nativeHelper", ".*unixHelper", ".*appleHelper", ".*nativeTestHelper", ".*iosTestHelper"
                        )
                        assertTargetOnAllDependencies(CommonizerTarget(IOS_X64, IOS_ARM64, IOS_SIMULATOR_ARM64))
                    }

                    getCommonizerDependencies("macosMain").assertEmpty()
                    getCommonizerDependencies("macosTest").assertEmpty()
                    getCommonizerDependencies("iosX64Main").assertEmpty()
                    getCommonizerDependencies("iosX64Test").assertEmpty()
                    getCommonizerDependencies("iosArm64Main").assertEmpty()
                    getCommonizerDependencies("iosArm64Test").assertEmpty()
                    getCommonizerDependencies("iosSimulatorArm64Main").assertEmpty()
                    getCommonizerDependencies("iosSimulatorArm64Test").assertEmpty()
                }

                getCommonizerDependencies("windowsX64Main").assertEmpty()
                getCommonizerDependencies("windowsX64Test").assertEmpty()
            }

            build(":assemble", buildOptions = testSourceSetsDependingOnMainParameterOption) {
                assertTestSourceSetsDependingOnMainParameter()
                assertTasksUpToDate(":commonizeNativeDistribution")
                assertOutputContains("Native Distribution Commonization: Cache hit")
                assertTasksUpToDate(":commonizeCInterop")
            }

            build(":compileNativeMainKotlinMetadata", buildOptions = testSourceSetsDependingOnMainParameterOption) {
                assertTestSourceSetsDependingOnMainParameter()
            }

            build(":compileUnixMainKotlinMetadata", buildOptions = testSourceSetsDependingOnMainParameterOption) {
                assertTestSourceSetsDependingOnMainParameter()
            }

            build(":compileLinuxMainKotlinMetadata", buildOptions = testSourceSetsDependingOnMainParameterOption) {
                assertTestSourceSetsDependingOnMainParameter()
            }

            if (isMac) {
                build(":compileAppleMainKotlinMetadata", buildOptions = testSourceSetsDependingOnMainParameterOption) {
                    assertTestSourceSetsDependingOnMainParameter()
                }

                build(":compileIosMainKotlinMetadata", buildOptions = testSourceSetsDependingOnMainParameterOption) {
                    assertTestSourceSetsDependingOnMainParameter()
                }
            }
        }
    }

    private fun testSingleNativePlatform(projectName: String, gradleVersion: GradleVersion) {

        val posixInIntransitiveMetadataConfigurationRegex = Regex(""".*intransitiveMetadataConfiguration:.*([pP])osix""")

        nativeProject(projectName, gradleVersion) {
            build(":p1:listNativePlatformMainDependencies", "-Pkotlin.mpp.enableIntransitiveMetadataConfiguration=false") {
                assertOutputDoesNotContain(posixInIntransitiveMetadataConfigurationRegex)
            }

            build(":p1:listNativePlatformMainDependencies", "-Pkotlin.mpp.enableIntransitiveMetadataConfiguration=true") {
                assertOutputContains(posixInIntransitiveMetadataConfigurationRegex)
            }

            build("assemble")
        }

    }

    private fun TestProject.configureCommonizerTargets() {
        buildGradleKts.replaceText("<targetA>", CommonizableTargets.targetA.value)
        buildGradleKts.replaceText("<targetB>", CommonizableTargets.targetB.value)
    }

    private fun BuildResult.assertNativeDistributionCommonizationCacheHit() {
        assertOutputContains("Native Distribution Commonization: Cache hit")
    }
}

private data class TargetSubstitution(val value: String) {
    override fun toString(): String = value
}

private object CommonizableTargets {
    private val os = OperatingSystem.current()

    val targetA = when {
        os.isMacOsX -> TargetSubstitution("macosX64")
        os.isLinux -> TargetSubstitution("linuxX64")
        os.isWindows -> TargetSubstitution("mingwX64")
        else -> fail("Unsupported os: ${os.name}")
    }

    val targetB = when {
        os.isMacOsX -> TargetSubstitution("linuxX64")
        os.isLinux -> TargetSubstitution("linuxArm64")
        os.isWindows -> TargetSubstitution("linuxX64")
        else -> fail("Unsupported os: ${os.name}")
    }
}
