/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import com.intellij.testFramework.TestDataFile
import org.jdom.input.SAXBuilder
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_DISABLE_WARNING_PROPERTY_NAME
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX
import org.jetbrains.kotlin.gradle.internals.NO_NATIVE_STDLIB_PROPERTY_WARNING
import org.jetbrains.kotlin.gradle.internals.NO_NATIVE_STDLIB_WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.isWindows
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import org.junit.Assume
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MPPNativeTargets {
    val current = when {
        HostManager.hostIsMingw -> "mingw64"
        HostManager.hostIsLinux -> "linux64"
        HostManager.hostIsMac -> "macos64"
        else -> error("Unknown host")
    }

    val unsupported = when {
        HostManager.hostIsMingw -> listOf("macos64")
        HostManager.hostIsLinux -> listOf("macos64", "mingw64")
        HostManager.hostIsMac -> listOf("mingw64")
        else -> error("Unknown host")
    }

    val supported = listOf("linux64", "macos64", "mingw64").filter { !unsupported.contains(it) }
}

class GeneralNativeIT : BaseGradleIT() {

    val nativeHostTargetName = MPPNativeTargets.current

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    private val SINGLE_NATIVE_TARGET_PLACHOLDER = "<SingleNativeTarget>"

    private fun Project.configureSingleNativeTarget(preset: String = HostManager.host.presetName) {
        projectDir.walk()
            .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
            .forEach { file ->
                file.modify {
                    it.replace(SINGLE_NATIVE_TARGET_PLACHOLDER, preset)
                }
            }
    }

    @Test
    fun testParallelExecutionSmoke(): Unit = with(transformProjectWithPluginsDsl("native-parallel")) {
        // Check that the K/N compiler can be started in-process in parallel.
        build(":one:compileKotlinLinux", ":two:compileKotlinLinux") {
            assertSuccessful()
        }
    }

    @Test
    fun testIncorrectDependenciesWarning() = with(Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
        setupWorkingDir()
        gradleBuildScript().modify {
            it.replace(
                "api 'org.jetbrains.kotlin:kotlin-stdlib-common'",
                "compileOnly 'org.jetbrains.kotlin:kotlin-stdlib-common'"
            )
        }

        build {
            assertSuccessful()
            assertContains("A compileOnly dependency is used in the Kotlin/Native target")
        }
        build("-Pkotlin.native.ignoreIncorrectDependencies=true") {
            assertSuccessful()
            assertNotContains("A compileOnly dependency is used in the Kotlin/Native target")
        }
    }

    @Test
    fun testEndorsedLibsController() {
        with(
            transformProjectWithPluginsDsl("native-endorsed")
        ) {
            configureSingleNativeTarget()
            setupWorkingDir()

            build("build") {
                assertSuccessful()
            }
            gradleBuildScript().modify {
                it.replace("enableEndorsedLibs = true", "")
            }
            build("build") {
                assertFailed()
            }
        }
    }

    @Test
    fun testCanProduceNativeLibraries() = with(transformProjectWithPluginsDsl("libraries", directoryPrefix = "native-binaries")) {
        configureSingleNativeTarget()

        val baseName = "native_library"

        val sharedPrefix = CompilerOutputKind.DYNAMIC.prefix(HostManager.host)
        val sharedSuffix = CompilerOutputKind.DYNAMIC.suffix(HostManager.host)
        val sharedPaths = listOf(
            "build/bin/host/debugShared/$sharedPrefix$baseName$sharedSuffix",
        )

        val staticPrefix = CompilerOutputKind.STATIC.prefix(HostManager.host)
        val staticSuffix = CompilerOutputKind.STATIC.suffix(HostManager.host)
        val staticPaths = listOf(
            "build/bin/host/debugStatic/$staticPrefix$baseName$staticSuffix",
        )

        val headerPaths = listOf(
            "build/bin/host/debugShared/$sharedPrefix${baseName}_api.h",
            "build/bin/host/debugStatic/$staticPrefix${baseName}_api.h",
        )

        val klibPrefix = CompilerOutputKind.LIBRARY.prefix(HostManager.host)
        val klibSuffix = CompilerOutputKind.LIBRARY.suffix(HostManager.host)
        val klibPath = "${targetClassesDir("host")}${klibPrefix}native-library$klibSuffix"

        val linkTasks = listOf(
            ":linkDebugSharedHost",
            ":linkDebugStaticHost",
        )

        val klibTask = ":compileKotlinHost"

        // Building
        build(":assemble") {
            assertSuccessful()
            assertTasksExecuted(linkTasks + klibTask)

            sharedPaths.forEach { assertFileExists(it) }
            staticPaths.forEach { assertFileExists(it) }
            headerPaths.forEach {
                assertFileExists(it)
                assertFileContains(it, "_KInt (*exported)();")
            }
            assertFileExists(klibPath)
        }

        // Test that all up-to date checks are correct
        build(":assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks)
            assertTasksUpToDate(klibTask)
        }

        // Remove header of one of libraries and check that it is rebuilt.
        assertTrue(projectDir.resolve(headerPaths[0]).delete())
        build(":assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks.drop(1))
            assertTasksUpToDate(klibTask)
            assertTasksExecuted(linkTasks[0])
        }

        // Check that plugin doesn't allow exporting dependencies not added in the API configuration.
        gradleBuildScript().modify {
            it.replace("api(project(\":exported\"))", "")
        }
        build(":assemble") {
            assertFailed()
            val failureMsg = "Following dependencies exported in the debugShared binary " +
                    "are not specified as API-dependencies of a corresponding source set"
            assertTrue(output.contains(failureMsg))
        }
    }

    @Test
    fun testCanProduceNativeFrameworks() = with(transformProjectWithPluginsDsl("frameworks", directoryPrefix = "native-binaries")) {
        Assume.assumeTrue(HostManager.hostIsMac)

        val baseName = "main"
        val frameworkPrefix = CompilerOutputKind.FRAMEWORK.prefix(HostManager.host)
        val frameworkSuffix = CompilerOutputKind.FRAMEWORK.suffix(HostManager.host)
        val frameworkPaths = listOf(
            "build/bin/ios/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix.dSYM",
            "build/bin/ios/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix",
            "build/bin/ios/mainReleaseFramework/$frameworkPrefix$baseName$frameworkSuffix",
            "build/bin/iosSim/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix.dSYM",
            "build/bin/iosSim/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix",
            "build/bin/iosSim/mainReleaseFramework/$frameworkPrefix$baseName$frameworkSuffix",
        )

        val headerPaths = listOf(
            "build/bin/ios/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix/headers/$baseName.h",
            "build/bin/ios/mainReleaseFramework/$frameworkPrefix$baseName$frameworkSuffix/headers/$baseName.h",
            "build/bin/iosSim/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix/headers/$baseName.h",
            "build/bin/iosSim/mainReleaseFramework/$frameworkPrefix$baseName$frameworkSuffix/headers/$baseName.h",
        )

        val frameworkTasks = listOf(
            ":linkMainDebugFrameworkIos",
            ":linkMainReleaseFrameworkIos",
            ":linkCustomDebugFrameworkIos",
            ":linkMainDebugFrameworkIosSim",
            ":linkMainReleaseFrameworkIosSim",
        )

        // Check building
        // Check dependency exporting and bitcode embedding in frameworks.
        build("assemble") {
            assertSuccessful()
            headerPaths.forEach { assertFileExists(it) }
            frameworkPaths.forEach { assertFileExists(it) }

            fileInWorkingDir(headerPaths[0]).readText().contains("+ (int32_t)exported")

            // Check that by default release frameworks have bitcode embedded.
            checkNativeCommandLineFor(":linkMainReleaseFrameworkIos") {
                assertTrue(it.contains("-Xembed-bitcode"))
                assertTrue(it.contains("-opt"))
            }
            // Check that by default debug frameworks have bitcode marker embedded.
            checkNativeCommandLineFor(":linkMainDebugFrameworkIos") {
                assertTrue(it.contains("-Xembed-bitcode-marker"))
                assertTrue(it.contains("-g"))
            }
            checkNativeCommandLineFor(":linkCustomDebugFrameworkIos") {
                assertTrue(it.contains("-linker-option -L."))
                assertTrue(it.contains("-Xtime"))
                assertTrue(it.contains("-Xstatic-framework"))
                assertFalse(it.contains("-Xembed-bitcode-marker"))
                assertFalse(it.contains("-Xembed-bitcode"))
            }
            // Check that bitcode is disabled for iOS simulator.
            checkNativeCommandLineFor(":linkMainReleaseFrameworkIosSim", ":linkMainDebugFrameworkIosSim") {
                assertFalse(it.contains("-Xembed-bitcode"))
                assertFalse(it.contains("-Xembed-bitcode-marker"))
            }
        }

        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(frameworkTasks)
        }

        assertTrue(projectDir.resolve(headerPaths[0]).delete())
        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(frameworkTasks.drop(1))
            assertTasksExecuted(frameworkTasks[0])
        }
    }

    @Test
    fun testExportApiOnlyToLibraries() = with(transformProjectWithPluginsDsl("libraries", directoryPrefix = "native-binaries")) {
        configureSingleNativeTarget()

        // Check that plugin doesn't allow exporting dependencies not added in the API configuration.
        gradleBuildScript().modify {
            it.replace("api(project(\":exported\"))", "")
        }

        fun failureMsgFor(binaryName: String) =
            "Following dependencies exported in the $binaryName binary are not specified as API-dependencies of a corresponding source set"

        build("linkDebugSharedHost") {
            assertFailed()
            assertContains(failureMsgFor("debugShared"))
        }
        build("linkDebugStaticHost") {
            assertFailed()
            assertContains(failureMsgFor("debugStatic"))
        }
    }

    private fun CompiledProject.checkNativeCommandLineFor(vararg taskPaths: String, check: (String) -> Unit) =
        taskPaths.forEach { taskPath ->
            val commandLine = output.lineSequence().dropWhile {
                !it.contains("Executing actions for task '$taskPath'")
            }.first {
                it.contains("Run tool: \"konanc\"")
            }
            check(commandLine)
        }

    @Test
    fun testNativeExecutables() = with(transformProjectWithPluginsDsl("executables", directoryPrefix = "native-binaries")) {
        configureSingleNativeTarget()

        val binaries = mutableListOf(
            "debugExecutable" to "native-binary",
            "releaseExecutable" to "native-binary",
            "fooDebugExecutable" to "foo",
            "fooReleaseExecutable" to "foo",
            "barReleaseExecutable" to "bar",
            "bazReleaseExecutable" to "my-baz",
        )
        val linkTasks = binaries.map { (name, _) -> "link${name.capitalize()}Host" }
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
            "runFooDebugExecutable",
            "runFooReleaseExecutable",
            "runBarReleaseExecutable",
            "runBazReleaseExecutable",
        ).map { it + "Host" }.toMutableList()

        // Check building
        build("hostMainBinaries") {
            assertSuccessful()
            assertTasksExecuted(linkTasks.map { ":$it" })
            assertTasksExecuted(":compileKotlinHost")
            outputFiles.forEach { (_, file) ->
                assertFileExists(file)
            }
        }

        // Check run tasks are generated.
        build("tasks") {
            assertSuccessful()
            runTasks.forEach {
                assertTrue(output.contains(it), "The 'tasks' output doesn't contain a task ${it}")
            }
        }

        // Check that run tasks work fine and an entry point can be specified.
        build("runDebugExecutableHost") {
            assertSuccessful()
            assertTrue(output.contains("<root>.main"))
        }

        build("runBazReleaseExecutableHost") {
            assertSuccessful()
            assertTrue(output.contains("foo.main"))
        }
    }

    private fun testNativeBinaryDsl(project: String) = with(
        transformProjectWithPluginsDsl(project, directoryPrefix = "native-binaries")
    ) {
        val hostSuffix = nativeHostTargetName.capitalize()

        build("tasks") {
            assertSuccessful()

            // Check that getters work fine.
            assertTrue(output.contains("Check link task: linkReleaseShared$hostSuffix"))
            assertTrue(output.contains("Check run task: runFooReleaseExecutable$hostSuffix"))
        }
    }

    @Test
    fun testNativeBinaryKotlinDSL() = testNativeBinaryDsl("kotlin-dsl")

    @Test
    fun testNativeBinaryGroovyDSL() = testNativeBinaryDsl("groovy-dsl")

    @Test
    fun testKotlinOptions() = with(
        transformProjectWithPluginsDsl("native-kotlin-options")
    ) {
        configureSingleNativeTarget()

        build(":compileKotlinHost") {
            assertSuccessful()
            checkNativeCommandLineFor(":compileKotlinHost") {
                assertFalse(it.contains("-verbose"))
            }
        }

        build("clean") {
            assertSuccessful()
        }

        gradleBuildScript().appendText(
            """kotlin.targets["host"].compilations["main"].kotlinOptions.verbose = true"""
        )
        build(":compileKotlinHost") {
            assertSuccessful()
            checkNativeCommandLineFor(":compileKotlinHost") {
                assertTrue(it.contains("-verbose"))
            }
        }
    }

    // We propagate compilation args to link tasks for now (see KT-33717).
    // TODO: Reenable the test when the args are separated.
    @Ignore
    @Test
    fun testNativeFreeArgsWarning() = with(transformProjectWithPluginsDsl("kotlin-dsl", directoryPrefix = "native-binaries")) {
        gradleBuildScript().appendText(
            """kotlin.targets["macos64"].compilations["main"].kotlinOptions.freeCompilerArgs += "-opt""""
        )
        gradleBuildScript("exported").appendText(
            """
                kotlin.targets["macos64"].compilations["main"].kotlinOptions.freeCompilerArgs += "-opt"
                kotlin.targets["macos64"].compilations["test"].kotlinOptions.freeCompilerArgs += "-g"
                kotlin.targets["linux64"].compilations["main"].kotlinOptions.freeCompilerArgs +=
                    listOf("-g", "-Xdisable-phases=Devirtualization,BuildDFG")
            """.trimIndent()
        )
        build("tasks") {
            assertSuccessful()
            assertContains(
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

    private fun getBootedSimulators(workingDirectory: File): Set<String>? =
        if (HostManager.hostIsMac) {
            val simulators = runProcess(listOf("xcrun", "simctl", "list"), workingDirectory, System.getenv()).also {
                assertTrue(it.isSuccessful, "xcrun exection failed")
            }.output

            simulators.split('\n').filter { it.contains("(Booted)") }.map { it.trim() }.toSet()
        } else {
            null
        }

    @Test
    fun testNativeTests() = with(Project("native-tests")) {
        setupWorkingDir()
        configureSingleNativeTarget()
        val testTasks = listOf("hostTest", "iosTest")
        val hostTestTask = "hostTest"

        val suffix = if (isWindows) "exe" else "kexe"

        val defaultOutputFile = "build/bin/host/debugTest/test.$suffix"
        val anotherOutputFile = "build/bin/host/anotherDebugTest/another.$suffix"

        val hostIsMac = HostManager.hostIsMac

        build("tasks") {
            assertSuccessful()
            testTasks.forEach {
                // We need to create tasks for all hosts
                assertTrue(output.contains("$it - "), "There is no test task '$it' in the task list.")
            }
        }

        // Perform all following checks in a single test to avoid running the K/N compiler several times.
        // Check that tests are not built during the ":assemble" execution
        build("assemble") {
            assertSuccessful()
            assertNoSuchFile(defaultOutputFile)
            assertNoSuchFile(anotherOutputFile)
        }

        val testsToExecute = mutableListOf(":$hostTestTask")
        if (hostIsMac) {
            testsToExecute.add(":iosTest")
        }
        val testsToSkip = testTasks.map { ":$it" } - testsToExecute

        // Store currently booted simulators to check that they don't leak (MacOS only).
        val bootedSimulatorsBefore = getBootedSimulators(projectDir)

        // Check the case when all tests pass.
        build("check") {
            assertSuccessful()

            assertTasksExecuted(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())

            assertContainsRegex("org\\.foo\\.test\\.TestKt\\.fooTest\\s+PASSED".toRegex())
            assertContainsRegex("org\\.foo\\.test\\.TestKt\\.barTest\\s+PASSED".toRegex())

            assertFileExists(defaultOutputFile)
        }

        // Check simulator process leaking.
        val bootedSimulatorsAfter = getBootedSimulators(projectDir)
        assertEquals(bootedSimulatorsBefore, bootedSimulatorsAfter)

        // Check the case with failed tests.
        projectDir.resolve("src/commonTest/kotlin/test.kt").appendText(
            """
                @Test
                fun fail() {
                    throw IllegalStateException("FAILURE!")
                }
            """.trimIndent()
        )
        build("check") {
            assertFailed()

            assertTasksFailed(":allTests")
            // In the aggregation report mode platform-specific tasks
            // are executed successfully even if there are failing tests.
            assertTasksExecuted(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())

            assertContainsRegex("org\\.foo\\.test\\.TestKt\\.fail\\s+FAILED".toRegex())
        }

        // Check that individual test reports are created correctly.
        build("check", "-Pkotlin.tests.individualTaskReports=true", "--continue") {
            assertFailed()

            // In the individual report mode platform-specific tasks
            // fail if there are failing tests.
            assertTasksFailed(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())


            fun assertStacktrace(taskName: String) {
                val testReport = projectDir.resolve("build/test-results/$taskName/TEST-org.foo.test.TestKt.xml")
                val stacktrace = SAXBuilder().build(testReport).rootElement
                    .getChildren("testcase")
                    .single { it.getAttribute("name").value == "fail" }
                    .getChild("failure")
                    .text
                assertTrue(stacktrace.contains("""at org\.foo\.test#fail\(.*test\.kt:24\)""".toRegex()))
            }

            fun assertTestResultsAnyOf(
                @TestDataFile assertionFile1Name: String,
                @TestDataFile assertionFile2Name: String,
                vararg testReportNames: String
            ) {
                try {
                    assertTestResults(assertionFile1Name, *testReportNames)
                } catch (e: AssertionError) {
                    assertTestResults(assertionFile2Name, *testReportNames)
                }
            }

            assertTestResults("testProject/native-tests/TEST-TestKt.xml", hostTestTask)
            // K/N doesn't report line numbers correctly on Linux (see KT-35408).
            // TODO: Uncomment when this is fixed.
            //assertStacktrace(hostTestTask)
            if (hostIsMac) {
                assertTestResultsAnyOf(
                    "testProject/native-tests/TEST-TestKt.xml",
                    "testProject/native-tests/TEST-TestKt-IOSsim.xml",
                    "iosTest"
                )
                assertStacktrace("iosTest")
            }
        }

        build("linkAnotherDebugTestHost") {
            assertSuccessful()
            assertFileExists(anotherOutputFile)
        }
    }

    @Test
    fun testNativeTestGetters() = with(Project("native-tests")) {
        // Check that test binaries can be accessed in a buildscript.
        build("checkNewGetters") {
            assertSuccessful()
            val suffixes = listOf("kexe")
            val names = listOf("test", "another")
            val files = names.flatMap { name ->
                suffixes.map { suffix ->
                    "$name.$suffix"
                }
            }

            files.forEach {
                assertContains("Get test: $it")
                assertContains("Find test: $it")
            }
        }

        // Check that accessing a test as an executable fails or returns null and shows the corresponding warning.
        build("checkOldGet") {
            assertFailed()
            assertContains(
                """
                    |Probably you are accessing the default test binary using the 'binaries.getExecutable("test", DEBUG)' method.
                    |Since 1.3.40 tests are represented by a separate binary type. To get the default test binary, use:
                    |
                    |    binaries.getTest("DEBUG")
                """.trimMargin()
            )
        }

        build("checkOldFind") {
            assertSuccessful()
            assertContains(
                """
                    |Probably you are accessing the default test binary using the 'binaries.findExecutable("test", DEBUG)' method.
                    |Since 1.3.40 tests are represented by a separate binary type. To get the default test binary, use:
                    |
                    |    binaries.findTest("DEBUG")
                """.trimMargin()
            )
            assertContains("Find test: null")
        }
    }

    @Test
    fun kt33750() {
        // Check that build fails if a test executable crashes.
        with(Project("native-tests")) {
            setupWorkingDir()
            projectDir.resolve("src/commonTest/kotlin/test.kt").appendText("\nval fail: Int = error(\"\")\n")
            build("check") {
                assertFailed()
                output.contains("exited with errors \\(exit code: \\d+\\)".toRegex())
            }
        }
    }

    @Test
    fun testCinterop() {
        with(transformProjectWithPluginsDsl("native-cinterop")) {
            configureSingleNativeTarget()

            build(":build") {
                assertSuccessful()
                assertFileExists("build/libs/host/main/native-cinterop-cinterop-number.klib")
                assertFileExists("build/classes/kotlin/host/main/native-cinterop-cinterop-number.klib")
                assertFileExists("build/classes/kotlin/host/test/native-cinterop_test.klib")
            }

            // Check that changing the compiler version in properties causes interop reprocessing and source recompilation.
            val hostLibraryTasks = listOf(
                ":cinteropNumberHost",
                ":compileKotlinHost"
            )
            build(":build") {
                assertSuccessful()
                assertTasksUpToDate(hostLibraryTasks)
            }

            build(*hostLibraryTasks.toTypedArray(), "-Porg.jetbrains.kotlin.native.version=1.3.70-dev-13258") {
                assertSuccessful()
                assertTasksExecuted(hostLibraryTasks)
            }
        }
    }

    @Test
    fun testNativeCompilerDownloading() {
        // The plugin shouldn't download the K/N compiler if there are no corresponding targets in the project.
        with(Project("sample-old-style-app", directoryPrefix = "new-mpp-lib-and-app")) {
            build("tasks") {
                assertSuccessful()
                assertFalse(output.contains("Kotlin/Native distribution: "))
            }
        }
        with(Project("native-libraries")) {
            build("tasks") {
                assertSuccessful()
                assertTrue(output.contains("Kotlin/Native distribution: "))
                // Check for KT-30258.
                assertFalse(output.contains("Deprecated Gradle features were used in this build, making it incompatible with Gradle 6.0."))
            }

            // This directory actually doesn't contain a K/N distribution
            // but we still can run a project configuration and check logs.
            val currentDir = projectDir.absolutePath
            build("tasks", "-Pkotlin.native.home=$currentDir") {
                assertSuccessful()
                assertContains("User-provided Kotlin/Native distribution: $currentDir")
                assertNotContains("Project property 'org.jetbrains.kotlin.native.home' is deprecated")
                assertContains(NO_NATIVE_STDLIB_WARNING)
                assertContains(NO_NATIVE_STDLIB_PROPERTY_WARNING)
            }

            // Deprecated property.
            build("tasks", "-Porg.jetbrains.kotlin.native.home=$currentDir", "-Pkotlin.native.nostdlib=true") {
                assertSuccessful()
                assertContains("User-provided Kotlin/Native distribution: $currentDir")
                assertContains("Project property 'org.jetbrains.kotlin.native.home' is deprecated")
                assertNotContains(NO_NATIVE_STDLIB_WARNING)
                assertNotContains(NO_NATIVE_STDLIB_PROPERTY_WARNING)
            }

            build("tasks", "-Pkotlin.native.version=1.3-eap-10779") {
                assertSuccessful()
                assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-(macos|linux|windows)-1\\.3-eap-10779".toRegex())
                assertNotContains("Project property 'org.jetbrains.kotlin.native.version' is deprecated")
            }

            // Deprecated property
            build("tasks", "-Porg.jetbrains.kotlin.native.version=1.3-eap-10779") {
                assertSuccessful()
                assertContainsRegex("Kotlin/Native distribution: .*kotlin-native-(macos|linux|windows)-1\\.3-eap-10779".toRegex())
                assertContains("Project property 'org.jetbrains.kotlin.native.version' is deprecated")
            }
        }

        // Gradle 5.0 introduced a new API for Ivy repository layouts.
        // MPP plugin uses this API to download K/N if Gradle version is >= 5.0.
        // Check this too (see KT-30258).
        with(Project("native-libraries")) {
            build("tasks", "-Pkotlin.native.version=1.3.50-eap-11606") {
                assertSuccessful()
                assertTrue(output.contains("Kotlin/Native distribution: "))
                assertFalse(output.contains("Deprecated Gradle features were used in this build, making it incompatible with Gradle 6.0."))
            }
        }
    }

    @Test
    fun testKt29725() {
        with(Project("native-libraries")) {
            // Assert that a project with a native target can be configured with Gradle 5.2
            build("tasks") {
                assertSuccessful()
            }
        }
    }

    @Test
    fun testIgnoreDisabledNativeTargets() = with(Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
        build {
            assertSuccessful()
            assertEquals(1, output.lines().count { DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX in it })
        }
        build("-P$DISABLED_NATIVE_TARGETS_REPORTER_DISABLE_WARNING_PROPERTY_NAME=true") {
            assertSuccessful()
            assertNotContains(DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX)
        }
    }

    @Test
    fun testNativeArgsWithSpaces() = with(Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
        setupWorkingDir()
        val compilcatedDirectoryName = if (HostManager.hostIsMingw) {
            // Windows doesn't allow creating a file with " in its name.
            "path with spaces"
        } else {
            "path with spaces and \""
        }

        val fileWithSpacesInPath = projectDir.resolve("src/commonMain/kotlin/$compilcatedDirectoryName")
            .apply { mkdirs() }
            .resolve("B.kt")
        fileWithSpacesInPath.writeText("fun foo() = 42")

        build("compileKotlin${nativeHostTargetName.capitalize()}") {
            assertSuccessful()
            checkNativeCommandLineFor(":compileKotlin${nativeHostTargetName.capitalize()}") {
                it.contains(fileWithSpacesInPath.absolutePath)
            }
        }
    }

}