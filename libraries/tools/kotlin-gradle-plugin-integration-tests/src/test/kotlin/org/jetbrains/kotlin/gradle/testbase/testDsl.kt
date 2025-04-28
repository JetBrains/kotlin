/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.gradle.model.ModelContainer
import org.jetbrains.kotlin.gradle.model.ModelFetcherBuildAction
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.util.isTeamCityRun
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.io.File
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Create a new test project.
 *
 * @param [projectName] test project name in `src/test/resources/testProject` directory.
 * @param [buildOptions] common Gradle build options
 * @param [buildJdk] path to JDK build should run with. *Note* Only append to 'gradle.properties'!
 * @param [enableKotlinDaemonMemoryLimitInMb] limit max heap size for Kotlin Daemon.
 * `null` enables the default limit inherited from the Gradle process.
 */
@OptIn(EnvironmentalVariablesOverride::class)
fun KGPBaseTest.project(
    projectName: String,
    gradleVersion: GradleVersion,
    buildOptions: BuildOptions = defaultBuildOptions,
    enableBuildScan: Boolean = false,
    addHeapDumpOptions: Boolean = true,
    enableGradleDebug: EnableGradleDebug = EnableGradleDebug.AUTO,
    enableGradleDaemonMemoryLimitInMb: Int? = 1024,
    enableKotlinDaemonMemoryLimitInMb: Int? = 1024,
    projectPathAdditionalSuffix: String = "",
    buildJdk: File? = null,
    localRepoDir: Path? = defaultLocalRepo(gradleVersion),
    environmentVariables: EnvironmentalVariables = EnvironmentalVariables(),
    dependencyManagement: DependencyManagement = DependencyManagement.DefaultDependencyManagement(),
    test: TestProject.() -> Unit = {},
): TestProject {
    val projectPath = setupProjectFromTestResources(
        projectName,
        gradleVersion,
        workingDir,
        projectPathAdditionalSuffix,
    )
    projectPath.addDefaultSettingsToSettingsGradle(
        gradleVersion,
        dependencyManagement,
        localRepoDir,
        buildOptions.isolatedProjects.toBooleanFlag(gradleVersion)
    )
    projectPath.enableCacheRedirector()
    projectPath.enableAndroidSdk()
    if (buildOptions.languageVersion != null || buildOptions.languageApiVersion != null) {
        projectPath.applyKotlinCompilerArgsPlugin()
    }

    val gradleRunner = GradleRunner
        .create()
        .withProjectDir(projectPath.toFile())
        .withTestKitDir(testKitDir.toAbsolutePath().toFile())
        .withGradleVersion(gradleVersion.version)

    val testProject = TestProject(
        gradleRunner = gradleRunner,
        projectName = projectName,
        projectPath = projectPath,
        buildOptions = buildOptions,
        gradleVersion = gradleVersion,
        enableBuildScan = enableBuildScan,
        enableGradleDebug = enableGradleDebug,
        enableGradleDaemonMemoryLimitInMb = enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb = enableKotlinDaemonMemoryLimitInMb,
        environmentVariables = environmentVariables
    )
    addHeapDumpOptions.ifTrue { testProject.addHeapDumpOptions() }
    localRepoDir?.let { testProject.configureLocalRepository(localRepoDir) }
    if (buildJdk != null) testProject.setupNonDefaultJdk(buildJdk)

    testProject.customizeProject()

    val result = runCatching {
        testProject.test()
    }
    // A convenient place to place a breakpoint to be able to inspect project output files
    result.getOrThrow()
    return testProject
}

/**
 * Create a new test project with configuring single native target.
 *
 * @param [projectName] test project name in `src/test/resources/testProject` directory.
 * @param [buildOptions] common Gradle build options
 * @param [buildJdk] path to JDK build should run with. *Note* Only append to 'gradle.properties'!
 * @param [enableKotlinDaemonMemoryLimitInMb] limit max heap size for Kotlin Daemon.
 * `null` enables the default limit inherited from the Gradle process.
 */
@OptIn(EnvironmentalVariablesOverride::class)
fun KGPBaseTest.nativeProject(
    projectName: String,
    gradleVersion: GradleVersion,
    buildOptions: BuildOptions = defaultBuildOptions,
    enableBuildScan: Boolean = false,
    dependencyManagement: DependencyManagement = DependencyManagement.DefaultDependencyManagement(),
    addHeapDumpOptions: Boolean = true,
    enableGradleDebug: EnableGradleDebug = EnableGradleDebug.AUTO,
    enableGradleDaemonMemoryLimitInMb: Int? = 1024,
    enableKotlinDaemonMemoryLimitInMb: Int? = 1024,
    projectPathAdditionalSuffix: String = "",
    buildJdk: File? = null,
    localRepoDir: Path? = null,
    environmentVariables: EnvironmentalVariables = EnvironmentalVariables(),
    configureSubProjects: Boolean = false,
    test: TestProject.() -> Unit = {},
): TestProject {
    val project = project(
        projectName = projectName,
        gradleVersion = gradleVersion,
        buildOptions = buildOptions,
        enableBuildScan = enableBuildScan,
        dependencyManagement = dependencyManagement,
        addHeapDumpOptions = addHeapDumpOptions,
        enableGradleDebug = enableGradleDebug,
        enableGradleDaemonMemoryLimitInMb = enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb = enableKotlinDaemonMemoryLimitInMb,
        projectPathAdditionalSuffix = projectPathAdditionalSuffix,
        buildJdk = buildJdk,
        localRepoDir = localRepoDir,
        environmentVariables = environmentVariables,
    )
    project.configureSingleNativeTarget(configureSubProjects)
    project.test()
    return project
}

/**
 * Trigger test project build with given [buildArguments] and assert build is successful.
 */
fun TestProject.build(
    vararg buildArguments: String,
    enableGradleDebug: EnableGradleDebug = this.enableGradleDebug,
    kotlinDaemonDebugPort: Int? = this.kotlinDaemonDebugPort,
    enableBuildCacheDebug: Boolean = false,
    enableBuildScan: Boolean = this.enableBuildScan,
    enableGradleDaemonMemoryLimitInMb: Int? = this.enableGradleDaemonMemoryLimitInMb,
    enableKotlinDaemonMemoryLimitInMb: Int? = this.enableKotlinDaemonMemoryLimitInMb,
    buildOptions: BuildOptions = this.buildOptions,
    environmentVariables: EnvironmentalVariables = this.environmentVariables,
    inputStream: InputStream? = null,
    forwardBuildOutput: Boolean = enableGradleDebug.toBooleanFlag(),
    gradleRunnerAction: GradleRunner.() -> BuildResult = GradleRunner::build,
    assertions: BuildResult.() -> Unit = {},
) = buildWithAction(
    parameters = BuildParameterization(
        buildArguments = buildArguments.toList(),
        enableGradleDebug = enableGradleDebug,
        kotlinDaemonDebugPort = kotlinDaemonDebugPort,
        enableBuildCacheDebug = enableBuildCacheDebug,
        enableBuildScan = enableBuildScan,
        buildOptions = buildOptions,
        enableGradleDaemonMemoryLimitInMb = enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb = enableKotlinDaemonMemoryLimitInMb,
        environmentVariables = environmentVariables,
        forwardBuildOutput = forwardBuildOutput,
    ),
    assertions = assertions,
    inputStream = inputStream,
    action = gradleRunnerAction,
)

/**
 * Trigger test project build with given [buildArguments] and assert build is failed.
 */
fun TestProject.buildAndFail(
    vararg buildArguments: String,
    enableGradleDebug: EnableGradleDebug = this.enableGradleDebug,
    kotlinDaemonDebugPort: Int? = this.kotlinDaemonDebugPort,
    enableBuildCacheDebug: Boolean = false,
    enableBuildScan: Boolean = this.enableBuildScan,
    buildOptions: BuildOptions = this.buildOptions,
    enableGradleDaemonMemoryLimitInMb: Int? = this.enableGradleDaemonMemoryLimitInMb,
    enableKotlinDaemonMemoryLimitInMb: Int? = this.enableKotlinDaemonMemoryLimitInMb,
    environmentVariables: EnvironmentalVariables = this.environmentVariables,
    inputStream: InputStream? = null,
    forwardBuildOutput: Boolean = enableGradleDebug.toBooleanFlag(),
    assertions: BuildResult.() -> Unit = {},
) = buildWithAction(
    parameters = BuildParameterization(
        buildArguments = buildArguments.toList(),
        enableGradleDebug = enableGradleDebug,
        kotlinDaemonDebugPort = kotlinDaemonDebugPort,
        enableBuildCacheDebug = enableBuildCacheDebug,
        enableBuildScan = enableBuildScan,
        buildOptions = buildOptions,
        enableGradleDaemonMemoryLimitInMb = enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb = enableKotlinDaemonMemoryLimitInMb,
        environmentVariables = environmentVariables,
        forwardBuildOutput = forwardBuildOutput,
    ),
    assertions = assertions,
    inputStream = inputStream,
    action = GradleRunner::buildAndFail,
)

private data class BuildParameterization(
    val buildArguments: List<String>,
    val enableGradleDebug: EnableGradleDebug,
    val kotlinDaemonDebugPort: Int?,
    val enableBuildCacheDebug: Boolean,
    val enableBuildScan: Boolean,
    val buildOptions: BuildOptions,
    val enableGradleDaemonMemoryLimitInMb: Int?,
    val enableKotlinDaemonMemoryLimitInMb: Int?,
    val environmentVariables: EnvironmentalVariables,
    /** If `true`, enable [GradleRunner.forwardOutput]. */
    val forwardBuildOutput: Boolean = enableGradleDebug.toBooleanFlag(),
)

private fun TestProject.buildWithAction(
    parameters: BuildParameterization,
    inputStream: InputStream?,
    assertions: BuildResult.() -> Unit = {},
    action: GradleRunner.() -> BuildResult = GradleRunner::build,
) {
    with(parameters) {
        if (enableBuildScan) agreeToBuildScanService()
        ensureKotlinCompilerArgumentsPluginAppliedCorrectly(buildOptions)

        val runWithDebug = enableGradleDebug.toBooleanFlag()
        if (runWithDebug && isTeamCityRun) {
            fail("Please don't set `enableGradleDebug = true` in teamcity run, this can fail build")
        }

        val connectSubprocessVMToDebugger =
            runWithDebug && (environmentVariables.overridingEnvironmentVariablesInstantiationBacktrace != null || buildOptions.continuousBuild == true)
        val allBuildArguments = commonBuildSetup(
            buildArguments = buildArguments,
            buildOptions = buildOptions,
            enableBuildCacheDebug = enableBuildCacheDebug,
            enableBuildScan = enableBuildScan,
            enableGradleDaemonMemoryLimitInMb = enableGradleDaemonMemoryLimitInMb,
            enableKotlinDaemonMemoryLimitInMb = enableKotlinDaemonMemoryLimitInMb,
            connectSubprocessVMToDebugger = connectSubprocessVMToDebugger,
            gradleVersion = gradleVersion,
            kotlinDaemonDebugPort = kotlinDaemonDebugPort
        )
        val gradleRunnerForBuild = gradleRunner
            .also { if (forwardBuildOutput) it.forwardOutput() }
            .also { if (environmentVariables.environmentalVariables.isNotEmpty()) it.withEnvironment(System.getenv() + environmentVariables.environmentalVariables) }
            .withDebug(runWithDebug && !connectSubprocessVMToDebugger)
            .withArguments(allBuildArguments)

        inputStream?.let {
            (gradleRunnerForBuild as DefaultGradleRunner).withStandardInput(it)
        }

        withBuildSummary(allBuildArguments) {
            val buildResult = if (connectSubprocessVMToDebugger) {
                validateDebuggingSocketIsListeningForTestsWithEnv(
                    runCatching {
                        gradleRunnerForBuild.action()
                    },
                    environmentVariables.overridingEnvironmentVariablesInstantiationBacktrace
                )
            } else {
                gradleRunnerForBuild.action()
            }
            if (enableBuildScan) buildResult.printBuildScanUrl()
            assertions(buildResult)
            buildResult.additionalAssertions(buildOptions)
        }
    }
}

fun getGradleUserHome(): File {
    return testKitDir.toAbsolutePath().toFile().normalize().absoluteFile
}

private fun validateDebuggingSocketIsListeningForTestsWithEnv(
    buildResult: Result<BuildResult>,
    overridingEnvironmentVariablesInstantiationBacktrace: Throwable?,
): BuildResult {
    if (buildResult.isSuccess) {
        return buildResult.getOrThrow()
    }
    val exception = buildResult.exceptionOrNull()!!
    val exceptionMessage = exception.fullMessage

    if (!exceptionMessage.contains("AGENT_ERROR_TRANSPORT_INIT")) {
        // This is not a debugger connection issue
        throw exception
    }

    fail(
        buildString {
            appendLine(
                """
                ⚠ withDebug failed to connect to test that was overriding environment variables
                
                To debug a test that runs with environment variables:
                    1. Create run configuration "Remote JVM Debug". Select Debugger Mode: "Listen to remote JVM" and check "Auto restart"
                    2. Specify Host: ${EnableGradleDebug.LOOPBACK_IP} and Port: ${EnableGradleDebug.PORT_FOR_DEBUGGING_KGP_IT_WITH_ENVS}
                    3. Run this run configuration and then run the test under debugger
                """.trimIndent()
            )
            appendLine()
            appendLine("JVM connection failed at ${EnableGradleDebug.LOOPBACK_IP}:${EnableGradleDebug.PORT_FOR_DEBUGGING_KGP_IT_WITH_ENVS} with:")
            appendLine(exceptionMessage)
            appendLine()
            appendLine("Environment variables instantiated at:")
            overridingEnvironmentVariablesInstantiationBacktrace?.backtrace()?.lineSequence()?.drop(1)?.forEach {
                appendLine("  ${it}")
            }
            appendLine()
        }
    )
}

private fun BuildResult.additionalAssertions(buildOptions: BuildOptions) {
    if (buildOptions.warningMode != WarningMode.Fail && buildOptions.warningMode != WarningMode.None) {
        assertDeprecationWarningsArePresent(buildOptions.warningMode)
    }
}

private fun TestProject.ensureKotlinCompilerArgumentsPluginAppliedCorrectly(buildOptions: BuildOptions) {
    if (this.buildOptions.languageVersion != null || this.buildOptions.languageApiVersion != null) return // plugin is applied
    // plugin's not applied
    check(buildOptions.languageVersion == null && buildOptions.languageApiVersion == null) {
        "Kotlin language or API version passed on the build level, but the plugin wasn't applied"
    }
}

internal inline fun <reified T> TestProject.getModels(
    crossinline assertions: ModelContainer<T>.() -> Unit,
) {
    val allBuildArguments = commonBuildSetup(
        buildArguments = emptyList(),
        buildOptions = buildOptions,
        enableBuildCacheDebug = false,
        enableBuildScan = enableBuildScan,
        enableGradleDaemonMemoryLimitInMb = enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb = enableKotlinDaemonMemoryLimitInMb,
        connectSubprocessVMToDebugger = false,
        gradleVersion = gradleVersion
    )

    val connector = GradleConnector
        .newConnector()
        .useGradleUserHomeDir(getGradleUserHome())
        .useGradleVersion(gradleVersion.version)
        .forProjectDirectory(projectPath.toAbsolutePath().toFile())

    connector.connect().use {
        assertions(
            it
                .action(ModelFetcherBuildAction(T::class.java))
                .withArguments(allBuildArguments)
                .run()
        )
    }
}

fun TestProject.enableLocalBuildCache(
    buildCacheLocation: Path,
) {
    val settingsFile = if (Files.exists(settingsGradle)) settingsGradle else settingsGradleKts

    settingsFile.append(
        """
        buildCache {
            local {
                directory = "${buildCacheLocation.toUri()}"
            }
        }
        """.trimIndent()
    )
}

fun TestProject.enableStatisticReports(
    type: BuildReportType,
    url: String?,
) {
    gradleProperties.append(
        "\nkotlin.build.report.output=${type.name}\n"
    )

    url?.also {
        gradleProperties.append(
            "\nkotlin.build.report.http.url=$url\n"
        )
    }
}

fun String.wrapIntoBlock(s: String): String =
    """
        |$s {
        |    $this
        |}
        """.trimMargin()

open class GradleProject(
    val projectName: String,
    val projectPath: Path,
) {
    val buildGradle: Path get() = projectPath.resolve("build.gradle")
    val buildGradleKts: Path get() = projectPath.resolve("build.gradle.kts")
    val settingsGradle: Path get() = projectPath.resolve("settings.gradle")
    val settingsGradleKts: Path get() = projectPath.resolve("settings.gradle.kts")
    val gradleProperties: Path get() = projectPath.resolve("gradle.properties")
    val buildFileNames: Set<String> get() = setOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")

    fun classesDir(
        sourceSet: String = "main",
        targetName: String? = null,
        language: String = "kotlin",
    ): Path = projectPath.resolve("build/classes/$language/${targetName.orEmpty()}/$sourceSet/")

    fun kotlinClassesDir(
        sourceSet: String = "main",
        targetName: String? = null,
    ): Path = classesDir(sourceSet, targetName, language = "kotlin")

    fun javaClassesDir(
        sourceSet: String = "main",
    ): Path = classesDir(sourceSet, language = "java")

    fun kotlinSourcesDir(
        sourceSet: String = "main",
    ): Path = projectPath.resolve("src/$sourceSet/kotlin")

    fun javaSourcesDir(
        sourceSet: String = "main",
    ): Path = projectPath.resolve("src/$sourceSet/java")

    fun relativeToProject(
        files: List<Path>,
    ): List<Path> = files.map { projectPath.relativize(it) }

    private var counter = 0
    fun generateIdentifier(): String {
        return counter.toString().also { counter += 1 }
    }

    fun markAsUsingInjections() {
        usesInjections = true
    }

    var usesInjections = false
        private set
}

/**
 * You need at least Gradle "7.0" for supporting environment variables with Gradle runner
 */
class EnvironmentalVariables @EnvironmentalVariablesOverride constructor(
    val environmentalVariables: Map<String, String> = emptyMap(),
) {
    val overridingEnvironmentVariablesInstantiationBacktrace: Throwable? = if (environmentalVariables.isNotEmpty()) Throwable() else null

    @EnvironmentalVariablesOverride
    constructor(vararg environmentVariables: Pair<String, String>) : this(mapOf(*environmentVariables))
}

@RequiresOptIn("Environmental variables override may lead to interference of parallel builds")
annotation class EnvironmentalVariablesOverride

@OptIn(EnvironmentalVariablesOverride::class)
class TestProject(
    val gradleRunner: GradleRunner,
    projectName: String,
    projectPath: Path,
    val buildOptions: BuildOptions,
    val gradleVersion: GradleVersion,
    val enableBuildScan: Boolean,
    val enableGradleDaemonMemoryLimitInMb: Int?,
    val enableKotlinDaemonMemoryLimitInMb: Int?,
    val enableGradleDebug: EnableGradleDebug,
    /**
     * A port to debug the Kotlin daemon at.
     * Note that we'll need to let the debugger start listening at this port first *before* the Kotlin daemon is launched.
     */
    val kotlinDaemonDebugPort: Int? = null,
    val environmentVariables: EnvironmentalVariables = EnvironmentalVariables(),
) : GradleProject(projectName, projectPath) {
    fun subProject(name: String) = GradleProject(name, projectPath.resolve(name))

    /**
     * Includes another project as a submodule in the current project.
     *
     * - Copies the other project to a directory inside this project.
     * - Updates this project's `settings.gradle(.kts)` with `include(":$newSubmoduleName")`
     *
     * @param otherProjectName The name of the other project to include as a submodule.
     * @param pathPrefix An optional prefix to prepend to the submodule's path. Defaults to an empty string.
     * @param newSubmoduleName An optional new name for the submodule. Defaults to [otherProjectName].
     * @param isKts Whether to update a `settings.gradle.kts` instead of a `settings.gradle` file. Defaults to `false`.
     */
    fun includeOtherProjectAsSubmodule(
        otherProjectName: String,
        pathPrefix: String = "",
        newSubmoduleName: String = otherProjectName,
        isKts: Boolean = settingsGradleKts.exists(),
        localRepoDir: Path? = null,
        configure: GradleProject.() -> Unit = {},
    ) {
        val otherProjectPath = if (pathPrefix.isEmpty()) {
            otherProjectName.testProjectPath
        } else {
            "$pathPrefix/$otherProjectName".testProjectPath
        }
        otherProjectPath.copyRecursively(projectPath.resolve(newSubmoduleName))

        val gradleSettingToUpdate = if (isKts) settingsGradleKts else settingsGradle

        gradleSettingToUpdate.append(
            """

            include(":$newSubmoduleName")
            """.trimIndent()
        )

        localRepoDir?.let { subProject(newSubmoduleName).configureLocalRepository(localRepoDir) }
        subProject(newSubmoduleName).configure()
    }

    fun includeOtherProjectAsIncludedBuild(
        otherProjectName: String,
        pathPrefix: String,
        newProjectName: String = otherProjectName,
    ) {
        val otherProjectPath = "$pathPrefix/$otherProjectName".testProjectPath
        otherProjectPath.copyRecursively(projectPath.resolve(newProjectName))

        projectPath.resolve(newProjectName).addDefaultSettingsToSettingsGradle(gradleVersion)

        if (settingsGradle.exists()) {
            settingsGradle.append(
                """

                    includeBuild '$newProjectName'
                """.trimIndent()
            )
        } else {
            settingsGradleKts.append(
                """

                    includeBuild("$newProjectName")
                """.trimIndent()
            )
        }
    }

    /**
     * Copies the contents of a directory from the test data of another project into this project's directory structure.
     *
     * @param otherProjectName The name of the other project whose directory is copied from its test data.
     */
    fun embedDirectoryFromTestData(otherProjectName: String, destination: String = otherProjectName) {
        val otherProjectPath = otherProjectName.testProjectPath
        otherProjectPath.copyRecursively(projectPath.resolve(destination))
    }
}

private fun commonBuildSetup(
    buildArguments: List<String>,
    buildOptions: BuildOptions,
    enableBuildCacheDebug: Boolean,
    enableBuildScan: Boolean,
    enableGradleDaemonMemoryLimitInMb: Int?,
    enableKotlinDaemonMemoryLimitInMb: Int?,
    connectSubprocessVMToDebugger: Boolean,
    gradleVersion: GradleVersion,
    kotlinDaemonDebugPort: Int? = null,
): List<String> {
    val jdkLocations = allJdkProperties
        .map { System.getProperty(it) }
        .sortedWith(compareBy { it.toString() })
        .joinToString(separator = ",")

    val gradleJvmOptions =
        collectGradleJvmOptions(enableGradleDaemonMemoryLimitInMb, buildOptions.fileLeaksReportFile, connectSubprocessVMToDebugger)
    val kotlinDaemonJvmArgs = collectKotlinJvmArgs(enableKotlinDaemonMemoryLimitInMb, kotlinDaemonDebugPort)

    /**
     * Encloses each argument into double quotes to properly handle values with whitespaces based on [enclose] value
     */
    fun List<String>.joinToJvmArgsString(enclose: Boolean = true) = if (enclose) {
        joinToString(separator = "\" \"", prefix = "\"", postfix = "\"")
    } else {
        joinToString(separator = " ")
    }

    return buildOptions.toArguments(gradleVersion) + buildArguments + listOfNotNull(
        // Required toolchains should be pre-installed via repo. Tests should not download any JDKs
        "-Porg.gradle.java.installations.auto-download=false",
        "-Porg.gradle.java.installations.auto-detect=false",
        "-Porg.gradle.java.installations.paths=$jdkLocations",
        // Disable automatic download of android SDK.
        // It should be downloaded in dependencies/android-sdk to enable caching and prevent sdk installation failures.
        "-Pandroid.builder.sdkDownload=false",
        // Decreasing Gradle daemon idle timeout to 1 min from default 3 hours.
        // This should help with OOM on CI when agents do not have enough free memory available.
        "-Dorg.gradle.daemon.idletimeout=60000",
        if (gradleJvmOptions.isNotEmpty()) {
            "-Dorg.gradle.jvmargs=${gradleJvmOptions.joinToJvmArgsString()}"
        } else null,
        if (enableBuildCacheDebug) "-Dorg.gradle.caching.debug=true" else null,
        if (enableBuildScan) "--scan" else null,
        if (kotlinDaemonJvmArgs.isNotEmpty()) {
            // do not enclose as KGP transforms arguments like "-Xmx1024m" to -"-Xmx1024m": KT-72870
            "-Pkotlin.daemon.jvmargs=${kotlinDaemonJvmArgs.joinToJvmArgsString(enclose = false)}"
        } else null,
        // Configure a non-default directory to be able to track Kotlin daemons started from the tests
        // Useful for the tests like KGPDaemonsBaseTest
        if (buildOptions.customKotlinDaemonRunFilesDirectory != null) {
            "-D${CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property}=${buildOptions.customKotlinDaemonRunFilesDirectory.absolutePath}"
        } else null,
    )
}

private fun collectGradleJvmOptions(
    enableGradleDaemonMemoryLimitInMb: Int?,
    useFileLeakDetectorToFile: File?,
    connectSubprocessVMToDebugger: Boolean,
): List<String> = buildList {
    if (useFileLeakDetectorToFile != null) {
        val fileLeakDetector = File("src/test/resources/common/file-leak-detector-1.15-jar-with-dependencies.jar")
        add("-javaagent:${fileLeakDetector.absolutePath}=trace=${useFileLeakDetectorToFile.absolutePath}")
    }
    // Limiting Gradle daemon heap size to reduce memory pressure on CI agents
    if (enableGradleDaemonMemoryLimitInMb != null) {
        add("-Xmx${enableGradleDaemonMemoryLimitInMb}m")
    }
    /**
     * This mode is used to debug the target project when the target project has environment variables. There should be someone listening
     * for the debugee on [EnableGradleDebug.LOOPBACK_IP]:[EnableGradleDebug.PORT_FOR_DEBUGGING_KGP_IT_WITH_ENVS] which we check
     * in [validateDebuggingSocketIsListeningForTestsWithEnv]
     */
    if (connectSubprocessVMToDebugger) {
        add("-agentlib:jdwp=transport=dt_socket,server=n,suspend=n,address=${EnableGradleDebug.LOOPBACK_IP}:${EnableGradleDebug.PORT_FOR_DEBUGGING_KGP_IT_WITH_ENVS}")
    }
}

private fun collectKotlinJvmArgs(
    enableKotlinDaemonMemoryLimitInMb: Int?,
    kotlinDaemonDebugPort: Int?,
): List<String> = buildList {
    if (enableKotlinDaemonMemoryLimitInMb != null) {
        // Limiting Kotlin daemon heap size to reduce memory pressure on CI agents
        add("-Xmx${enableKotlinDaemonMemoryLimitInMb}m")
    }
    if (kotlinDaemonDebugPort != null) {
        // Note that we pass "server=n", meaning that we'll need to let the debugger start listening at this port first *before* the
        // Kotlin daemon is launched. That is usually easier than trying to attach the debugger when the Kotlin daemon is launched
        // (currently if we don't attach fast enough, the Kotlin daemon will fail to launch).
        add("-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=$kotlinDaemonDebugPort")
    }
}


private fun TestProject.withBuildSummary(
    buildArguments: List<String>,
    run: () -> Unit,
) {
    try {
        run()
    } catch (t: Throwable) {
        println("<=== Test build: $projectName ===>")
        println("<=== Using Gradle version: ${gradleVersion.version} ===>")
        println("<=== Run arguments: ${buildArguments.joinToString()} ===>")
        println("<=== Project path:  ${projectPath.toAbsolutePath()} ===>")
        throw t
    }
}

/**
 * This property is configured to read konan from specific directory.
 */
private const val konanDataDirForIntegrationTests = "konanDataDirForIntegrationTests"
val konanDir
    get() =
        System.getProperty(konanDataDirForIntegrationTests)?.let { Paths.get(it) }
            ?: error("Please specify a shared konan directory using \"${konanDataDirForIntegrationTests}\" system property")

/**
 * On changing test kit dir location update related location in 'cleanTestKitCache' task.
 */
internal val testKitDir get() = Paths.get(".").resolve("build").resolve("testKitCache")

/**
 * Use this directory to store some cross-test information, such as [BuildOptions.customKotlinDaemonRunFilesDirectory]
 *
 * Should be preferred over [testKitDir] to avoid potetial clashes.
 *
 * On changing this directory, update related location in 'cleanTestKitCache' task.
 */
internal val kgpTestInfraWorkingDirectory get() = Paths.get(".").resolve("build").resolve("kgpTestInfra")

private val hashAlphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
private fun randomHash(length: Int = 15): String {
    return List(length) { hashAlphabet.random() }.joinToString("")
}

private fun setupProjectFromTestResources(
    projectName: String,
    gradleVersion: GradleVersion,
    tempDir: Path,
    optionalSubDir: String,
): Path {
    val testProjectPath = projectName.testProjectPath
    assertTrue("Test project doesn't exists") { Files.exists(testProjectPath) }
    assertTrue("Test project path isn't a directory") { Files.isDirectory(testProjectPath) }

    return tempDir
        .resolve(gradleVersion.version)
        .resolve(randomHash())
        .resolve(projectName)
        .resolve(optionalSubDir)
        .also {
            testProjectPath.copyRecursively(it)
        }
}

private val String.testProjectPath: Path get() = Paths.get("src", "test", "resources", "testProject", this)

internal fun Path.addDefaultSettingsToSettingsGradle(
    gradleVersion: GradleVersion,
    dependencyManagement: DependencyManagement = DependencyManagement.DefaultDependencyManagement(),
    localRepo: Path? = null,
    projectIsolationEnabled: Boolean = false,
) {
    addPluginManagementToSettings()
    when (dependencyManagement) {
        is DependencyManagement.DefaultDependencyManagement -> {
            // we cannot switch to dependencyManagement before Gradle 8.1 because of KT-65708
            if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_1) && !projectIsolationEnabled) {
                addDependencyRepositoriesToBuildScript(
                    additionalDependencyRepositories = dependencyManagement.additionalRepos,
                    localRepo = localRepo
                )
            } else {
                addDependencyManagementToSettings(
                    additionalDependencyRepositories = dependencyManagement.additionalRepos,
                    localRepo = localRepo
                )
            }
        }
        is DependencyManagement.DisabledDependencyManagement -> {}
    }
}

private fun Path.addDependencyRepositoriesToBuildScript(
    additionalDependencyRepositories: Set<String>,
    localRepo: Path? = null,
) {
    val buildGradle = resolve("build.gradle")
    val buildGradleKts = resolve("build.gradle.kts")
    val settingsGradle = resolve("settings.gradle")
    val settingsGradleKts = resolve("settings.gradle.kts")
    when {
        Files.exists(buildGradle) -> buildGradle.modify {
            it.insertBlockToBuildScriptAfterPluginsAndImports(
                getGroovyRepositoryBlock(additionalDependencyRepositories, localRepo).wrapWithAllProjectBlock()
            )
        }

        Files.exists(buildGradleKts) -> buildGradleKts.modify {
            it.insertBlockToBuildScriptAfterPluginsAndImports(
                getKotlinRepositoryBlock(additionalDependencyRepositories, localRepo).wrapWithAllProjectBlock()
            )
        }

        Files.exists(settingsGradle) -> buildGradle.toFile()
            .writeText(
                getGroovyRepositoryBlock(
                    additionalDependencyRepositories,
                    localRepo
                ).wrapWithAllProjectBlock()
            )

        Files.exists(settingsGradleKts) -> buildGradleKts.toFile()
            .writeText(
                getKotlinRepositoryBlock(
                    additionalDependencyRepositories,
                    localRepo
                ).wrapWithAllProjectBlock()
            )

        else -> error("No build-file or settings file found")
    }

    if (Files.exists(resolve("buildSrc"))) {
        resolve("buildSrc").addDependencyRepositoriesToBuildScript(additionalDependencyRepositories, localRepo)
    }
}

private fun String.wrapWithAllProjectBlock(): String =
    """
    |
    |allprojects {
    |    $this
    |}
    |
    """.trimMargin()

internal fun String.insertBlockToBuildScriptAfterPluginsAndImports(blockToInsert: String): String {
    val importsPattern = Regex("^import.*$", RegexOption.MULTILINE)
    val pluginsBlockPattern = Regex("plugins\\s*\\{[^}]*}", RegexOption.DOT_MATCHES_ALL)

    val lastImportIndex = importsPattern.findAll(this).map { it.range.last }.maxOrNull()
    val pluginBlockEndIndex = pluginsBlockPattern.find(this)?.range?.last

    val insertionIndex = listOfNotNull(lastImportIndex, pluginBlockEndIndex).maxOrNull() ?: return blockToInsert + this

    return StringBuilder(this).insert(insertionIndex + 1, "\n$blockToInsert\n").toString()
}

internal fun String.insertBlockToBuildScriptAfterImports(blockToInsert: String): String {
    val importsPattern = Regex("^import.*$", RegexOption.MULTILINE)

    val lastImportIndex = importsPattern.findAll(this).map { it.range.last }.maxOrNull() ?: return blockToInsert + this
    return StringBuilder(this).insert(lastImportIndex + 1, "\n$blockToInsert\n").toString()
}


internal fun Path.addPluginManagementToSettings() {
    val buildGradle = resolve("build.gradle")
    val buildGradleKts = resolve("build.gradle.kts")
    val settingsGradle = resolve("settings.gradle")
    val settingsGradleKts = resolve("settings.gradle.kts")
    when {
        Files.exists(settingsGradle) -> settingsGradle.modify {
            if (!it.contains("pluginManagement {")) {
                """
                |$DEFAULT_GROOVY_SETTINGS_FILE
                |
                |$it
                |""".trimMargin()
            } else {
                it
            }
        }

        Files.exists(settingsGradleKts) -> settingsGradleKts.modify {
            if (!it.contains("pluginManagement {")) {
                """
                |$DEFAULT_KOTLIN_SETTINGS_FILE
                |
                |$it
                """.trimMargin()
            } else {
                it
            }
        }

        Files.exists(buildGradle) -> settingsGradle.toFile().writeText(DEFAULT_GROOVY_SETTINGS_FILE)

        Files.exists(buildGradleKts) -> settingsGradleKts.toFile().writeText(DEFAULT_KOTLIN_SETTINGS_FILE)

        else -> error("No build-file or settings file found")
    }

    if (Files.exists(resolve("buildSrc"))) {
        resolve("buildSrc").addPluginManagementToSettings()
    }
}


internal fun Path.addDependencyManagementToSettings(
    gradleRepositoriesMode: RepositoriesMode = RepositoriesMode.PREFER_SETTINGS,
    additionalDependencyRepositories: Set<String>,
    localRepo: Path? = null,
) {
    val buildGradle = resolve("build.gradle")
    val buildGradleKts = resolve("build.gradle.kts")
    val settingsGradle = resolve("settings.gradle")
    val settingsGradleKts = resolve("settings.gradle.kts")
    when {
        Files.exists(settingsGradle) -> settingsGradle.modify {
            if (!it.contains("dependencyManagement {")) {
                """
                |$it
                |
                |${
                    getGroovyDependencyManagementBlock(
                        gradleRepositoriesMode,
                        additionalDependencyRepositories,
                        localRepo
                    )
                }
                """.trimMargin()
            } else {
                it
            }
        }

        Files.exists(settingsGradleKts) -> settingsGradleKts.modify {
            if (!it.contains("dependencyManagement {")) {
                """
                |$it
                |
                |${
                    getKotlinDependencyManagementBlock(
                        gradleRepositoriesMode,
                        additionalDependencyRepositories,
                        localRepo
                    )
                }
                """.trimMargin()
            } else {
                it
            }
        }

        Files.exists(buildGradle) -> settingsGradle.toFile()
            .writeText(
                getGroovyDependencyManagementBlock(
                    gradleRepositoriesMode,
                    additionalDependencyRepositories,
                    localRepo
                )
            )

        Files.exists(buildGradleKts) -> settingsGradleKts.toFile()
            .writeText(
                getKotlinDependencyManagementBlock(
                    gradleRepositoriesMode,
                    additionalDependencyRepositories,
                    localRepo
                )
            )

        else -> error("No build-file or settings file found")
    }

    if (Files.exists(resolve("buildSrc"))) {
        resolve("buildSrc").addDependencyManagementToSettings(gradleRepositoriesMode, additionalDependencyRepositories, localRepo)
    }
}

private fun TestProject.agreeToBuildScanService() {
    val settingsFile = if (Files.exists(settingsGradle)) settingsGradle else settingsGradleKts
    settingsFile.append(
        """
            
        gradleEnterprise {
            buildScan {
                termsOfServiceUrl = "https://gradle.com/terms-of-service"
                termsOfServiceAgree = "yes"
            }
        }

        """.trimIndent()
    )
}

private fun BuildResult.printBuildScanUrl() {
    val buildScanUrl = output
        .lineSequence()
        .firstOrNull { it.contains("https://gradle.com/s/") }
        ?.replaceBefore("https://gradle", "")
    if (buildScanUrl != null) {
        println("Build scan url: $buildScanUrl")
    } else {
        // It is ok to not fail the build as Develocity server may be down or have temporary issues.
        // In such a case, we should not fail the whole test suite.
        printBuildOutput()
    }
}

private fun TestProject.setupNonDefaultJdk(pathToJdk: File) {
    gradleProperties.modify {
        """
        |org.gradle.java.home=${pathToJdk.absolutePath.normalizePath()}
        |
        |$it
        """.trimMargin()
    }
}

internal fun TestProject.runShellCommands(path: Path = projectPath, commands: MutableList<List<String>>.() -> Unit = {}) {
    val commandsList = mutableListOf<List<String>>()
    commands(commandsList)

    commandsList.forEach {
        runProcess(
            it,
            path.toFile(),
            environmentVariables.environmentalVariables
        ).apply {
            assertTrue(isSuccessful, output)
        }
    }
}

internal fun Path.enableAndroidSdk() {
    val androidSdk = KtTestUtil.findAndroidSdk()
    resolve("local.properties")
        .also { if (!it.exists()) it.createFile() }
        .appendText(
            """
            sdk.dir=${androidSdk.absolutePath.normalizePath()}
            """.trimIndent()
        )
    acceptAndroidSdkLicenses(androidSdk)
    applyAndroidTestFixes()
}

internal fun Path.enableCacheRedirector() {
    // Path relative to the current Gradle module project dir
    val redirectorScript = Paths.get("../../../repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts")
    assert(redirectorScript.exists()) {
        "$redirectorScript does not exist! Please provide correct path to 'cache-redirector.settings.gradle.kts' file."
    }
    val gradleDir = resolve("gradle").also { it.createDirectories() }
    redirectorScript.copyTo(gradleDir.resolve("cache-redirector.settings.gradle.kts"))

    val projectCacheRedirectorStatus = Paths
        .get("../../../gradle.properties")
        .readText()
        .lineSequence()
        .first { it.startsWith("cacheRedirectorEnabled") }

    resolve("gradle.properties")
        .also { if (!it.exists()) it.createFile() }
        .appendText(
            """
            |
            |$projectCacheRedirectorStatus
            |
            """.trimMargin()
        )

    val settingsGradle = resolve("settings.gradle")
    val settingsGradleKts = resolve("settings.gradle.kts")
    when {
        Files.exists(settingsGradle) -> settingsGradle.modify {
            """
            |${it.substringBefore("pluginManagement {")}
            |pluginManagement {
            |    apply from: 'gradle/cache-redirector.settings.gradle.kts'
            |${it.substringAfter("pluginManagement {")}
            """.trimMargin()
        }
        Files.exists(settingsGradleKts) -> settingsGradleKts.modify {
            """
            |${it.substringBefore("pluginManagement {")}
            |pluginManagement {
            |    apply(from = "gradle/cache-redirector.settings.gradle.kts")
            |${it.substringAfter("pluginManagement {")}
            """.trimMargin()
        }
    }
}

private fun GradleProject.addHeapDumpOptions() {
    addPropertyToGradleProperties(
        propertyName = "org.gradle.jvmargs",
        mapOf(
            "-XX:+HeapDumpOnOutOfMemoryError" to "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:HeapDumpPath" to "-XX:HeapDumpPath=\"${System.getProperty("user.dir")}${File.separatorChar}build\""
        ),
    )
}

private const val SINGLE_NATIVE_TARGET_PLACEHOLDER = "<SingleNativeTarget>"
private const val LOCAL_REPOSITORY_PLACEHOLDER = "<localRepo>"

private fun TestProject.configureSingleNativeTarget(
    configureSubProjects: Boolean = false,
    preset: String = HostManager.host.presetName,
) {
    val buildScript = if (buildGradle.exists()) buildGradle else buildGradleKts
    buildScript.modify {
        it.replace(SINGLE_NATIVE_TARGET_PLACEHOLDER, preset)
    }
    if (configureSubProjects) {
        configureSingleNativeTargetInSubFolders(preset)
    }
}

private fun TestProject.configureSingleNativeTargetInSubFolders(preset: String = HostManager.host.presetName) {
    projectPath.walk()
        .filter { it.isRegularFile() && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
        .forEach { file ->
            file.modify {
                it.replace(SINGLE_NATIVE_TARGET_PLACEHOLDER, preset)
            }
        }
}

internal fun GradleProject.configureLocalRepository(localRepoDir: Path) {
    projectPath.walk()
        .filter { it.isRegularFile() && it.name in buildFileNames }
        .forEach { file ->
            file.modify { it.replace(LOCAL_REPOSITORY_PLACEHOLDER, localRepoDir.absolutePathString().replace("\\", "\\\\")) }
        }
}

internal fun TestProject.enableStableConfigurationCachePreview() {
    val settingsFile = if (settingsGradleKts.exists()) {
        settingsGradleKts
    } else {
        settingsGradle
    }
    settingsFile.append(
        """
            |
            |enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
            """.trimMargin()
    )
}

/**
 * Represents different types of dependency management provided to tests.
 */
sealed interface DependencyManagement {
    class DefaultDependencyManagement(val additionalRepos: Set<String> = emptySet()) : DependencyManagement
    data object DisabledDependencyManagement : DependencyManagement
}

/**
 * Resolves the temporary local repository path for the test with specified Gradle version.
 */
fun KGPBaseTest.defaultLocalRepo(gradleVersion: GradleVersion) = workingDir.resolve(gradleVersion.version).resolve("repo")

// https://developer.android.com/studio/intro/update.html#download-with-gradle
private fun acceptAndroidSdkLicenses(androidHome: File) {
    val sdkLicensesDir = androidHome.resolve("licenses")
    if (!sdkLicensesDir.exists()) sdkLicensesDir.mkdirs()

    val sdkLicenses = listOf(
        "8933bad161af4178b1185d1a37fbf41ea5269c55",
        "d56f5187479451eabf01fb78af6dfcb131a6481e",
        "24333f8a63b6825ea9c5514f83c2829b004d1fee",
    )
    val sdkPreviewLicense = "84831b9409646a918e30573bab4c9c91346d8abd"

    val sdkLicenseFile = sdkLicensesDir.resolve("android-sdk-license")
    if (!sdkLicenseFile.exists()) {
        sdkLicenseFile.createNewFile()
        sdkLicenseFile.writeText(
            sdkLicenses.joinToString(separator = "\n")
        )
    } else {
        sdkLicenses
            .subtract(
                sdkLicenseFile.readText().lines()
            )
            .forEach {
                sdkLicenseFile.appendText("$it\n")
            }
    }

    val sdkPreviewLicenseFile = sdkLicensesDir.resolve("android-sdk-preview-license")
    if (!sdkPreviewLicenseFile.exists()) {
        sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
    } else {
        if (sdkPreviewLicense != sdkPreviewLicenseFile.readText().trim()) {
            sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
        }
    }
}

/**
 * Indicates if the test and the Gradle build started by the test should run in the same process.
 * This setup allows using a single debugger for both the test and the build process (including build script injections).
 *
 * Add `kotlin.gradle.autoDebugIT=false` to `local.properties` to opt out of implicit withDebug when debugging the tests in IDE.
 */
enum class EnableGradleDebug {
    DISABLED,
    ENABLED,
    AUTO;

    fun toBooleanFlag(): Boolean {
        return when (this) {
            DISABLED -> false
            ENABLED -> true
            AUTO -> System.getProperty("kotlin.gradle.autoDebugIT").toBoolean() &&
                    ManagementFactory.getRuntimeMXBean().inputArguments.toString().contains("-agentlib:jdwp")
        }
    }

    companion object {
        const val LOOPBACK_IP = "127.0.0.1"
        const val PORT_FOR_DEBUGGING_KGP_IT_WITH_ENVS = 5100
    }
}

private fun Throwable.backtrace(): String = StringWriter().use {
    PrintWriter(it).use {
        this.printStackTrace(it)
    }
    it
}.toString()
