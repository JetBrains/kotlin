/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import com.intellij.testFramework.TestDataFile
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jdom.input.SAXBuilder
import org.jetbrains.kotlin.gradle.internals.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@DisplayName("Tests for general K/N builds")
@NativeGradlePluginTests
class GeneralNativeIT : KGPBaseTest() {

    private val nativeHostTargetName = MPPNativeTargets.current

    @DisplayName("K/N compiler can be started in-process in parallel")
    @GradleTest
    fun shouldCheckKNativeCompilerStartedInParallel(gradleVersion: GradleVersion) {
        nativeProject("native-parallel", gradleVersion) {
            val synchronisationBlock =
                """
                tasks.getByPath("compileKotlinLinux").doFirst {
                    val countDownLatch = rootProject.extra["countDownLatch"] as CountDownLatch
                    countDownLatch.countDown()
                    countDownLatch.await()
                }
                """.trimIndent()
            buildGradleKts.appendText(
                """
                val countDownLatch by extra(CountDownLatch(2))
                """.trimIndent()
            )
            // we are adding synchronisation before compileKotlinLinux execution in each subproject for making
            // at least the execution of these tasks parallel
            subProject("one").buildGradleKts.appendText(synchronisationBlock)
            subProject("two").buildGradleKts.appendText(synchronisationBlock)

            build(":one:compileKotlinLinux", ":two:compileKotlinLinux")
        }
    }

    @DisplayName("Build with ignoreIncorrectDependencies turned on")
    @GradleTest
    fun testIncorrectDependenciesWarning(gradleVersion: GradleVersion) {
        nativeProject("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            buildGradle.replaceText(
                "api 'org.jetbrains.kotlin:kotlin-stdlib-common'",
                "compileOnly 'org.jetbrains.kotlin:kotlin-stdlib-common'"
            )

            build {
                assertOutputContains("A compileOnly dependency is used in the Kotlin/Native target")
            }
            build("-Pkotlin.native.ignoreIncorrectDependencies=true") {
                assertOutputDoesNotContain("A compileOnly dependency is used in the Kotlin/Native target")
            }
        }
    }

    @DisplayName("Can produce native libraries")
    @GradleTest
    fun testCanProduceNativeLibraries(gradleVersion: GradleVersion) {
        nativeProject(
            "native-binaries/libraries",
            gradleVersion,
            configureSubProjects = true
        ) {
            val baseName = "native_library"

            val sharedPrefix = CompilerOutputKind.DYNAMIC.prefix(HostManager.host)
            val sharedSuffix = CompilerOutputKind.DYNAMIC.suffix(HostManager.host)
            val sharedPaths = listOf(
                "build/bin/host/debugShared/$sharedPrefix$baseName$sharedSuffix",
                "build/bin/host/releaseShared/$sharedPrefix$baseName$sharedSuffix",
            )

            val staticPrefix = CompilerOutputKind.STATIC.prefix(HostManager.host)
            val staticSuffix = CompilerOutputKind.STATIC.suffix(HostManager.host)
            val staticPaths = listOf(
                "build/bin/host/debugStatic/$staticPrefix$baseName$staticSuffix",
                "build/bin/host/releaseStatic/$staticPrefix$baseName$staticSuffix",
            )

            val headerPaths = listOf(
                "build/bin/host/debugShared/$sharedPrefix${baseName}_api.h",
                "build/bin/host/debugStatic/$staticPrefix${baseName}_api.h",
                "build/bin/host/releaseShared/$sharedPrefix${baseName}_api.h",
                "build/bin/host/releaseStatic/$staticPrefix${baseName}_api.h",
            )

            val klibPrefix = CompilerOutputKind.LIBRARY.prefix(HostManager.host)
            val klibSuffix = CompilerOutputKind.LIBRARY.suffix(HostManager.host)
            val klibPath = "${kotlinClassesDir(targetName = "host")}${klibPrefix}/klib/native-library$klibSuffix"

            val linkTasks = listOf(
                ":linkDebugSharedHost",
                ":linkDebugStaticHost",
                ":linkReleaseSharedHost",
                ":linkReleaseStaticHost",
            )

            val klibTask = ":compileKotlinHost"

            build(":assemble") {
                assertTasksExecuted(linkTasks + klibTask)

                sharedPaths.forEach { assertFileInProjectExists(it) }
                staticPaths.forEach { assertFileInProjectExists(it) }
                headerPaths.forEach {
                    assertFileInProjectExists(it)
                    assertFileInProjectContains(it, "_KInt (*exported)();")
                }
                assertFileInProjectExists(klibPath)
            }

            // Test that all up-to date checks are correct
            build(":assemble") {
                assertTasksUpToDate(linkTasks)
                assertTasksUpToDate(klibTask)
            }

            // Remove header of one of libraries and check that it is rebuilt.
            assertTrue(projectPath.resolve(headerPaths[0]).deleteIfExists())
            build(":assemble") {
                assertTasksUpToDate(linkTasks.drop(1))
                assertTasksUpToDate(klibTask)
                assertTasksExecuted(linkTasks[0])
            }
        }
    }

    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("Can provide native framework artifact")
    @GradleTest
    fun testCanProvideNativeFrameworkArtifact(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/frameworks", gradleVersion = gradleVersion) {
            buildGradleKts.appendText(
                //language=kotlin
                """
                val frameworkTargets = Attribute.of(
                    "org.jetbrains.kotlin.native.framework.targets",
                    Set::class.java
                )
                val kotlinNativeBuildTypeAttribute = Attribute.of(
                    "org.jetbrains.kotlin.native.build.type",
                    String::class.java
                )
                     
                fun validateConfiguration(conf: Configuration, targets: Set<String>, expectedBuildType: String) {
                    if (conf.artifacts.files.count() != 1 || conf.artifacts.files.singleFile.name != "main.framework") {
                        throw IllegalStateException("No single artifact with proper name \"main.framework\"")
                    }
                    val confTargets = conf.attributes.getAttribute(frameworkTargets)!!
                    val buildType = conf.attributes.getAttribute(kotlinNativeBuildTypeAttribute)!!
                    if (confTargets.size != targets.size || !confTargets.containsAll(targets)) {
                        throw IllegalStateException("Framework has incorrect attributes. Expected targets: \"${'$'}targets\", actual: \"${'$'}confTargets\"")
                    }
                    if (buildType != expectedBuildType) {
                       throw IllegalStateException("Framework has incorrect attributes. Expected build type: \"${'$'}expectedBuildType\", actual: \"${'$'}buildType\"")
                    }
                }
                
                tasks.register("validateThinArtifacts") {
                    doLast {
                        val targets = listOf("ios" to "ios_arm64", "iosSim" to "ios_x64")
                        val buildTypes = listOf("release", "debug")
                        targets.forEach { (name, target) ->
                            buildTypes.forEach { buildType ->
                                val conf = project.configurations.getByName("main${'$'}{buildType.capitalize()}Framework${'$'}{name.capitalize()}")
                                validateConfiguration(conf, setOf(target), buildType.toUpperCase())
                            }
                        }
                    }
                }
                
                tasks.register("validateFatArtifacts") {
                    doLast {
                        val buildTypes = listOf("release", "debug")
                        buildTypes.forEach { buildType ->
                            val conf = project.configurations.getByName("main${'$'}{buildType.capitalize()}FrameworkIosFat")
                            validateConfiguration(conf, setOf("ios_x64", "ios_arm64"), buildType.toUpperCase())
                        }
                    }
                }
                
                tasks.register("validateCustomAttributesSetting") {
                    doLast {
                        val conf = project.configurations.getByName("customReleaseFrameworkIos")
                        val attr1Value = conf.attributes.getAttribute(disambiguation1Attribute)
                        if (attr1Value != "someValue") {
                            throw IllegalStateException("myDisambiguation1Attribute has incorrect value. Expected: \"someValue\", actual: \"${'$'}attr1Value\"")
                        }
                        val attr2Value = conf.attributes.getAttribute(disambiguation2Attribute)
                        if (attr2Value != "someValue2") {
                           throw IllegalStateException("myDisambiguation2Attribute has incorrect value. Expected: \"someValue2\", actual: \"${'$'}attr2Value\"")
                        }
                    }
                }
                """.trimIndent()
            )

            build(":validateThinArtifacts")
            build(":validateFatArtifacts")
            build(":validateCustomAttributesSetting")
        }
    }

    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("Can provide native framework")
    @GradleTest
    fun testCanProduceNativeFrameworks(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/frameworks", gradleVersion = gradleVersion) {
            fun assemble(assertions: BuildResult.() -> Unit) {
                build("assemble", assertions = assertions)
            }

            data class BinaryMeta(val name: String, val isStatic: Boolean = false)

            val frameworkPrefix = CompilerOutputKind.FRAMEWORK.prefix(HostManager.host)
            val frameworkSuffix = CompilerOutputKind.FRAMEWORK.suffix(HostManager.host)
            val targets = listOf("ios", "iosSim")
            val binaries = mapOf(
                "ios" to listOf(BinaryMeta("main"), BinaryMeta("custom", true)),
                "iosSim" to listOf(BinaryMeta("main"))
            )
            val frameworkPaths = targets.flatMap { target ->
                binaries.getValue(target).flatMap {
                    val list = listOf(
                        "build/bin/$target/${it.name}DebugFramework/$frameworkPrefix${it.name}$frameworkSuffix",
                        "build/bin/$target/${it.name}ReleaseFramework/$frameworkPrefix${it.name}$frameworkSuffix",
                    )
                    if (it.isStatic) {
                        list
                    } else {
                        list + "build/bin/$target/${it.name}DebugFramework/$frameworkPrefix${it.name}$frameworkSuffix.dSYM"
                    }
                }
            }

            val headerPaths = targets.flatMap { target ->
                binaries.getValue(target).flatMap {
                    listOf(
                        "build/bin/$target/${it.name}DebugFramework/$frameworkPrefix${it.name}$frameworkSuffix/headers/${it.name}.h",
                        "build/bin/$target/${it.name}ReleaseFramework/$frameworkPrefix${it.name}$frameworkSuffix/headers/${it.name}.h",
                    )
                }
            }

            val frameworkTasks = targets.flatMap { target ->
                binaries.getValue(target).flatMap {
                    listOf(
                        ":link${it.name.capitalize()}DebugFramework${target.capitalize()}",
                        ":link${it.name.capitalize()}ReleaseFramework${target.capitalize()}",
                    )
                }
            }

            // Check building
            // Check dependency exporting and bitcode embedding in frameworks.
            assemble {
                headerPaths.forEach { assertFileInProjectExists(it) }
                frameworkPaths.forEach { assertDirectoryInProjectExists(it) }

                assertFileInProjectContains(headerPaths[0], "+ (int32_t)exported")
                val xcodeMajorVersion = Xcode.findCurrent().version.major

                // Check that by default release frameworks have bitcode embedded.
                extractNativeTasksCommandLineArgumentsFromOutput(":linkMainReleaseFrameworkIos") {
                    if (xcodeMajorVersion < 14) {
                        assertCommandLineArgumentsContain("-Xembed-bitcode")
                    } else {
                        assertCommandLineArgumentsDoNotContain("-Xembed-bitcode")
                    }
                    assertCommandLineArgumentsContain("-opt")
                }
                // Check that by default debug frameworks have bitcode marker embedded.
                extractNativeTasksCommandLineArgumentsFromOutput(":linkMainDebugFrameworkIos") {
                    if (xcodeMajorVersion < 14) {
                        assertCommandLineArgumentsContain("-Xembed-bitcode-marker")
                    } else {
                        assertCommandLineArgumentsDoNotContain("-Xembed-bitcode-marker")
                    }
                    assertCommandLineArgumentsContain("-g")
                }
                // Check that bitcode can be disabled by setting custom compiler options
                extractNativeTasksCommandLineArgumentsFromOutput(":linkCustomDebugFrameworkIos") {
                    assertCommandLineArgumentsContainSequentially("-linker-option", "-L.")
                    assertCommandLineArgumentsContain(
                        "-Xtime",
                        "-Xstatic-framework",
                    )
                    assertCommandLineArgumentsDoNotContain(
                        "-Xembed-bitcode-marker",
                        "-Xembed-bitcode",
                    )
                }
                // Check that bitcode is disabled for iOS simulator.
                extractNativeTasksCommandLineArgumentsFromOutput(":linkMainReleaseFrameworkIosSim", ":linkMainDebugFrameworkIosSim") {
                    assertCommandLineArgumentsDoNotContain(
                        "-Xembed-bitcode-marker",
                        "-Xembed-bitcode"
                    )
                }
            }

            assemble {
                assertTasksUpToDate(frameworkTasks)
            }

            assertTrue(projectPath.resolve(headerPaths[0]).deleteIfExists())
            assemble {
                assertTasksUpToDate(frameworkTasks.drop(1))
                assertTasksExecuted(frameworkTasks[0])
            }
        }
    }

    @DisplayName("Checks exporting non api library")
    @GradleTest
    fun shouldFailOnExportingNonApiLibrary(gradleVersion: GradleVersion) {
        testExportApi(
            nativeProject("native-binaries/libraries", gradleVersion, configureSubProjects = true),
            listOf(
                ExportApiTestData("linkDebugSharedHost", "debugShared"),
                ExportApiTestData("linkDebugStaticHost", "debugStatic"),
            )
        )
    }

    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    @DisplayName("Checks exporting non api framework")
    @GradleTest
    fun testExportApiOnlyToFrameworks(gradleVersion: GradleVersion) {
        testExportApi(
            nativeProject("native-binaries/frameworks", gradleVersion),
            listOf(
                ExportApiTestData("linkMainDebugFrameworkIos", "mainDebugFramework")
            )
        )
    }

    private data class ExportApiTestData(val taskName: String, val binaryName: String)

    private fun testExportApi(project: TestProject, testData: List<ExportApiTestData>) = with(project) {
        // Check that plugin doesn't allow exporting dependencies not added in the API configuration.
        buildGradleKts.replaceText("api(project(\":exported\"))", "")

        fun failureMsgFor(binaryName: String) =
            "Following dependencies exported in the $binaryName binary are not specified as API-dependencies of a corresponding source set"

        testData.forEach {
            buildAndFail(it.taskName) {
                assertOutputContains(failureMsgFor(it.binaryName))
            }
        }
    }

    @DisplayName("Transitive export is not required for exporting variant")
    @GradleTest
    fun testTransitiveExportIsNotRequiredForExportingVariant(gradleVersion: GradleVersion) {
        project("native-binaries/export-published-lib", gradleVersion) {
            val headerPath = "shared/build/bin/linuxX64/debugStatic/libshared_api.h"

            build(":lib:publish")

            build(":shared:linkDebugStaticLinuxX64") {
                assertFileInProjectExists(headerPath)

                // Check that the function from exported published library (:lib) is included to the header:
                assertFileContains(projectPath.resolve(headerPath), "funInShared", "funToExport")
            }
        }
    }

    @DisplayName("Checking native executables")
    @GradleTest
    fun testNativeExecutables(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/executables", gradleVersion) {
            val binaries = mutableListOf(
                "debugExecutable" to "native-binary",
                "releaseExecutable" to "native-binary",
                "bazDebugExecutable" to "my-baz",
            )
            val linkTasks =
                binaries.map { (name, _) -> "link${name.capitalize()}Host" }
            val outputFiles = binaries.map { (name, fileBaseName) ->
                val outputKind = NativeOutputKind.values().single { name.endsWith(it.taskNameClassifier, true) }.compilerOutputKind
                val prefix = outputKind.prefix(HostManager.host)
                val suffix = outputKind.suffix(HostManager.host)
                val fileName = "$prefix$fileBaseName$suffix"
                name to "build/bin/host/$name/$fileName"
            }.toMap()
            val runTasks = listOf(
                "runDebugExecutable",
                "runReleaseExecutable",
                "runBazDebugExecutable",
            ).map { it + "Host" }.toMutableList()

            // Check building
            build("hostMainBinaries") {
                assertTasksExecuted(linkTasks.map { ":$it" })
                assertTasksExecuted(":compileKotlinHost")
                outputFiles.forEach { (_, file) ->
                    assertFileInProjectExists(file)
                }
            }

            // Check run tasks are generated.
            build("tasks") {
                runTasks.forEach {
                    assertOutputContains((it), "The 'tasks' output doesn't contain a task ${it}")
                }
            }

            // Check that run tasks work fine and an entry point can be specified.
            build("runDebugExecutableHost") {
                assertOutputContains("<root>.main")
            }

            build("runBazDebugExecutableHost") {
                assertOutputContains("foo.main")
            }
        }
    }

    private fun testNativeBinaryDsl(project: String, gradleVersion: GradleVersion) {
        nativeProject("native-binaries" + "/" + project, gradleVersion)
        {
            val hostSuffix = nativeHostTargetName.capitalize()

            build("tasks") {

                // Check that getters work fine.
                assertOutputContains("Check link task: linkReleaseShared$hostSuffix")
                assertOutputContains("Check run task: runFooReleaseExecutable$hostSuffix")
            }

        }
    }

    @DisplayName("Native binaries with kotlin-dsl")
    @GradleTest
    fun testNativeBinaryKotlinDSL(gradleVersion: GradleVersion) {
        testNativeBinaryDsl("kotlin-dsl", gradleVersion)
    }

    @DisplayName("Native binaries with groovy-dsl")
    @GradleTest
    fun testNativeBinaryGroovyDSL(gradleVersion: GradleVersion) {
        testNativeBinaryDsl("groovy-dsl", gradleVersion)
    }

    @DisplayName("Checking kotlinOptions property")
    @GradleTest
    fun testKotlinOptions(gradleVersion: GradleVersion) {
        nativeProject("native-kotlin-options", gradleVersion) {
            build(":compileKotlinHost") {
                extractNativeTasksCommandLineArgumentsFromOutput(":compileKotlinHost") {
                    assertCommandLineArgumentsDoNotContain("-verbose")
                }
            }

            build("clean")

            buildGradle.appendText(
                """kotlin.targets["host"].compilations["main"].kotlinOptions.verbose = true"""
            )
            build(":compileKotlinHost") {
                extractNativeTasksCommandLineArgumentsFromOutput(":compileKotlinHost") {
                    assertCommandLineArgumentsContain("-verbose")
                }
            }
        }
    }

    // We propagate compilation args to link tasks for now (see KT-33717).
    // TODO: Reenable the test when the args are separated.
    @Disabled
    @DisplayName("Native free args warning check")
    @GradleTest
    fun testNativeFreeArgsWarning(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/kotlin-dsl", gradleVersion) {
            buildGradleKts.appendText(
                """kotlin.targets["macos64"].compilations["main"].kotlinOptions.freeCompilerArgs += "-opt""""
            )
            subProject("exported").buildGradleKts.appendText(
                """
                kotlin.targets["macos64"].compilations["main"].kotlinOptions.freeCompilerArgs += "-opt"
                kotlin.targets["macos64"].compilations["test"].kotlinOptions.freeCompilerArgs += "-g"
                kotlin.targets["linux64"].compilations["main"].kotlinOptions.freeCompilerArgs +=
                    listOf("-g", "-Xdisable-phases=Devirtualization,BuildDFG")
            """.trimIndent()
            )
            build("tasks") {
                assertOutputContains(
                    """
                The following free compiler arguments must be specified for a binary instead of a compilation:
                    * In project ':':
                        * In target 'macos64':
                            * Compilation: 'main', arguments: [-opt]
                    * In project ':exported':
                        * In target 'linux64':
                            * Compilation: 'main', arguments: [-g, -Xdisable-phases=Devirtualization,BuildDFG]
                        * In target 'macos64':
                            * Compilation: 'main', arguments: [-opt]
                            * Compilation: 'test', arguments: [-g]

                Please move them into final binary declarations. E.g. binaries.executable { freeCompilerArgs += "..." }
                See more about final binaries: https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#building-final-native-binaries.
                """.trimIndent()
                )
            }
        }
    }


    @OptIn(EnvironmentalVariablesOverride::class)
    @DisplayName("Checking native tests")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @GradleTest
    fun testNativeTests(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion) {
            val hostTestTask = "hostTest"
            val testTasks = listOf(hostTestTask, "iosTest", "iosArm64Test")

            val testsToExecute = mutableListOf(":$hostTestTask")
            when (HostManager.host) {
                KonanTarget.MACOS_X64 -> testsToExecute.add(":iosTest")
                KonanTarget.MACOS_ARM64 -> testsToExecute.add(":iosArm64Test")
                else -> {}
            }
            val testsToSkip = testTasks.map { ":$it" } - testsToExecute

            val suffix = HostManager.host.family.exeSuffix
            val defaultOutputFile = "build/bin/host/debugTest/test.$suffix"
            val anotherOutputFile = "build/bin/host/anotherDebugTest/another.$suffix"

            build("tasks") {
                testTasks.forEach {
                    // We need to create tasks for all hosts
                    assertOutputContains("$it - ", "There is no test task '$it' in the task list.")
                }
            }

            // Perform all following checks in a single test to avoid running the K/N compiler several times.
            // Check that tests are not built during the ":assemble" execution
            build("assemble") {
                assertFileInProjectNotExists(defaultOutputFile)
                assertFileInProjectNotExists(anotherOutputFile)
            }

            // Store currently booted simulators to check that they don't leak (MacOS only).
            val bootedSimulatorsBefore = getBootedSimulators()

            // Check the case when all tests pass.
            build("check", buildOptions = defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)) {

                assertTasksExecuted(*testsToExecute.toTypedArray())
                assertTasksSkipped(*testsToSkip.toTypedArray())

                if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0)) {
                    assertOutputContains("org\\.foo\\.test\\.TestKt\\.fooTest\\s+PASSED".toRegex())
                    assertOutputContains("org\\.foo\\.test\\.TestKt\\.barTest\\s+PASSED".toRegex())
                } else {
                    assertOutputContains("org\\.foo\\.test\\.TestKt\\.fooTest\\[host]\\s+PASSED".toRegex())
                    assertOutputContains("org\\.foo\\.test\\.TestKt\\.barTest\\[host]\\s+PASSED".toRegex())
                }

                assertFileInProjectExists(defaultOutputFile)
            }

            checkTestsUpToDate(
                testsToExecute,
                testsToSkip,
                EnvironmentalVariables(mapOf("ANDROID_HOME" to projectPath.absolutePathString()))
            )

            // Check simulator process leaking.
            val bootedSimulatorsAfter = getBootedSimulators()
            assertEquals(bootedSimulatorsBefore, bootedSimulatorsAfter)

            // Check the case with failed tests.
            checkFailedTests(hostTestTask, testsToExecute, testsToSkip)

            build("linkAnotherDebugTestHost") {
                assertFileInProjectExists(anotherOutputFile)
            }
        }
    }

    private fun TestProject.getBootedSimulators(): Set<String>? =
        if (HostManager.hostIsMac) {
            val simulators = runProcess(listOf("xcrun", "simctl", "list"), projectPath.toFile(), System.getenv()).also {
                assertTrue(it.isSuccessful, "xcrun exection failed")
            }.output

            simulators.split('\n').filter { it.contains("(Booted)") }.map { it.trim() }.toSet()
        } else {
            null
        }

    private fun TestProject.checkTestsUpToDate(
        testsToExecute: List<String>,
        testsToSkip: List<String>,
        newEnv: EnvironmentalVariables,
    ) {

        // Check that test tasks are up-to-date on second run
        build("check") {

            assertTasksUpToDate(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())
        }

        // Check that setting new value to tracked environment variable triggers tests rerun
        build(
            "check",
            environmentVariables = newEnv
        ) {

            assertTasksExecuted(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())
        }

        build(
            "check",
            environmentVariables = newEnv
        ) {

            assertTasksUpToDate(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())
        }
    }

    private fun TestProject.checkFailedTests(hostTestTask: String, testsToExecute: List<String>, testsToSkip: List<String>) {
        projectPath.resolve("src/commonTest/kotlin/test.kt").appendText(
            """
                @Test
                fun fail() {
                    throw IllegalStateException("FAILURE!")
                }
            """.trimIndent()
        )
        buildAndFail("check") {

            assertTasksFailed(":allTests")
            // In the aggregation report mode platform-specific tasks
            // are executed successfully even if there are failing tests.
            assertTasksExecuted(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())

            if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_0)) {
                assertOutputContains("org\\.foo\\.test\\.TestKt\\.fail\\s+FAILED".toRegex())
            } else {
                assertOutputContains("org\\.foo\\.test\\.TestKt\\.fail\\[host]\\s+FAILED".toRegex())
            }
        }

        // Check that individual test reports are created correctly.
        buildAndFail("check", "-Pkotlin.tests.individualTaskReports=true", "--continue") {

            // In the individual report mode platform-specific tasks
            // fail if there are failing tests.
            assertTasksFailed(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())


            fun assertStacktrace(taskName: String, targetName: String) {
                val testReport = projectPath.resolve("build/test-results/$taskName/TEST-org.foo.test.TestKt.xml").toFile()
                val stacktrace = SAXBuilder().build(testReport).rootElement
                    .getChildren("testcase")
                    .single { it.getAttribute("name").value == "fail" || it.getAttribute("name").value == "fail[$targetName]" }
                    .getChild("failure")
                    .text
                assertTrue(stacktrace.contains("""at org\.foo\.test#fail\(.*test\.kt:29\)""".toRegex()))
            }

            fun assertTestResultsAnyOf(
                @TestDataFile assertionFile1Name: String,
                @TestDataFile assertionFile2Name: String,
                vararg testReportNames: String,
            ) {
                try {
                    assertTestResults(projectPath.resolve(assertionFile1Name), *testReportNames) { s ->
                        val excl = "Invalid connection: com.apple.coresymbolicationd"
                        s.lines().filter { it != excl }.joinToString("\n")
                    }
                } catch (e: AssertionError) {
                    assertTestResults(projectPath.resolve(assertionFile2Name), *testReportNames) { s ->
                        val excl = "Invalid connection: com.apple.coresymbolicationd"
                        s.lines().filter { it != excl }.joinToString("\n")
                    }
                }
            }

            val expectedHostTestResult = "TEST-TestKt.xml"
            val expectedIOSTestResults = listOf(
                "TEST-TestKt-iOSsim.xml",
                "TEST-TestKt-iOSArm64sim.xml",
            )

            assertTestResults(projectPath.resolve(expectedHostTestResult), hostTestTask)
            // K/N doesn't report line numbers correctly on Linux (see KT-35408).
            // TODO: Move assertStacktrace(hostTestTask, "host") out of if clause
            if (HostManager.hostIsMac) {
                assertStacktrace(hostTestTask, "host")
                val testTarget = when (HostManager.host) {
                    KonanTarget.MACOS_ARM64 -> "iosArm64"
                    KonanTarget.MACOS_X64 -> "ios"
                    else -> throw IllegalStateException("Unsupported host: ${HostManager.host}")
                }
                val testTask = "${testTarget}Test"
                assertTestResultsAnyOf(
                    expectedIOSTestResults[0],
                    expectedIOSTestResults[1],
                    testTask
                )
                assertStacktrace(testTask, testTarget)
            }
        }
    }

    @DisplayName("Checking work with tests' getters")
    @GradleTest
    fun testNativeTestGetters(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion) {
            // Check that test binaries can be accessed in a buildscript.
            build("checkGetters") {
                val suffix = if (HostManager.hostIsMingw) "exe" else "kexe"
                val names = listOf("test", "another")
                val files = names.map { "$it.$suffix" }

                files.forEach {
                    assertOutputContains("Get test: $it")
                    assertOutputContains("Find test: $it")
                }
            }
        }
    }

    @DisplayName("Checks that build fails if a test executable crashes")
    @GradleTest
    fun kt33750(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion) {
            projectPath.resolve("src/commonTest/kotlin/test.kt")
                .appendText("\nval fail: Int = error(\"\")\n")
            buildAndFail("check") {
                assertOutputContains("Execution failed for task ':allTests'")
            }
        }
    }

    @DisplayName("Checks builds with cinterop tool")
    @GradleTest
    fun testCinterop(gradleVersion: GradleVersion) {
        nativeProject("native-cinterop", gradleVersion, configureSubProjects = true) {
            fun libraryFiles(projectName: String, cinteropName: String) = listOf(
                projectPath.resolve("$projectName/build/classes/kotlin/host/main/cinterop/${projectName}-cinterop-$cinteropName.klib"),
                projectPath.resolve("$projectName/build/classes/kotlin/host/main/klib/${projectName}.klib"),
                projectPath.resolve("$projectName/build/classes/kotlin/host/test/klib/${projectName}_test.klib")
            )

            // Enable info log to see cinterop environment variables.
            build(
                ":projectLibrary:build",
            ) {
                assertTasksExecuted(":projectLibrary:cinteropAnotherNumberHost")
                libraryFiles("projectLibrary", "anotherNumber").forEach { assertFileExists(it) }
                assertNativeTasksCustomEnvironment(
                    ":projectLibrary:cinteropAnotherNumberHost",
                    toolName = NativeToolKind.C_INTEROP
                ) { env ->
                    assertEquals("1", env["LIBCLANG_DISABLE_CRASH_RECOVERY"])
                }
            }

            build(":publishedLibrary:build", ":publishedLibrary:publish") {
                assertTasksExecuted(":publishedLibrary:cinteropNumberHost")
                libraryFiles("publishedLibrary", "number").forEach { assertFileExists(it) }
            }

            build(":build")
        }
    }

    @DisplayName("Checks builds with changing compiler version")
    @OptIn(EnvironmentalVariablesOverride::class)
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @GradleTest
    fun testCompilerVersionChange(gradleVersion: GradleVersion) {
        if (KotlinToolingVersion(TestVersions.Kotlin.STABLE_RELEASE) < KotlinToolingVersion("1.9.20-Beta")) {
            val konanDataDir = defaultBuildOptions.konanDataDir?.toAbsolutePath()?.normalize()?.absolutePathString()
                ?: error("konanDataDir must not be null in this test. Please set a custom konanDataDir property.")

            nativeProject(
                "native-compiler-version",
                gradleVersion,
                environmentVariables = EnvironmentalVariables(mapOf("KONAN_DATA_DIR" to konanDataDir)),
            ) {
                val compileTasks = ":compileKotlinHost"

                build(compileTasks) {
                    assertTasksExecuted(compileTasks)
                }

                build(compileTasks) {
                    assertTasksUpToDate(compileTasks)
                }

                // Check that changing K/N version lead to tasks rerun
                build(compileTasks, "-Porg.jetbrains.kotlin.native.version=${TestVersions.Kotlin.STABLE_RELEASE}") {
                    assertTasksExecuted(compileTasks)
                }
            }
        } else {
            fail(
                "Please remove setting KONAN_DATA_DIR environment variable," +
                        " because Kotlin stable release now supports konan.data.dir Gradle property."
            )
        }

    }

    @DisplayName("Assert that a project with a native target can be configure")
    @GradleTest
    fun testKt29725(gradleVersion: GradleVersion) {
        nativeProject("native-compiler-version", gradleVersion) {
            build("tasks")
        }
    }

    @DisplayName("Assert that a project with a native target can be configure")
    @GradleTest
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS])
    fun testIgnoreDisabledNativeTargets(gradleVersion: GradleVersion) {
        nativeProject("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            build {
                assertHasDiagnostic(KotlinToolingDiagnostics.DisabledKotlinNativeTargets)
            }
            build("-P$KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS_PROPERTY=true") {
                assertNoDiagnostic(KotlinToolingDiagnostics.DisabledKotlinNativeTargets)
            }
        }
    }

    @DisplayName("Checks native arguments with the spaces in it")
    @GradleTest
    fun testNativeArgsWithSpaces(gradleVersion: GradleVersion) {
        nativeProject("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            val complicatedDirectoryName = if (HostManager.hostIsMingw) {
                // Windows doesn't allow creating a file with " in its name.
                "path with spaces"
            } else {
                "path with spaces and \""
            }

            val fileWithSpacesInPath = projectPath.resolve("src/commonMain/kotlin/$complicatedDirectoryName").toFile()
                .apply { mkdirs() }
                .canonicalFile
                .resolve("B.kt")
            fileWithSpacesInPath.writeText("fun foo() = 42")

            build(
                "compileKotlin${nativeHostTargetName.capitalize()}",
            ) {
                extractNativeTasksCommandLineArgumentsFromOutput(":compileKotlin${nativeHostTargetName.capitalize()}") {
                    val escapedQuotedPath =
                        "\"${fileWithSpacesInPath.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                    assertCommandLineArgumentsContain(escapedQuotedPath)
                }
            }
        }
    }

    @DisplayName("Checks binary options dsl")
    @GradleTest
    fun testBinaryOptionsDSL(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/executables", gradleVersion) {
            buildGradleKts.appendText(
                """
                    kotlin.targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java) {
                        binaries.all { binaryOptions["memoryModel"] = "experimental" }
                    }
                """.trimIndent()
            )
            build(":linkDebugExecutableHost") {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugExecutableHost") {
                    assertCommandLineArgumentsContain("-Xbinary=memoryModel=experimental")
                }
            }
        }
    }

    @DisplayName("Checks binary options property")
    @GradleTest
    fun testBinaryOptionsProperty(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/executables", gradleVersion) {
            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.binary.memoryModel=experimental",
            ) {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugExecutableHost") {
                    assertCommandLineArgumentsContain("-Xbinary=memoryModel=experimental")
                }
            }
        }
    }

    @DisplayName("Checks binary options priority")
    @GradleTest
    fun testBinaryOptionsPriority(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/executables", gradleVersion) {
            buildGradleKts.appendText(
                """
                    kotlin.targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java) {
                        binaries.all { binaryOptions["memoryModel"] = "experimental" }
                    }
                """.trimIndent()
            )
            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.binary.memoryModel=strict",
            ) {
                extractNativeTasksCommandLineArgumentsFromOutput(":linkDebugExecutableHost") {
                    // Options set in the DSL have higher priority than options set in project properties.
                    assertCommandLineArgumentsContain("-Xbinary=memoryModel=experimental")
                }
            }
        }
    }

    @DisplayName("Checks citerop configuration variant aware resolution")
    @GradleTest
    fun testCinteropConfigurationsVariantAwareResolution(gradleVersion: GradleVersion) {
        nativeProject("native-cinterop", gradleVersion, configureSubProjects = true) {
            build(":publishedLibrary:publish")

            build(":dependencyInsight", "--configuration", "hostTestCInterop", "--dependency", "org.example:publishedLibrary") {
                assertOutputContainsNativeFrameworkVariant("hostApiElements-published", gradleVersion)
            }

            subProject("projectLibrary").buildGradle.appendText(
                "\n" + """
                                configurations.create("ktlint") {
                                    def bundlingAttribute = Attribute.of("org.gradle.dependency.bundling", String)
                                    attributes.attribute(bundlingAttribute, "external")
                                }
                            """.trimIndent()
            )

            build(":dependencyInsight", "--configuration", "hostTestCInterop", "--dependency", ":projectLibrary") {
                assertOutputContainsNativeFrameworkVariant("hostCInteropApiElements", gradleVersion)
            }
            build(":dependencyInsight", "--configuration", "hostCompileKlibraries", "--dependency", ":projectLibrary") {
                assertOutputContainsNativeFrameworkVariant("hostApiElements", gradleVersion)
            }
        }
    }

    @DisplayName("Checks allowing to override download url")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @GradleTest
    fun shouldAllowToOverrideDownloadUrl(gradleVersion: GradleVersion, @TempDir customKonanDir: Path) {
        nativeProject("native-parallel", gradleVersion) {
            gradleProperties.appendText(
                """
                
                kotlin.native.distribution.baseDownloadUrl=https://non-existent.net
                """.trimIndent()
            )

            gradleProperties.replaceText("cacheRedirectorEnabled=true", "cacheRedirectorEnabled=false")

            buildAndFail(
                "build",
                buildOptions = defaultBuildOptions.copy(
                    konanDataDir = customKonanDir.toAbsolutePath()
                )
            ) {
                assertOutputContains("Could not HEAD 'https://non-existent.net")
            }
        }
    }

    // KT-52303
    @DisplayName("Checks that changing build dir applied to binaries")
    @GradleTest
    fun testBuildDirChangeAppliedToBinaries(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/executables", gradleVersion) {
            buildGradleKts.appendText(
                """
                    project.buildDir = file("${'$'}{project.buildDir.absolutePath}/mydir")
                """.trimIndent()
            )
            build(":linkDebugExecutableHost") {
                assertDirectoryInProjectExists("build/mydir/bin/host/debugExecutable")
                assertFileInProjectNotExists("build/bin")
            }
        }
    }

    // KT-54439
    @DisplayName("Checks Language settings sync ")
    @GradleTest
    fun testLanguageSettingsSyncToNativeTasks(gradleVersion: GradleVersion) {
        nativeProject("native-kotlin-options", gradleVersion) {
            buildGradle.modify {
                """
                |${it.substringBefore("kotlin {")}
                |
                |kotlin {
                |    ${HostManager.host.presetName}("host") {
                |        compilations.named("main").configure { 
                |            kotlinOptions.freeCompilerArgs += ["-Xverbose-phases=Linker"] 
                |        }
                |    }
                |}
                """.trimMargin()
            }

            build("assemble") {
                assertOutputContains("-Xverbose-phases=Linker")
            }
        }
    }


    // KT-58537
    @DisplayName("Build native project with name containing space")
    @GradleTest
    fun testProjectNameWithSpaces(gradleVersion: GradleVersion) {
        nativeProject("native-root-project-name-with-space", gradleVersion, configureSubProjects = true) {
            build("assemble") {
                assertOutputDoesNotContain("Could not find \"Contains\" in")
            }
        }
    }
}