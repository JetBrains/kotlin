/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.jdom.input.SAXBuilder
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_DISABLE_WARNING_PROPERTY_NAME
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX
import org.jetbrains.kotlin.gradle.internals.NO_NATIVE_STDLIB_PROPERTY_WARNING
import org.jetbrains.kotlin.gradle.internals.NO_NATIVE_STDLIB_WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.isWindows
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.KLIB_PROPERTY_SHORT_NAME
import org.jetbrains.kotlin.library.KLIB_PROPERTY_UNIQUE_NAME
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.util.*
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class NativeTargets(val current: String, val supported: List<String>, val unsupported: List<String>)

fun configure(): NativeTargets {
    val all = listOf("linux64", "macos64", "mingw64", "wasm32")

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

    val supported = all.filter { !unsupported.contains(it) }

    return NativeTargets(current, supported, unsupported)
}

class GeneralNativeIT : BaseGradleIT() {

    val nativeHostTargetName = configure().current

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

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
            transformProjectWithPluginsDsl("new-mpp-native-endorsed")
        ) {
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
    fun testCanProduceNativeLibraries() = with(Project("new-mpp-native-libraries")) {
        val baseName = "main"

        val sharedPrefix = CompilerOutputKind.DYNAMIC.prefix(HostManager.host)
        val sharedSuffix = CompilerOutputKind.DYNAMIC.suffix(HostManager.host)
        val sharedPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugShared/$sharedPrefix$baseName$sharedSuffix",
            "build/bin/$nativeHostTargetName/mainReleaseShared/$sharedPrefix$baseName$sharedSuffix"
        )

        val staticPrefix = CompilerOutputKind.STATIC.prefix(HostManager.host)
        val staticSuffix = CompilerOutputKind.STATIC.suffix(HostManager.host)
        val staticPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugStatic/$staticPrefix$baseName$staticSuffix",
            "build/bin/$nativeHostTargetName/mainReleaseStatic/$staticPrefix$baseName$staticSuffix"
        )

        val headerPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugShared/$sharedPrefix${baseName}_api.h",
            "build/bin/$nativeHostTargetName/mainReleaseShared/$sharedPrefix${baseName}_api.h",
            "build/bin/$nativeHostTargetName/mainDebugStatic/$staticPrefix${baseName}_api.h",
            "build/bin/$nativeHostTargetName/mainReleaseStatic/$staticPrefix${baseName}_api.h"
        )

        val klibPrefix = CompilerOutputKind.LIBRARY.prefix(HostManager.host)
        val klibSuffix = CompilerOutputKind.LIBRARY.suffix(HostManager.host)
        val klibPath = "${targetClassesDir(nativeHostTargetName)}${klibPrefix}native-lib$klibSuffix"

        val frameworkPrefix = CompilerOutputKind.FRAMEWORK.prefix(HostManager.host)
        val frameworkSuffix = CompilerOutputKind.FRAMEWORK.suffix(HostManager.host)
        val frameworkPaths = listOf(
            "build/bin/$nativeHostTargetName/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix.dSYM",
            "build/bin/$nativeHostTargetName/mainDebugFramework/$frameworkPrefix$baseName$frameworkSuffix",
            "build/bin/$nativeHostTargetName/mainReleaseFramework/$frameworkPrefix$baseName$frameworkSuffix"
        )
            .takeIf { HostManager.hostIsMac }
            .orEmpty()

        val taskSuffix = nativeHostTargetName.capitalize()
        val linkTasks = listOf(
            ":linkMainDebugShared$taskSuffix",
            ":linkMainReleaseShared$taskSuffix",
            ":linkMainDebugStatic$taskSuffix",
            ":linkMainReleaseStatic$taskSuffix"
        )

        val klibTask = ":compileKotlin$taskSuffix"

        val frameworkTasks = listOf(":linkMainDebugFramework$taskSuffix", ":linkMainReleaseFramework$taskSuffix")
            .takeIf { HostManager.hostIsMac }
            .orEmpty()

        // Building
        build("assemble") {
            assertSuccessful()

            sharedPaths.forEach { assertFileExists(it) }
            staticPaths.forEach { assertFileExists(it) }
            headerPaths.forEach { assertFileExists(it) }
            frameworkPaths.forEach { assertFileExists(it) }
            assertFileExists(klibPath)
        }

        // Test that all up-to date checks are correct
        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks)
            assertTasksUpToDate(frameworkTasks)
            assertTasksUpToDate(klibTask)
        }

        // Remove outputs and check that they are rebuilt.
        assertTrue(projectDir.resolve(headerPaths[0]).delete())
        if (HostManager.hostIsMac) {
            assertTrue(projectDir.resolve(frameworkPaths[0]).deleteRecursively())
        }

        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks.drop(1))
            assertTasksUpToDate(klibTask)
            assertTasksExecuted(linkTasks[0])

            if (HostManager.hostIsMac) {
                assertTasksUpToDate(frameworkTasks.drop(1))
                assertTasksExecuted(frameworkTasks[0])
            }
        }
    }

    @Test
    fun testNativeBinaryGroovyDSL() {
        // Building K/N binaries is very time-consuming. So we check building only for Kotlin DSL.
        // For Groovy DSl we just check that a project can be configured.
        val project = transformProjectWithPluginsDsl(
            "groovy-dsl", directoryPrefix = "new-mpp-native-binaries"
        )
        project.build("tasks") {
            assertSuccessful()

            // Check that getters work fine.
            val hostSuffix = nativeHostTargetName.capitalize()
            assertTrue(output.contains("Check link task: linkReleaseShared$hostSuffix"))
            assertTrue(output.contains("Check run task: runFooReleaseExecutable$hostSuffix"))
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
    fun testNativeBinaryKotlinDSL() = with(
        transformProjectWithPluginsDsl("kotlin-dsl", directoryPrefix = "new-mpp-native-binaries")
    ) {

        val hostSuffix = nativeHostTargetName.capitalize()
        val binaries = mutableListOf(
            "debugExecutable" to "native-binary",
            "releaseExecutable" to "native-binary",
            "fooDebugExecutable" to "foo",
            "fooReleaseExecutable" to "foo",
            "barReleaseExecutable" to "bar",
            "bazReleaseExecutable" to "my-baz",
            "test2ReleaseExecutable" to "test2",
            "releaseStatic" to "native_binary",
            "releaseShared" to "native_binary"
        )

        val linkTasks = binaries.map { (name, _) -> "link${name.capitalize()}$hostSuffix" }
        val outputFiles = binaries.map { (name, fileBaseName) ->
            val outputKind = NativeOutputKind.values().single { name.endsWith(it.taskNameClassifier, true) }.compilerOutputKind
            val prefix = outputKind.prefix(HostManager.host)
            val suffix = outputKind.suffix(HostManager.host)
            val fileName = "$prefix$fileBaseName$suffix"
            name to "build/bin/$nativeHostTargetName/$name/$fileName"
        }.toMap()

        val runTasks = listOf(
            "runDebugExecutable",
            "runReleaseExecutable",
            "runFooDebugExecutable",
            "runFooReleaseExecutable",
            "runBarReleaseExecutable",
            "runBazReleaseExecutable",
            "runTest2ReleaseExecutable"
        ).map { "$it$hostSuffix" }.toMutableList()

        val binariesTasks = arrayOf("${nativeHostTargetName}MainBinaries", "${nativeHostTargetName}TestBinaries")

        val compileTask = "compileKotlin$hostSuffix"
        val compileTestTask = "compileTestKotlin$hostSuffix"

        // Check that all link and run tasks are generated.
        build(*binariesTasks) {
            assertSuccessful()
            assertTasksExecuted(linkTasks.map { ":$it" })
            assertTasksExecuted(":$compileTask", ":$compileTestTask")
            outputFiles.forEach { (_, file) ->
                assertFileExists(file)
            }
            // Check that getters work fine.
            assertTrue(output.contains("Check link task: linkReleaseShared$hostSuffix"))
            assertTrue(output.contains("Check run task: runFooReleaseExecutable$hostSuffix"))

            // Check that dependency export works for static and shared libs.
            val staticSuffix = CompilerOutputKind.STATIC.suffix(HostManager.host)
            val sharedSuffix = CompilerOutputKind.DYNAMIC.suffix(HostManager.host)
            val staticPrefix = CompilerOutputKind.STATIC.prefix(HostManager.host)
            val sharedPrefix = CompilerOutputKind.DYNAMIC.prefix(HostManager.host)
            val staticHeader = outputFiles.getValue("releaseStatic").removeSuffix(staticSuffix) + "_api.h"
            val sharedHeader = outputFiles.getValue("releaseShared").removeSuffix(sharedSuffix) + "_api.h"
            assertTrue(fileInWorkingDir(staticHeader).readText().contains("${staticPrefix}native_binary_KInt (*exported)();"))
            assertTrue(fileInWorkingDir(sharedHeader).readText().contains("${sharedPrefix}native_binary_KInt (*exported)();"))
        }

        build("tasks") {
            assertSuccessful()
            runTasks.forEach {
                assertTrue(output.contains(it), "The 'tasks' output doesn't contain a task ${it}")
            }
        }

        // Clean the build to check that run tasks build corresponding binaries.
        build("clean") {
            assertSuccessful()
        }

        // Check that kotlinOptions work fine for a compilation.
        build(compileTask) {
            assertSuccessful()
            checkNativeCommandLineFor(":$compileTask") {
                assertTrue(it.contains("-verbose"))
            }
        }

        // Check that run tasks work fine and an entry point can be specified.
        build("runDebugExecutable$hostSuffix") {
            assertSuccessful()
            assertTrue(output.contains("<root>.main"))
        }

        build("runBazReleaseExecutable$hostSuffix") {
            assertSuccessful()
            assertTrue(output.contains("foo.main"))
        }

        build("runTest2ReleaseExecutable$hostSuffix") {
            assertSuccessful()
            assertTasksExecuted(":$compileTestTask")
            checkNativeCommandLineFor(":linkTest2ReleaseExecutable$hostSuffix") {
                assertTrue(it.contains("-tr"))
                assertTrue(it.contains("-Xtime"))
                // Check that kotlinOptions of the compilation don't affect the binary.
                assertFalse(it.contains("-verbose"))
                // Check that free args are still propagated to the binary (unlike other kotlinOptions, see KT-33717).
                // TODO: Reverse this check when the args are fully separated.
                assertTrue(it.contains("-nowarn"))
            }
            assertTrue(output.contains("tests.foo"))
        }

        // Check that we still have a default test task and it can be executed properly.
        build("${nativeHostTargetName}Test") {
            assertSuccessful()
            assertTrue(output.contains("tests.foo"))
        }

        if (HostManager.hostIsMac) {

            // Check dependency exporting and bitcode embedding in frameworks.
            // For release builds.
            build("linkReleaseFrameworkIos") {
                assertSuccessful()
                assertFileExists("build/bin/ios/releaseFramework/native_binary.framework")
                fileInWorkingDir("build/bin/ios/releaseFramework/native_binary.framework/Headers/native_binary.h")
                    .readText().contains("+ (int32_t)exported")
                // Check that by default release frameworks have bitcode embedded.
                checkNativeCommandLineFor(":linkReleaseFrameworkIos") {
                    assertTrue(it.contains("-Xembed-bitcode"))
                    assertTrue(it.contains("-opt"))
                }
            }

            // For debug builds.
            build("linkDebugFrameworkIos") {
                assertSuccessful()
                assertFileExists("build/bin/ios/debugFramework/native_binary.framework")
                assertTrue(
                    fileInWorkingDir("build/bin/ios/debugFramework/native_binary.framework/Headers/native_binary.h")
                        .readText()
                        .contains("+ (int32_t)exported")
                )
                // Check that by default debug frameworks have bitcode marker embedded.
                checkNativeCommandLineFor(":linkDebugFrameworkIos") {
                    assertTrue(it.contains("-Xembed-bitcode-marker"))
                    assertTrue(it.contains("-g"))
                }
            }

            // Check manual disabling bitcode embedding, custom command line args and building a static framework.
            build("linkCustomReleaseFrameworkIos") {
                assertSuccessful()
                checkNativeCommandLineFor(":linkCustomReleaseFrameworkIos") {
                    assertTrue(it.contains("-linker-option -L."))
                    assertTrue(it.contains("-Xtime"))
                    assertTrue(it.contains("-Xstatic-framework"))
                    assertFalse(it.contains("-Xembed-bitcode-marker"))
                    assertFalse(it.contains("-Xembed-bitcode"))
                }
            }

            // Check that bitcode is disabled for iOS simulator.
            build("linkReleaseFrameworkIosSim", "linkDebugFrameworkIosSim") {
                assertSuccessful()
                assertFileExists("build/bin/iosSim/releaseFramework/native_binary.framework")
                assertFileExists("build/bin/iosSim/debugFramework/native_binary.framework")
                checkNativeCommandLineFor(":linkReleaseFrameworkIosSim", ":linkDebugFrameworkIosSim") {
                    assertFalse(it.contains("-Xembed-bitcode"))
                    assertFalse(it.contains("-Xembed-bitcode-marker"))
                }
            }

            // Check that plugin doesn't allow exporting dependencies not added in the API configuration.
            val buildFile = listOf("build.gradle", "build.gradle.kts").map { projectDir.resolve(it) }.single { it.exists() }
            buildFile.modify {
                it.replace("api(project(\":exported\"))", "")
            }
            projectDir.resolve("src/commonMain/kotlin/PackageMain.kt").modify {
                // Remove usages of the ":exported" dependency to be able to compile the sources.
                it.checkedReplace("import com.example.exported", "").checkedReplace("val exp = exported()", "val exp = 42")
            }
            build("linkReleaseFrameworkIos") {
                assertFailed()
                val failureMsg = "Following dependencies exported in the releaseFramework binary " +
                        "are not specified as API-dependencies of a corresponding source set"
                assertTrue(output.contains(failureMsg))
            }
        }
    }

    // We propagate compilation args to link tasks for now (see KT-33717).
    // TODO: Reenable the test when the args are separated.
    @Ignore
    @Test
    fun testNativeFreeArgsWarning() = with(transformProjectWithPluginsDsl("kotlin-dsl", directoryPrefix = "new-mpp-native-binaries")) {
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
    fun testNativeTests() = with(Project("new-mpp-native-tests")) {
        val testTasks = listOf("macos64Test", "linux64Test", "mingw64Test", "iosTest")
        val hostTestTask = "${nativeHostTargetName}Test"

        val suffix = if (isWindows) "exe" else "kexe"

        val defaultOutputFile = "build/bin/$nativeHostTargetName/debugTest/test.$suffix"
        val anotherOutputFile = "build/bin/$nativeHostTargetName/anotherDebugTest/another.$suffix"

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

            assertTestResults("testProject/new-mpp-native-tests/TEST-TestKt.xml", hostTestTask)
            // K/N doesn't report line numbers correctly on Linux (see KT-35408).
            // TODO: Uncomment when this is fixed.
            //assertStacktrace(hostTestTask)
            if (hostIsMac) {
                assertTestResults("testProject/new-mpp-native-tests/TEST-TestKt.xml", "iosTest")
                assertStacktrace("iosTest")
            }
        }

        build("linkAnotherDebugTest${nativeHostTargetName}") {
            assertSuccessful()
            assertFileExists(anotherOutputFile)
        }
    }

    @Test
    fun testNativeTestGetters() = with(Project("new-mpp-native-tests")) {
        // Check that test binaries can be accessed in a buildscript.
        build("checkNewGetters") {
            assertSuccessful()
            val suffixes = listOf("exe", "kexe", "wasm")
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
        with(Project("new-mpp-native-tests")) {
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
        val libProject = Project("sample-lib", directoryPrefix = "new-mpp-lib-and-app")
        libProject.build("publish") {
            assertSuccessful()
        }
        val repo = libProject.projectDir.resolve("repo").absolutePath.replace('\\', '/')

        with(Project("new-mpp-native-cinterop")) {

            setupWorkingDir()
            listOf(gradleBuildScript(), gradleBuildScript("publishedLibrary")).forEach {
                it.appendText(
                    """
                    repositories {
                        maven { url '$repo' }
                    }
                """.trimIndent()
                )
            }

            val targetsToBuild = if (HostManager.hostIsMingw) {
                listOf(nativeHostTargetName, "mingw86")
            } else {
                listOf(nativeHostTargetName)
            }

            val libraryCinteropTasks = targetsToBuild.map { ":projectLibrary:cinteropStdio${it.capitalize()}" }
            val libraryCompileTasks = targetsToBuild.map { ":projectLibrary:compileKotlin${it.capitalize()}" }

            build(":projectLibrary:build") {
                assertSuccessful()
                assertTasksExecuted(libraryCinteropTasks)
                assertTrue(output.contains("Project test"), "No test output found")
                targetsToBuild.forEach {
                    assertFileExists("projectLibrary/build/classes/kotlin/$it/main/projectLibrary-cinterop-stdio.klib")
                }
            }

            build(":publishedLibrary:build", ":publishedLibrary:publish") {
                assertSuccessful()
                assertTasksExecuted(
                    targetsToBuild.map { ":publishedLibrary:cinteropStdio${it.capitalize()}" }
                )
                assertTrue(output.contains("Published test"), "No test output found")
                targetsToBuild.forEach {
                    assertFileExists("publishedLibrary/build/classes/kotlin/$it/main/publishedLibrary-cinterop-stdio.klib")
                    assertFileExists("publishedLibrary/build/classes/kotlin/$it/test/test-cinterop-stdio.klib")
                    assertFileExists("repo/org/example/publishedLibrary-$it/1.0/publishedLibrary-$it-1.0-cinterop-stdio.klib")
                }
            }

            build(":build") {
                assertSuccessful()
                assertTrue(output.contains("Dependent: Project print"), "No test output found")
                assertTrue(output.contains("Dependent: Published print"), "No test output found")
            }

            // Check that changing the compiler version in properties causes interop reprocessing and source recompilation.
            val hostLibraryTasks = listOf(
                ":projectLibrary:cinteropStdio${nativeHostTargetName.capitalize()}",
                ":projectLibrary:compileKotlin${nativeHostTargetName.capitalize()}"
            )
            build(":projectLibrary:build") {
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
        with(Project("new-mpp-native-libraries")) {
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
        with(Project("new-mpp-native-libraries")) {
            build("tasks", "-Pkotlin.native.version=1.3.50-eap-11606") {
                assertSuccessful()
                assertTrue(output.contains("Kotlin/Native distribution: "))
                assertFalse(output.contains("Deprecated Gradle features were used in this build, making it incompatible with Gradle 6.0."))
            }
        }
    }

    @Test
    fun testKt29725() {
        with(Project("new-mpp-native-libraries")) {
            // Assert that a project with a native target can be configured with Gradle 5.2
            build("tasks") {
                assertSuccessful()
            }
        }
    }

    @Test
    fun testIgnoreDisabledNativeTargets() = with(Project("sample-lib",  directoryPrefix = "new-mpp-lib-and-app")) {
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