/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import com.intellij.openapi.util.JDOMUtil
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.internals.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.capitalize
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OsCondition(enabledOnCI = [OS.LINUX, OS.MAC, OS.WINDOWS])
@DisplayName("Tests for general K/N builds")
@NativeGradlePluginTests
class GeneralNativeIT : KGPBaseTest() {

    private val nativeHostTargetName = MPPNativeTargets.current

    @DisplayName("K/N compiler can be started in-process in parallel")
    @GradleTest
    @TestMetadata("native-parallel")
    fun shouldCheckKNativeCompilerStartedInParallel(gradleVersion: GradleVersion) {
        nativeProject("native-parallel", gradleVersion) {
            buildGradleKts.appendText(
                """
                open class Box: BuildServiceParameters, java.io.Serializable {
                    var latch: CountDownLatch? = null
                    private fun writeObject(stream: java.io.ObjectOutputStream) { /* do nothing */ }
                    private fun readObject(stream: java.io.ObjectInputStream) { this.latch = CountDownLatch(2) }
                }
                abstract class Service : BuildService<Box>
                gradle.sharedServices.registerIfAbsent("service", Service::class.java) {}
                """.trimIndent()
            )

            fun insertSynchronisationBlock(file: Path) {
                file.appendText(
                    """
                    run {
                        val serviceProvider = gradle.sharedServices.registrations.getByName("service").getService()
                        tasks.getByPath("compileKotlinLinux").doFirst {
                            val service = serviceProvider.get()
                            val countDownLatch = service.parameters.javaClass.getMethod("getLatch").invoke(service.parameters) as CountDownLatch
                            countDownLatch.countDown()
                            countDownLatch.await()
                        }
                    }
                    """.trimIndent()
                )
            }

            // we are adding synchronisation before compileKotlinLinux execution in each subproject for making
            // at least the execution of these tasks parallel
            insertSynchronisationBlock(subProject("one").buildGradleKts)
            insertSynchronisationBlock(subProject("two").buildGradleKts)

            build(":one:compileKotlinLinux", ":two:compileKotlinLinux")
        }
    }

    @DisplayName("Can produce native libraries")
    @GradleTest
    @TestMetadata("native-binaries/libraries")
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
            val klibPath = "${kotlinClassesDir(targetName = "host")}${klibPrefix}/klib/native-library"

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
                assertDirectoryInProjectExists(klibPath)
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
    @DisplayName("Can provide native framework")
    @GradleTest
    @TestMetadata("native-binaries/frameworks")
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
            // Check dependency exporting in frameworks.
            assemble {
                headerPaths.forEach { assertFileInProjectExists(it) }
                frameworkPaths.forEach { assertDirectoryInProjectExists(it) }

                assertFileInProjectContains(headerPaths[0], "+ (int32_t)exported")

                extractNativeTasksCommandLineArgumentsFromOutput(":linkMainReleaseFrameworkIos") {
                    assertCommandLineArgumentsContain("-opt")
                }
                extractNativeTasksCommandLineArgumentsFromOutput(":linkMainDebugFrameworkIos") {
                    assertCommandLineArgumentsContain("-g")
                }
                // Check that bitcode can be disabled by setting custom compiler options
                extractNativeTasksCommandLineArgumentsFromOutput(":linkCustomDebugFrameworkIos") {
                    assertCommandLineArgumentsContainSequentially("-linker-option", "-L.")
                    assertCommandLineArgumentsContain(
                        "-Xtime",
                        "-Xstatic-framework",
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
    @TestMetadata("native-binaries/libraries")
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
    @TestMetadata("native-binaries/frameworks")
    fun testExportApiOnlyToFrameworks(gradleVersion: GradleVersion) {
        testExportApi(
            nativeProject("native-binaries/frameworks", gradleVersion),
            listOf(
                ExportApiTestData("linkMainDebugFrameworkIos", "mainDebugFramework")
            )
        )
    }

    @DisplayName("Checks generating lldbinit file")
    @GradleTest
    @TestMetadata("native-binaries/frameworks")
    fun testGenerateLLDBInitFile(gradleVersion: GradleVersion) {
        nativeProject("native-binaries/frameworks", gradleVersion = gradleVersion) {
            val lldbPath = projectPath.resolve("build").resolve("lldbinit")

            build(":setupLldbScript") {
                assertFileInProjectExists(lldbPath.absolutePathString())
                assertFileContains(lldbPath, "command script import")
                assertFileContains(lldbPath, "konan_lldb.py")

                val scriptContent = lldbPath.readLines()
                assert(scriptContent.size == 1) {
                    "lldbinit file contains more than 1 line or doesn't contains lines at all"
                }

                val scriptPath = scriptContent
                    .first()
                    .replace("command script import", "")
                    .trimIndent()

                assertFileInProjectExists(scriptPath)
            }
        }
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
    @TestMetadata("native-binaries/export-published-lib")
    fun testTransitiveExportIsNotRequiredForExportingVariant(gradleVersion: GradleVersion) {
        project(
            "native-binaries/export-published-lib",
            gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion)
        ) {
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
    @TestMetadata("native-binaries/executables")
    fun testNativeExecutables(gradleVersion: GradleVersion) {
        nativeProject(
            "native-binaries/executables",
            gradleVersion,
            /**
             * Enable CC since 8.0 for KT-69918:
             * - Before 8.0 Gradle doesn't deserialize CC during the first execution and the issue is not visible
             * - Before 7.4.2 there is a CC serialization failure because Gradle can't serialize ComponentResult
             */
            buildOptions = defaultBuildOptions.disableConfigurationCacheForGradle7(gradleVersion),
        ) {
            val binaries = listOf(
                "debugExecutable" to "native-binary",
                "releaseExecutable" to "native-binary",
                "bazDebugExecutable" to "my-baz",
            )
            val linkTasks =
                binaries.map { (name, _) -> "link${name.capitalize()}Host" }
            val outputFiles = binaries.associate { (name, fileBaseName) ->
                val outputKind = NativeOutputKind.entries.single { name.endsWith(it.taskNameClassifier, true) }.compilerOutputKind
                val prefix = outputKind.prefix(HostManager.host)
                val suffix = outputKind.suffix(HostManager.host)
                val fileName = "$prefix$fileBaseName$suffix"
                name to "build/bin/host/$name/$fileName"
            }
            val runTasks = listOf(
                "runDebugExecutable",
                "runReleaseExecutable",
                "runBazDebugExecutable",
            ).map { it + "Host" }

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
                    assertOutputContains((it), "The 'tasks' output doesn't contain a task $it")
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
        nativeProject("native-binaries/$project", gradleVersion)
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
    @TestMetadata("native-binaries/kotlin-dsl")
    fun testNativeBinaryKotlinDSL(gradleVersion: GradleVersion) {
        testNativeBinaryDsl("kotlin-dsl", gradleVersion)
    }

    @DisplayName("Native binaries with groovy-dsl")
    @GradleTest
    @TestMetadata("native-binaries/groovy-dsl")
    fun testNativeBinaryGroovyDSL(gradleVersion: GradleVersion) {
        testNativeBinaryDsl("groovy-dsl", gradleVersion)
    }

    @DisplayName("Checking kotlinOptions property")
    @GradleTest
    @TestMetadata("native-kotlin-options")
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
    @TestMetadata("native-binaries/kotlin-dsl")
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
    @GradleTest
    @TestMetadata("native-tests")
    fun testNativeTests(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion) {
            val hostTestTask = "hostTest"
            val testTasks = setOf(hostTestTask, "iosTest", "iosArm64Test")

            val testsToExecute = buildSet {
                add(":$hostTestTask")
                when (HostManager.host) {
                    KonanTarget.MACOS_X64 -> add(":iosTest")
                    KonanTarget.MACOS_ARM64 -> add(":iosArm64Test")
                    else -> {}
                }
            }
            val testsToSkip = testTasks.map { ":$it" } - testsToExecute

            enablePassedTestLogging()

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

            // Store currently booted simulators to check that they don't leak (macOS only).
            val bootedSimulatorsBefore = getBootedSimulators()

            // Check the case when all tests pass.
            build("check") {
                assertTasksExecuted(testsToExecute)
                assertTasksSkipped(testsToSkip)

                assertOutputContains("org.foo.test.TestKt.fooTest[host] PASSED")
                assertOutputContains("org.foo.test.TestKt.barTest[host] PASSED")

                assertFileInProjectExists(defaultOutputFile)
            }

            checkTestsUpToDate(
                testsToExecute,
                testsToSkip,
                EnvironmentalVariables(mapOf("ANDROID_HOME" to projectPath.absolutePathString()))
            )

            // Check if the simulator process leaks.
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
        testsToExecute: Collection<String>,
        testsToSkip: Collection<String>,
        newEnv: EnvironmentalVariables,
    ) {

        // Check that test tasks are up to date on second run
        build("check") {
            assertTasksUpToDate(testsToExecute)
            assertTasksSkipped(testsToSkip)
        }

        // Check that setting new value to tracked environment variable triggers tests rerun
        build(
            "check",
            environmentVariables = newEnv
        ) {
            assertTasksExecuted(testsToExecute)
            assertTasksSkipped(testsToSkip)
        }

        build(
            "check",
            environmentVariables = newEnv
        ) {
            assertTasksUpToDate(testsToExecute)
            assertTasksSkipped(testsToSkip)
        }
    }

    private fun TestProject.checkFailedTests(
        hostTestTask: String,
        testsToExecute: Collection<String>,
        testsToSkip: Collection<String>,
    ) {
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
            assertTasksExecuted(testsToExecute)
            assertTasksSkipped(testsToSkip)

            assertOutputContains("org.foo.test.TestKt.fail[host] FAILED")
        }

        // Check that individual test reports are created correctly.
        buildAndFail(
            "check",
            "-Pkotlin.tests.individualTaskReports=true",
            buildOptions = defaultBuildOptions.copy(continueAfterFailure = true)
        ) {

            // In the individual report mode platform-specific tasks
            // fail if there are failing tests.
            assertTasksFailed(testsToExecute)
            assertTasksSkipped(testsToSkip)


            fun assertStacktrace(taskName: String, targetName: String) {
                val testReport = projectPath.resolve("build/test-results/$taskName/TEST-org.foo.test.TestKt.xml").toFile()
                val stacktrace = JDOMUtil.load(testReport)
                    .getChildren("testcase")
                    .single { it.getAttribute("name").value == "fail" || it.getAttribute("name").value == "fail[$targetName]" }
                    .getChild("failure")
                    .text
                assertTrue(stacktrace.contains("""at org\.foo\.test#fail\(.*test\.kt:29\)""".toRegex()))
            }

            val expectedHostTestResult = "TEST-TestKt.xml"
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

                val expectedXmlPath = projectPath.resolve("TEST-TestKt-iOSsim.xml")
                projectPath.resolve("TEST-TestKt-iOSsim-template.xml").copyTo(expectedXmlPath)
                expectedXmlPath.replaceText("<target>", testTarget)
                assertTestResults(
                    expectedXmlPath,
                    testTask,
                    cleanupStdOut = {
                        // Sometimes the output contains this "Invalid connection" line.
                        // Remove it to make the test stable.
                        // See also https://youtrack.jetbrains.com/issue/KT-76748.
                        it.replace("Invalid connection: com.apple.coresymbolicationd\n", "")
                    }
                )
                assertStacktrace(testTask, testTarget)
            }
        }
    }

    @DisplayName("Checking work with tests' getters")
    @GradleTest
    @TestMetadata("native-tests")
    fun testNativeTestGetters(gradleVersion: GradleVersion) {
        nativeProject("native-tests", gradleVersion) {
            // Check that test binaries can be accessed in a buildscript.
            build("tasks") {
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
    @TestMetadata("native-tests")
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
    @TestMetadata("native-cinterop")
    fun testCinterop(gradleVersion: GradleVersion) {
        nativeProject(
            "native-cinterop",
            gradleVersion, configureSubProjects = true,
            localRepoDir = defaultLocalRepo(gradleVersion)
        ) {
            fun libraryDirectories(projectName: String, cinteropName: String) = listOf(
                projectPath.resolve("$projectName/build/classes/kotlin/host/main/cinterop/${projectName}-cinterop-$cinteropName"),
                projectPath.resolve("$projectName/build/classes/kotlin/host/main/klib/${projectName}"),
                projectPath.resolve("$projectName/build/classes/kotlin/host/test/klib/${projectName}_test")
            )

            // Enable info log to see cinterop environment variables.
            build(
                ":projectLibrary:build",
            ) {
                assertTasksExecuted(":projectLibrary:cinteropAnotherNumberHost")
                libraryDirectories("projectLibrary", "anotherNumber").forEach { assertDirectoryExists(it) }
                assertNativeTasksCustomEnvironment(
                    ":projectLibrary:cinteropAnotherNumberHost",
                    toolName = NativeToolKind.C_INTEROP
                ) { env ->
                    assertEquals("1", env["LIBCLANG_DISABLE_CRASH_RECOVERY"])
                }
            }

            build(":publishedLibrary:build", ":publishedLibrary:publish") {
                assertTasksExecuted(":publishedLibrary:cinteropNumberHost")
                libraryDirectories("publishedLibrary", "number").forEach { assertDirectoryExists(it) }
            }

            build(":build")
        }
    }

    @DisplayName("Checks builds with changing compiler version")
    @GradleTestVersions
    @GradleTest
    @TestMetadata("native-compiler-version")
    fun testCompilerVersionChange(gradleVersion: GradleVersion) {
        nativeProject("native-compiler-version", gradleVersion) {
            val compileTasks = ":compileKotlinHost"

            build(compileTasks) {
                assertTasksExecuted(compileTasks)
            }

            build(compileTasks) {
                assertTasksUpToDate(compileTasks)
            }

            // Check that changing K/N version lead to tasks rerun
            build(
                compileTasks, buildOptions = defaultBuildOptions.copy(
                    nativeOptions = defaultBuildOptions.nativeOptions.copy(version = TestVersions.Kotlin.STABLE_RELEASE)
                )
            ) {
                assertTasksExecuted(compileTasks)
            }
        }
    }

    @DisplayName("Assert that a project with a native target can be configure")
    @GradleTest
    @TestMetadata("native-compiler-version")
    fun testKt29725(gradleVersion: GradleVersion) {
        nativeProject("native-compiler-version", gradleVersion) {
            build("tasks")
        }
    }

    @DisplayName("Assert that a project with a native target can be configured on platforms without frameworks")
    @GradleTest
    @OsCondition(supportedOn = [OS.LINUX, OS.WINDOWS])
    @TestMetadata("new-mpp-lib-and-app/sample-lib")
    fun testIgnoreDisabledNativeTargets(gradleVersion: GradleVersion) {
        nativeProject("new-mpp-lib-and-app/sample-lib", gradleVersion, buildOptions = defaultBuildOptions.disableKlibsCrossCompilation()) {
            build {
                assertHasDiagnostic(KotlinToolingDiagnostics.DisabledKotlinNativeTargets)
            }
            build("-P$KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS_PROPERTY=true") {
                assertNoDiagnostic(KotlinToolingDiagnostics.DisabledKotlinNativeTargets)
            }
        }
    }

    @DisplayName("Checks native arguments with the spaces in it")
    @TestMetadata("new-mpp-lib-and-app/sample-lib")
    @GradleTest
    fun testNativeArgsWithSpaces(gradleVersion: GradleVersion) {
        nativeProject("new-mpp-lib-and-app/sample-lib", gradleVersion) {
            val complicatedDirectoryName = if (HostManager.hostIsMingw) {
                // Windows doesn't allow creating a file with " in its name.
                "path with spaces"
            } else {
                "path with spaces and \""
            }

            val fileWithSpacesInPath = projectPath.resolve("src/commonMain/kotlin/$complicatedDirectoryName")
                .createDirectories()
                .absolute()
                .normalize()
                .toRealPath()
                .resolve("B.kt")
            fileWithSpacesInPath.writeText("fun foo() = 42")

            val expectedEscapeQuotedPath =
                fileWithSpacesInPath.absolutePathString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .let { "\"$it\"" }

            build(
                "compileKotlin${nativeHostTargetName.capitalize()}",
            ) {
                extractNativeTasksCommandLineArgumentsFromOutput(":compileKotlin${nativeHostTargetName.capitalize()}") {
                    assertCommandLineArgumentsContain(expectedEscapeQuotedPath)
                }
            }
        }
    }

    @DisplayName("Checks binary options dsl")
    @GradleTest
    @TestMetadata("native-binaries")
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
    @TestMetadata("native-binaries")
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
    @TestMetadata("native-binaries")
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

    @DisplayName("Checks cinterop configuration variant aware resolution")
    @GradleTest
    @TestMetadata("native-cinterop")
    fun testCinteropConfigurationsVariantAwareResolution(gradleVersion: GradleVersion) {
        nativeProject(
            "native-cinterop",
            gradleVersion, configureSubProjects = true,
            localRepoDir = defaultLocalRepo(gradleVersion)
        ) {
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
    @GradleTest
    @TestMetadata("native-parallel")
    fun shouldAllowToOverrideDownloadUrl(gradleVersion: GradleVersion, @TempDir customKonanDir: Path) {
        nativeProject(
            "native-parallel", gradleVersion,
            dependencyManagement = DependencyManagement.DisabledDependencyManagement
        ) {
            gradleProperties.appendText(
                """
                
                kotlin.native.distribution.baseDownloadUrl=https://non-existent.net
                """.trimIndent()
            )

            gradleProperties.replaceText("cacheRedirectorEnabled=true", "cacheRedirectorEnabled=false")

            buildAndFail(
                "build",
                buildOptions = defaultBuildOptions.copy(
                    nativeOptions = defaultBuildOptions.nativeOptions.copy(
                        distributionDownloadFromMaven = false // please remove this test, when this flag is removed
                    ),
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
    @TestMetadata("native-binaries")
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
    @TestMetadata("native-kotlin-options")
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
    @TestMetadata("native-root-project-name-with-space")
    fun testProjectNameWithSpaces(gradleVersion: GradleVersion) {
        nativeProject("native-root-project-name-with-space", gradleVersion, configureSubProjects = true) {
            build("assemble") {
                assertOutputDoesNotContain("Could not find \"Contains\" in")
            }
        }
    }

    @DisplayName("Test compiler arguments for K/Native Tasks")
    @GradleTest
    @TestMetadata("native-binaries")
    fun testCompilerArgumentsLogLevel(gradleVersion: GradleVersion) {
        nativeProject("native-libraries", gradleVersion) {
            val updatedBuildOptions = buildOptions.copy(
                compilerArgumentsLogLevel = "warning"
            )
            build("assemble", buildOptions = updatedBuildOptions) {
                val tasksWithNativeCompilerArguments = listOf(
                    ":compileCommonMainKotlinMetadata", // it is shared native metadata, which is compiled by konan
                    ":compileKotlinLinux64",
                    ":linkMainDebugStaticLinux64",
                )
                for (task in tasksWithNativeCompilerArguments) {
                    val taskOutput = getOutputForTask(task, LogLevel.INFO)
                    assertTrue(
                        taskOutput.contains("Arguments = "),
                        "Arguments were not logged by Task $task"
                    )
                }
            }
        }
    }

}
