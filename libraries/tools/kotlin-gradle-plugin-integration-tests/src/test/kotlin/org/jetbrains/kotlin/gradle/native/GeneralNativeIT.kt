/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import com.intellij.testFramework.TestDataFile
import org.gradle.util.GradleVersion
import org.jdom.input.SAXBuilder
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.chooseWrapperVersionOrFinishTest
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_DISABLE_WARNING_PROPERTY_NAME
import org.jetbrains.kotlin.gradle.internals.DISABLED_NATIVE_TARGETS_REPORTER_WARNING_PREFIX
import org.jetbrains.kotlin.gradle.internals.NO_NATIVE_STDLIB_PROPERTY_WARNING
import org.jetbrains.kotlin.gradle.internals.NO_NATIVE_STDLIB_WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.junit.*
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*
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
        HostManager.hostIsMac -> listOf("linuxMipsel32")
        else -> error("Unknown host")
    }

    val supported = listOf("linux64", "macos64", "mingw64").filter { !unsupported.contains(it) }
}

internal fun BaseGradleIT.transformNativeTestProject(
    projectName: String,
    wrapperVersion: GradleVersionRequired = defaultGradleVersion,
    directoryPrefix: String? = null
): BaseGradleIT.Project {
    val project = Project(projectName, wrapperVersion, directoryPrefix = directoryPrefix)
    project.setupWorkingDir()
    project.configureSingleNativeTarget()
    project.gradleProperties().apply {
        configureJvmMemory()
        disableKotlinNativeCaches()
    }
    return project
}

internal fun BaseGradleIT.transformNativeTestProjectWithPluginDsl(
    projectName: String,
    wrapperVersion: GradleVersionRequired = defaultGradleVersion,
    directoryPrefix: String? = null
): BaseGradleIT.Project {
    val project = transformProjectWithPluginsDsl(projectName, wrapperVersion, directoryPrefix = directoryPrefix)
    project.configureSingleNativeTarget()
    project.gradleProperties().apply {
        configureJvmMemory()
        disableKotlinNativeCaches()
    }
    return project
}

internal fun File.configureJvmMemory() {
    appendText("\norg.gradle.jvmargs=-Xmx1g\n")
}

internal fun File.disableKotlinNativeCaches() {
    appendText("\nkotlin.native.cacheKind=none\n")
}

private const val SINGLE_NATIVE_TARGET_PLACEHOLDER = "<SingleNativeTarget>"

private fun BaseGradleIT.Project.configureSingleNativeTarget(preset: String = HostManager.host.presetName) {
    projectDir.walk()
        .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
        .forEach { file ->
            file.modify {
                it.replace(SINGLE_NATIVE_TARGET_PLACEHOLDER, preset)
            }
        }
}

internal fun BaseGradleIT.BuildOptions.withCustomKonanDataDir(
    customKonanDataDir: File
) = this.copy(
    customEnvironmentVariables = this.customEnvironmentVariables +
            ("KONAN_DATA_DIR" to customKonanDataDir.absolutePath)
)

class GeneralNativeIT : BaseGradleIT() {

    val nativeHostTargetName = MPPNativeTargets.current

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT

    @Test
    fun testParallelExecutionSmoke(): Unit = with(transformNativeTestProjectWithPluginDsl("native-parallel")) {
        // Check that the K/N compiler can be started in-process in parallel.
        build(":one:compileKotlinLinux", ":two:compileKotlinLinux") {
            assertSuccessful()
        }
    }

    @Test
    fun testIncorrectDependenciesWarning() = with(transformNativeTestProject("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
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
            transformNativeTestProjectWithPluginDsl("native-endorsed")
        ) {
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
    fun testCanProduceNativeLibraries() = with(transformNativeTestProjectWithPluginDsl("libraries", directoryPrefix = "native-binaries")) {
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
        val klibPath = "${targetClassesDir("host")}${klibPrefix}/klib/native-library$klibSuffix"

        val linkTasks = listOf(
            ":linkDebugSharedHost",
            ":linkDebugStaticHost",
            ":linkReleaseSharedHost",
            ":linkReleaseStaticHost",
        )

        val klibTask = ":compileKotlinHost"

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
    }

    @Test
    fun testCanProvideNativeFrameworkArtifact() = with(
        transformNativeTestProjectWithPluginDsl("frameworks", directoryPrefix = "native-binaries")
    ) {
        Assume.assumeTrue(HostManager.hostIsMac)

        gradleBuildScript().appendText(
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

        build(":validateThinArtifacts") {
            assertSuccessful()
        }

        build(":validateFatArtifacts") {
            assertSuccessful()
        }

        build(":validateCustomAttributesSetting") {
            assertSuccessful()
        }
    }

    @Test
    fun testCanProduceNativeFrameworks() = with(
        transformNativeTestProjectWithPluginDsl("frameworks", directoryPrefix = "native-binaries")
    ) {
        Assume.assumeTrue(HostManager.hostIsMac)

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
        build("assemble") {
            assertSuccessful()
            headerPaths.forEach { assertFileExists(it) }
            frameworkPaths.forEach { assertFileExists(it) }

            fileInWorkingDir(headerPaths[0]).readText().contains("+ (int32_t)exported")

            // Check that by default release frameworks have bitcode embedded.
            withNativeCommandLineArguments(":linkMainReleaseFrameworkIos") { arguments ->
                assertTrue("-Xembed-bitcode" in arguments)
                assertTrue("-opt" in arguments)
            }
            // Check that by default debug frameworks have bitcode marker embedded.
            withNativeCommandLineArguments(":linkMainDebugFrameworkIos") { arguments ->
                assertTrue("-Xembed-bitcode-marker" in arguments)
                assertTrue("-g" in arguments)
            }
            // Check that bitcode can be disabled by setting custom compiler options
            withNativeCommandLineArguments(":linkCustomDebugFrameworkIos") { arguments ->
                assertTrue(arguments.containsSequentially("-linker-option", "-L."))
                assertTrue("-Xtime" in arguments)
                assertTrue("-Xstatic-framework" in arguments)
                assertFalse("-Xembed-bitcode-marker" in arguments)
                assertFalse("-Xembed-bitcode" in arguments)
            }
            // Check that bitcode is disabled for iOS simulator.
            withNativeCommandLineArguments(":linkMainReleaseFrameworkIosSim", ":linkMainDebugFrameworkIosSim") { arguments ->
                assertFalse("-Xembed-bitcode" in arguments)
                assertFalse("-Xembed-bitcode-marker" in arguments)
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
    fun testExportApiOnlyToLibraries() {
        val project = transformNativeTestProjectWithPluginDsl("libraries", directoryPrefix = "native-binaries")

        testExportApi(
            project, listOf(
                ExportApiTestData("linkDebugSharedHost", "debugShared"),
                ExportApiTestData("linkDebugStaticHost", "debugStatic"),
            )
        )
    }

    @Test
    fun testExportApiOnlyToFrameworks() {
        Assume.assumeTrue(HostManager.hostIsMac)
        val project = transformNativeTestProjectWithPluginDsl("frameworks", directoryPrefix = "native-binaries")

        testExportApi(
            project, listOf(
                ExportApiTestData("linkMainDebugFrameworkIos", "mainDebugFramework")
            )
        )
    }

    private data class ExportApiTestData(val taskName: String, val binaryName: String)

    private fun testExportApi(project: Project, testData: List<ExportApiTestData>) = with(project) {
        // Check that plugin doesn't allow exporting dependencies not added in the API configuration.
        gradleBuildScript().modify {
            it.replace("api(project(\":exported\"))", "")
        }

        fun failureMsgFor(binaryName: String) =
            "Following dependencies exported in the $binaryName binary are not specified as API-dependencies of a corresponding source set"

        testData.forEach {
            build(it.taskName) {
                assertFailed()
                assertContains(failureMsgFor(it.binaryName))
            }
        }
    }

    @Test
    fun testNativeExecutables() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        val binaries = mutableListOf(
            "debugExecutable" to "native-binary",
            "releaseExecutable" to "native-binary",
            "bazDebugExecutable" to "my-baz",
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
            "runBazDebugExecutable",
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

        build("runBazDebugExecutableHost") {
            assertSuccessful()
            assertTrue(output.contains("foo.main"))
        }
    }

    private fun testNativeBinaryDsl(project: String) = with(
        transformNativeTestProjectWithPluginDsl(project, directoryPrefix = "native-binaries")
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
        transformNativeTestProjectWithPluginDsl("native-kotlin-options")
    ) {
        build(":compileKotlinHost") {
            assertSuccessful()
            withNativeCommandLineArguments(":compileKotlinHost") { arguments ->
                assertFalse("-verbose" in arguments)
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
            withNativeCommandLineArguments(":compileKotlinHost") { arguments ->
                assertTrue("-verbose" in arguments)
            }
        }
    }

    // We propagate compilation args to link tasks for now (see KT-33717).
    // TODO: Reenable the test when the args are separated.
    @Ignore
    @Test
    fun testNativeFreeArgsWarning() = with(transformNativeTestProjectWithPluginDsl("kotlin-dsl", directoryPrefix = "native-binaries")) {
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
    fun testNativeTests() = with(transformNativeTestProject("native-tests")) {
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

        checkTestsUpToDate(testsToExecute, testsToSkip)

        // Check simulator process leaking.
        val bootedSimulatorsAfter = getBootedSimulators(projectDir)
        assertEquals(bootedSimulatorsBefore, bootedSimulatorsAfter)

        // Check the case with failed tests.
        checkFailedTests(hostTestTask, testsToExecute, testsToSkip)

        build("linkAnotherDebugTestHost") {
            assertSuccessful()
            assertFileExists(anotherOutputFile)
        }
    }

    private fun Project.checkTestsUpToDate(testsToExecute: List<String>, testsToSkip: List<String>) {
        // Check that test tasks are up-to-date on second run
        build("check") {
            assertSuccessful()

            assertTasksUpToDate(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())
        }

        // Check that setting new value to tracked environment variable triggers tests rerun
        build("check", options = defaultBuildOptions().copy(withDaemon = false, androidHome = projectDir)) {
            assertSuccessful()

            assertTasksExecuted(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())
        }

        build("check", options = defaultBuildOptions().copy(withDaemon = false, androidHome = projectDir)) {
            assertSuccessful()

            assertTasksUpToDate(*testsToExecute.toTypedArray())
            assertTasksSkipped(*testsToSkip.toTypedArray())
        }
    }

    private fun Project.checkFailedTests(hostTestTask: String, testsToExecute: List<String>, testsToSkip: List<String>) {
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


            fun assertStacktrace(taskName: String, targetName: String) {
                val testReport = projectDir.resolve("build/test-results/$taskName/TEST-org.foo.test.TestKt.xml")
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
                vararg testReportNames: String
            ) {
                try {
                    assertTestResults(assertionFile1Name, *testReportNames)
                } catch (e: AssertionError) {
                    assertTestResults(assertionFile2Name, *testReportNames)
                }
            }

            // Gradle 6.6+ slightly changed format of xml test results
            // If, in the test project, preset name was updated,
            // update accordingly test result output for Gradle6.6+
            val testGradleVersion = project.chooseWrapperVersionOrFinishTest()
            val expectedHostTestResult: String
            val expectedIOSTestResults: List<String>
            if (GradleVersion.version(testGradleVersion) < GradleVersion.version("6.6")) {
                expectedHostTestResult = "testProject/native-tests/TEST-TestKt_pre6.6.xml"
                expectedIOSTestResults = listOf(
                    "testProject/native-tests/TEST-TestKt_pre6.6.xml",
                    "testProject/native-tests/TEST-TestKt-iOSsim_pre6.6.xml",
                )
            } else {
                expectedHostTestResult = "testProject/native-tests/TEST-TestKt.xml"
                expectedIOSTestResults = listOf(
                    "testProject/native-tests/TEST-TestKt-iOSsim.xml",
                    "testProject/native-tests/TEST-TestKt-iOSArm64sim.xml",
                )
            }

            assertTestResults(expectedHostTestResult, hostTestTask)
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

    @Test
    fun testNativeTestGetters() = with(transformNativeTestProject("native-tests")) {
        // Check that test binaries can be accessed in a buildscript.
        build("checkNewGetters") {
            assertSuccessful()
            val suffix = if (HostManager.hostIsMingw) "exe" else "kexe"
            val names = listOf("test", "another")
            val files = names.map { "$it.$suffix" }

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
        with(transformNativeTestProject("native-tests")) {
            projectDir.resolve("src/commonTest/kotlin/test.kt").appendText("\nval fail: Int = error(\"\")\n")
            build("check") {
                assertFailed()
                output.contains("exited with errors \\(exit code: \\d+\\)".toRegex())
            }
        }
    }

    @Test
    fun testCinterop() = with(transformNativeTestProjectWithPluginDsl("native-cinterop")) {
        fun libraryFiles(projectName: String, cinteropName: String) = listOf(
            "$projectName/build/classes/kotlin/host/main/cinterop/${projectName}-cinterop-$cinteropName.klib",
            "$projectName/build/classes/kotlin/host/main/klib/${projectName}.klib",
            "$projectName/build/classes/kotlin/host/test/klib/${projectName}_test.klib",
        )

        // Enable info log to see cinterop environment variables.
        build(":projectLibrary:build", "--info") {
            assertSuccessful()
            assertTasksExecuted(":projectLibrary:cinteropAnotherNumberHost")
            libraryFiles("projectLibrary", "anotherNumber").forEach { assertFileExists(it) }
            withNativeCustomEnvironment(":projectLibrary:cinteropAnotherNumberHost", toolName = "cinterop") { env ->
                assertEquals("1", env["LIBCLANG_DISABLE_CRASH_RECOVERY"])
            }
        }

        build(":publishedLibrary:build", ":publishedLibrary:publish") {
            assertSuccessful()
            assertTasksExecuted(":publishedLibrary:cinteropNumberHost")
            libraryFiles("publishedLibrary", "number").forEach { assertFileExists(it) }
        }

        build(":build") {
            assertSuccessful()
        }
    }

    @Test
    fun testCompilerVersionChange() = with(transformNativeTestProjectWithPluginDsl("native-compiler-version")) {
        val compileTasks = listOf(":compileKotlinHost")
        val compileTasksArray = compileTasks.toTypedArray()

        build(*compileTasksArray) {
            assertSuccessful()
            assertTasksExecuted(compileTasks)
        }

        build(*compileTasksArray) {
            assertSuccessful()
            assertTasksUpToDate(compileTasks)
        }

        // Check that changing K/N version lead to tasks rerun
        build(*compileTasksArray, "-Porg.jetbrains.kotlin.native.version=1.5.30") {
            assertSuccessful()
            assertTasksExecuted(compileTasks)
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

            build("tasks", "-Pkotlin.native.version=1.5.20-dev-5613") {
                assertSuccessful()
                assertContainsRegex(
                    "Kotlin/Native distribution: .*kotlin-native-prebuilt-(macos|linux|windows)-1\\.5\\.20-dev-5613"
                        .toRegex()
                )
                assertNotContains("Project property 'org.jetbrains.kotlin.native.version' is deprecated")
            }

            // Deprecated property
            build("tasks", "-Porg.jetbrains.kotlin.native.version=1.5.20-dev-5613") {
                assertSuccessful()
                assertContainsRegex(
                    "Kotlin/Native distribution: .*kotlin-native-prebuilt-(macos|linux|windows)-1\\.5\\.20-dev-5613"
                        .toRegex()
                )
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
    fun testNativeArgsWithSpaces() = with(transformNativeTestProject("sample-lib", directoryPrefix = "new-mpp-lib-and-app")) {
        val complicatedDirectoryName = if (HostManager.hostIsMingw) {
            // Windows doesn't allow creating a file with " in its name.
            "path with spaces"
        } else {
            "path with spaces and \""
        }

        val fileWithSpacesInPath = projectDir.resolve("src/commonMain/kotlin/$complicatedDirectoryName")
            .apply { mkdirs() }
            .resolve("B.kt")
        fileWithSpacesInPath.writeText("fun foo() = 42")

        build("compileKotlin${nativeHostTargetName.capitalize()}") {
            assertSuccessful()
            withNativeCommandLineArguments(":compileKotlin${nativeHostTargetName.capitalize()}") { arguments ->
                val escapedQuotedPath =
                    "\"${fileWithSpacesInPath.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"")}\""
                assertTrue(
                    escapedQuotedPath in arguments,
                    """
                        Command-line arguments do not contain path with spaces.
                        Raw path = ${fileWithSpacesInPath.absolutePath}
                        Escaped quoted path = $escapedQuotedPath
                        Arguments: ${arguments.joinToString(separator = " ")}
                    """.trimIndent()
                )
            }
        }
    }

    @Test
    fun testBinaryOptionsDSL() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        gradleBuildScript().appendText(
            """
                kotlin.targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java) {
                    binaries.all { binaryOptions["memoryModel"] = "experimental" }
                }
            """.trimIndent()
        )
        build(":linkDebugExecutableHost") {
            assertSuccessful()
            withNativeCommandLineArguments(":linkDebugExecutableHost") {
                assertTrue(it.contains("-Xbinary=memoryModel=experimental"))
            }
        }
    }

    @Test
    fun testBinaryOptionsProperty() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        build(":linkDebugExecutableHost", "-Pkotlin.native.binary.memoryModel=experimental") {
            assertSuccessful()
            withNativeCommandLineArguments(":linkDebugExecutableHost") {
                assertTrue(it.contains("-Xbinary=memoryModel=experimental"))
            }
        }
    }

    @Test
    fun testBinaryOptionsPriority() = with(transformNativeTestProjectWithPluginDsl("executables", directoryPrefix = "native-binaries")) {
        gradleBuildScript().appendText(
            """
                kotlin.targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java) {
                    binaries.all { binaryOptions["memoryModel"] = "experimental" }
                }
            """.trimIndent()
        )
        build(":linkDebugExecutableHost", "-Pkotlin.native.binary.memoryModel=strict") {
            assertSuccessful()
            withNativeCommandLineArguments(":linkDebugExecutableHost") {
                // Options set in the DSL have higher priority than options set in project properties.
                assertTrue(it.contains("-Xbinary=memoryModel=experimental"))
            }
        }
    }

    @Test
    fun testCinteropConfigurationsVariantAwareResolution() = with(transformNativeTestProjectWithPluginDsl("native-cinterop")) {
        build(":publishedLibrary:publish") {
            assertSuccessful()
        }

        fun CompiledProject.assertVariantInDependencyInsight(variantName: String) {
            try {
                assertContains("variant \"$variantName\" [")
            } catch (originalError: AssertionError) {
                val matchedVariants = Regex("variant \"(.*?)\" \\[").findAll(output).toList()
                throw AssertionError(
                    "Expected variant $variantName. " +
                            if (matchedVariants.isNotEmpty())
                                "Matched instead: " + matchedVariants.joinToString { it.groupValues[1] }
                            else "No match.",
                    originalError
                )
            }
        }

        build(":dependencyInsight", "--configuration", "hostTestTestNumberCInterop", "--dependency", "org.example:publishedLibrary") {
            assertSuccessful()
            assertVariantInDependencyInsight("hostApiElements-published")
        }

        gradleBuildScript("projectLibrary").appendText(
            "\n" + """
            configurations.create("ktlint") {
                def bundlingAttribute = Attribute.of("org.gradle.dependency.bundling", String)
                attributes.attribute(bundlingAttribute, "external")
            }
        """.trimIndent()
        )

        build(":dependencyInsight", "--configuration", "hostTestTestNumberCInterop", "--dependency", ":projectLibrary") {
            assertSuccessful()
            assertVariantInDependencyInsight("hostCInteropApiElements")
        }
        build(":dependencyInsight", "--configuration", "hostCompileKlibraries", "--dependency", ":projectLibrary") {
            assertSuccessful()
            assertVariantInDependencyInsight("hostApiElements")
        }
    }

    @Test
    fun `check offline mode is propagated to the compiler`() = with(
        transformNativeTestProjectWithPluginDsl(
            "executables",
            directoryPrefix = "native-binaries"
        )
    ) {
        val buildOptions = defaultBuildOptions()

        val linkTask = ":linkDebugExecutableHost"
        val compileTask = ":compileKotlinHost"

        build(linkTask, options = buildOptions) {
            assertSuccessful()
        }

        // Check that --offline works when all the dependencies are already downloaded:
        val buildOptionsOffline = buildOptions.copy(freeCommandLineArgs = buildOptions.freeCommandLineArgs + "--offline")

        build("clean", linkTask, options = buildOptionsOffline) {
            assertSuccessful()
            withNativeCommandLineArguments(compileTask, linkTask) {
                assertTrue(it.contains("-Xoverride-konan-properties=airplaneMode=true"))
            }
        }

        // Check that --offline fails when there are no downloaded dependencies:
        run {
            val customKonanDataDir = tempDir.newFolder()
            val buildOptionsOfflineWithCustomKonanDataDir = buildOptionsOffline.withCustomKonanDataDir(customKonanDataDir)

            build("clean", linkTask, options = buildOptionsOfflineWithCustomKonanDataDir) {
                assertFailed()
                assertTasksNotExecuted(listOf(linkTask))
            }

            checkNoDependenciesDownloaded(customKonanDataDir)
        }

        // Check that the compiler is not extracted if it is not cached:
        run {
            val customKonanDataDir = tempDir.newFolder()
            val buildOptionsOfflineWithCustomKonanDataDir = buildOptionsOffline.withCustomKonanDataDir(customKonanDataDir)
            build(
                "clean", linkTask, "-Pkotlin.native.version=1.6.20-M1-9999",
                options = buildOptionsOfflineWithCustomKonanDataDir
            ) {
                assertFailed()
                assertTasksNotExecuted(listOf(linkTask, compileTask))
            }

            assertTrue(customKonanDataDir.listFiles().isNullOrEmpty())
        }
    }

    private fun checkNoDependenciesDownloaded(customKonanDataDir: File) {
        // Check that no files have actually been downloaded or extracted,
        // except for maybe the compiler itself, which can be extracted from the Gradle cache
        // (see NativeCompilerDownloader, it uses regular dependency resolution,
        // so supports --offline properly by default).
        val cacheDirName = "cache"
        val dependenciesDirName = "dependencies"

        fun assertDirectoryHasNothingButMaybe(directory: File, vararg names: String) {
            assertEquals(emptyList(), directory.listFiles().orEmpty().map { it.name } - names)
        }

        assertDirectoryHasNothingButMaybe(File(customKonanDataDir, cacheDirName), ".lock")
        assertDirectoryHasNothingButMaybe(File(customKonanDataDir, dependenciesDirName), ".extracted")

        val customKonanDataDirFiles = customKonanDataDir.listFiles().orEmpty().map { it.name } - setOf(cacheDirName, dependenciesDirName)
        if (customKonanDataDirFiles.isNotEmpty()) {
            assertEquals(1, customKonanDataDirFiles.size, message = customKonanDataDirFiles.toString())
            assertTrue(customKonanDataDirFiles.single().startsWith("kotlin-native-"), message = customKonanDataDirFiles.single())
        }
    }

    @Test
    fun `check offline mode is propagated to the cinterop`() = with(transformNativeTestProjectWithPluginDsl("native-cinterop")) {
        val buildOptions = defaultBuildOptions()
        val cinteropTask = ":projectLibrary:cinteropAnotherNumberHost"

        build(cinteropTask, options = buildOptions) {
            assertSuccessful()
        }

        // Check that --offline works when all the dependencies are already downloaded:
        val buildOptionsOffline = buildOptions.copy(freeCommandLineArgs = buildOptions.freeCommandLineArgs + "--offline")

        build("clean", cinteropTask, options = buildOptionsOffline) {
            assertSuccessful()
            withNativeCommandLineArguments(cinteropTask, toolName = "cinterop") {
                assertTrue(it.containsSequentially("-Xoverride-konan-properties", "airplaneMode=true"))
            }
        }

        // Check that --offline fails when there are no downloaded dependencies:
        run {
            val customKonanDataDir = tempDir.newFolder()
            val buildOptionsOfflineWithCustomKonanDataDir = buildOptionsOffline.withCustomKonanDataDir(customKonanDataDir)

            build("clean", cinteropTask, options = buildOptionsOfflineWithCustomKonanDataDir) {
                assertFailed()
            }

            checkNoDependenciesDownloaded(customKonanDataDir)
        }

        // Check that the compiler is not extracted if it is not cached:
        run {
            val customKonanDataDir = tempDir.newFolder()
            val buildOptionsOfflineWithCustomKonanDataDir = buildOptionsOffline.withCustomKonanDataDir(customKonanDataDir)

            build(
                "clean", cinteropTask, "-Pkotlin.native.version=1.6.20-M1-9999",
                options = buildOptionsOfflineWithCustomKonanDataDir
            ) {
                assertFailed()
                assertTasksNotExecuted(listOf(cinteropTask))
            }

            assertTrue(customKonanDataDir.listFiles().isNullOrEmpty())
        }
    }

    companion object {
        fun List<String>.containsSequentially(vararg elements: String): Boolean {
            check(elements.isNotEmpty())
            return Collections.indexOfSubList(this, elements.toList()) != -1
        }

        private enum class NativeToolSettingsKind(val title: String) {
            COMPILER_CLASSPATH("Classpath"),
            COMMAND_LINE_ARGUMENTS("Arguments"),
            CUSTOM_ENV_VARIABLES("Custom ENV variables")
        }

        private fun CompiledProject.extractNativeToolSettings(
            toolName: String,
            taskPath: String?,
            settingsKind: NativeToolSettingsKind
        ): Sequence<String> {
            val settingsPrefix = "${settingsKind.title} = ["
            val settings = output.lineSequence()
                .run {
                    if (taskPath != null) dropWhile { "Executing actions for task '$taskPath'" !in it }.drop(1) else this
                }
                .dropWhile {
                    check(taskPath == null || "Executing actions for task" !in it) { "Unexpected log line with new Gradle task: $it" }
                    "Run in-process tool \"$toolName\"" !in it && "Run \"$toolName\" tool in a separate JVM process" !in it
                }
                .drop(1)
                .dropWhile {
                    check(taskPath == null || "Executing actions for task" !in it) { "Unexpected log line with new Gradle task: $it" }
                    settingsPrefix !in it
                }

            val settingsHeader = settings.firstOrNull()
            check(settingsHeader != null && settingsPrefix in settingsHeader) {
                "Cannot find setting '${settingsKind.title}' for task ${taskPath}"
            }

            return if (settingsHeader.trimEnd().endsWith(']'))
                emptySequence() // No parameters.
            else
                settings.drop(1).map { it.trim() }.takeWhile { it != "]" }
        }

        fun CompiledProject.extractNativeCommandLineArguments(taskPath: String? = null, toolName: String): List<String> =
            extractNativeToolSettings(toolName, taskPath, NativeToolSettingsKind.COMMAND_LINE_ARGUMENTS).toList()

        fun CompiledProject.extractNativeCompilerClasspath(taskPath: String? = null, toolName: String): List<String> =
            extractNativeToolSettings(toolName, taskPath, NativeToolSettingsKind.COMPILER_CLASSPATH).toList()

        fun CompiledProject.extractNativeCustomEnvironment(taskPath: String? = null, toolName: String): Map<String, String> =
            extractNativeToolSettings(toolName, taskPath, NativeToolSettingsKind.CUSTOM_ENV_VARIABLES).map {
                val (key, value) = it.split("=")
                key.trim() to value.trim()
            }.toMap()

        fun CompiledProject.withNativeCommandLineArguments(
            vararg taskPaths: String,
            toolName: String = "konanc",
            check: (List<String>) -> Unit
        ) = taskPaths.forEach { taskPath -> check(extractNativeCommandLineArguments(taskPath, toolName)) }

        fun CompiledProject.withNativeCompilerClasspath(
            vararg taskPaths: String,
            toolName: String = "konanc",
            check: (List<String>) -> Unit
        ) = taskPaths.forEach { taskPath -> check(extractNativeCompilerClasspath(taskPath, toolName)) }

        fun CompiledProject.withNativeCustomEnvironment(
            vararg taskPaths: String,
            toolName: String = "konanc",
            check: (Map<String, String>) -> Unit
        ) = taskPaths.forEach { taskPath -> check(extractNativeCustomEnvironment(taskPath, toolName)) }

        @field:ClassRule
        @JvmField
        val tempDir = TemporaryFolder()

        @JvmStatic
        @AfterClass
        fun deleteTempDir() {
            tempDir.delete()
        }
    }
}