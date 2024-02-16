/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BaseGradleIT.Companion.acceptAndroidSdkLicenses
import org.jetbrains.kotlin.gradle.model.ModelContainer
import org.jetbrains.kotlin.gradle.model.ModelFetcherBuildAction
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.runProcess
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.test.assertTrue

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
    forceOutput: Boolean = false,
    enableBuildScan: Boolean = false,
    addHeapDumpOptions: Boolean = true,
    enableGradleDebug: Boolean = false,
    enableGradleDaemonMemoryLimitInMb: Int? = 1024,
    enableKotlinDaemonMemoryLimitInMb: Int? = 1024,
    projectPathAdditionalSuffix: String = "",
    buildJdk: File? = null,
    localRepoDir: Path? = null,
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
    projectPath.addDefaultSettingsToSettingsGradle(gradleVersion, dependencyManagement, localRepoDir)
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
        gradleRunner,
        projectName,
        projectPath,
        buildOptions,
        gradleVersion,
        forceOutput = forceOutput,
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
 * @param [projectName] test project name in 'src/test/resources/testProject` directory.
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
    forceOutput: Boolean = false,
    enableBuildScan: Boolean = false,
    addHeapDumpOptions: Boolean = true,
    enableGradleDebug: Boolean = false,
    enableGradleDaemonMemoryLimitInMb: Int? = 1024,
    enableKotlinDaemonMemoryLimitInMb: Int? = 1024,
    projectPathAdditionalSuffix: String = "",
    buildJdk: File? = null,
    localRepoDir: Path? = null,
    environmentVariables: EnvironmentalVariables = EnvironmentalVariables(),
    configureSubProjects: Boolean = false,
    dependencyManagement: DependencyManagement = DependencyManagement.DefaultDependencyManagement(),
    test: TestProject.() -> Unit = {},
): TestProject {
    val project = project(
        projectName = projectName,
        gradleVersion = gradleVersion,
        buildOptions = buildOptions,
        forceOutput = forceOutput,
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
    forceOutput: Boolean = this.forceOutput,
    enableGradleDebug: Boolean = this.enableGradleDebug,
    kotlinDaemonDebugPort: Int? = this.kotlinDaemonDebugPort,
    enableBuildCacheDebug: Boolean = false,
    enableBuildScan: Boolean = this.enableBuildScan,
    enableGradleDaemonMemoryLimitInMb: Int? = this.enableGradleDaemonMemoryLimitInMb,
    enableKotlinDaemonMemoryLimitInMb: Int? = this.enableKotlinDaemonMemoryLimitInMb,
    buildOptions: BuildOptions = this.buildOptions,
    environmentVariables: EnvironmentalVariables = this.environmentVariables,
    assertions: BuildResult.() -> Unit = {},
) {
    if (enableBuildScan) agreeToBuildScanService()
    ensureKotlinCompilerArgumentsPluginAppliedCorrectly(buildOptions)

    val allBuildArguments = commonBuildSetup(
        buildArguments.toList(),
        buildOptions,
        enableBuildCacheDebug,
        enableBuildScan,
        enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb,
        gradleVersion,
        kotlinDaemonDebugPort
    )
    val gradleRunnerForBuild = gradleRunner
        .also { if (forceOutput) it.forwardOutput() }
        .also { if (environmentVariables.environmentalVariables.isNotEmpty()) it.withEnvironment(System.getenv() + environmentVariables.environmentalVariables) }
        .withDebug(enableGradleDebug)
        .withArguments(allBuildArguments)
    withBuildSummary(allBuildArguments) {
        val buildResult = gradleRunnerForBuild.build()
        if (enableBuildScan) buildResult.printBuildScanUrl()
        assertions(buildResult)
        buildResult.additionalAssertions(buildOptions)
    }
}

/**
 * Trigger test project build with given [buildArguments] and assert build is failed.
 */
fun TestProject.buildAndFail(
    vararg buildArguments: String,
    forceOutput: Boolean = this.forceOutput,
    enableGradleDebug: Boolean = this.enableGradleDebug,
    kotlinDaemonDebugPort: Int? = this.kotlinDaemonDebugPort,
    enableBuildCacheDebug: Boolean = false,
    enableBuildScan: Boolean = this.enableBuildScan,
    buildOptions: BuildOptions = this.buildOptions,
    enableGradleDaemonMemoryLimitInMb: Int? = this.enableGradleDaemonMemoryLimitInMb,
    enableKotlinDaemonMemoryLimitInMb: Int? = this.enableKotlinDaemonMemoryLimitInMb,
    environmentVariables: EnvironmentalVariables = this.environmentVariables,
    assertions: BuildResult.() -> Unit = {},
) {
    if (enableBuildScan) agreeToBuildScanService()
    ensureKotlinCompilerArgumentsPluginAppliedCorrectly(buildOptions)

    val allBuildArguments = commonBuildSetup(
        buildArguments.toList(),
        buildOptions,
        enableBuildCacheDebug,
        enableBuildScan,
        enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb,
        gradleVersion,
        kotlinDaemonDebugPort
    )
    val gradleRunnerForBuild = gradleRunner
        .also { if (forceOutput) it.forwardOutput() }
        .also { if (environmentVariables.environmentalVariables.isNotEmpty()) it.withEnvironment(System.getenv() + environmentVariables.environmentalVariables) }
        .withDebug(enableGradleDebug)
        .withArguments(allBuildArguments)
    withBuildSummary(allBuildArguments) {
        val buildResult = gradleRunnerForBuild.buildAndFail()
        if (enableBuildScan) buildResult.printBuildScanUrl()
        assertions(buildResult)
        buildResult.additionalAssertions(buildOptions)
    }

}

fun getGradleUserHome(): File {
    return testKitDir.toAbsolutePath().toFile().canonicalFile
}

private fun BuildResult.additionalAssertions(buildOptions: BuildOptions) {
    if (buildOptions.warningMode != WarningMode.Fail) {
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
        emptyList(),
        buildOptions,
        false,
        enableBuildScan,
        enableGradleDaemonMemoryLimitInMb,
        enableKotlinDaemonMemoryLimitInMb,
        gradleVersion
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
}

/**
 * You need at least [TestVersions.Gradle.G_7_0] for supporting environment variables with gradle runner
 */
@JvmInline
value class EnvironmentalVariables @EnvironmentalVariablesOverride constructor(val environmentalVariables: Map<String, String> = emptyMap()) {
    @EnvironmentalVariablesOverride
    constructor(vararg environmentVariables: Pair<String, String>) : this(mapOf(*environmentVariables))
}

@RequiresOptIn("Environmental variables override may lead to interference of parallel builds and breaks Gradle tests debugging")
annotation class EnvironmentalVariablesOverride

@OptIn(EnvironmentalVariablesOverride::class)
class TestProject(
    val gradleRunner: GradleRunner,
    projectName: String,
    projectPath: Path,
    val buildOptions: BuildOptions,
    val gradleVersion: GradleVersion,
    val forceOutput: Boolean,
    val enableBuildScan: Boolean,
    val enableGradleDaemonMemoryLimitInMb: Int?,
    val enableKotlinDaemonMemoryLimitInMb: Int?,
    /**
     * Whether the test and the Gradle build launched by the test should be executed in the same process so that we can use the same
     * debugger for both (see https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html#isDebug--).
     */
    val enableGradleDebug: Boolean,
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
     * @param otherProjectName The name of the other project to include as a submodule.
     * @param pathPrefix An optional prefix to prepend to the submodule's path. Defaults to an empty string.
     * @param newSubmoduleName An optional new name for the submodule. Defaults to the otherProjectName.
     * @param isKts Whether to update a .kts settings file instead of a .gradle settings file. Defaults to false.
     */
    fun includeOtherProjectAsSubmodule(
        otherProjectName: String,
        pathPrefix: String = "",
        newSubmoduleName: String = otherProjectName,
        isKts: Boolean = false,
        localRepoDir: Path? = null,
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
    }

    fun includeOtherProjectAsIncludedBuild(
        otherProjectName: String,
        pathPrefix: String,
    ) {
        val otherProjectPath = "$pathPrefix/$otherProjectName".testProjectPath
        otherProjectPath.copyRecursively(projectPath.resolve(otherProjectName))

        projectPath.resolve(otherProjectName).addDefaultSettingsToSettingsGradle(gradleVersion)

        settingsGradle.append(
            """
            
            includeBuild '$otherProjectName'
            """.trimIndent()
        )
    }
}

private fun commonBuildSetup(
    buildArguments: List<String>,
    buildOptions: BuildOptions,
    enableBuildCacheDebug: Boolean,
    enableBuildScan: Boolean,
    enableGradleDaemonMemoryLimitInMb: Int?,
    enableKotlinDaemonMemoryLimitInMb: Int?,
    gradleVersion: GradleVersion,
    kotlinDaemonDebugPort: Int? = null,
): List<String> {
    // Following jdk system properties are provided via sub-project build.gradle.kts
    val jdkPropNameRegex = Regex("jdk\\d+Home")
    val jdkLocations = System.getProperties()
        .filterKeys { it.toString().matches(jdkPropNameRegex) }
        .values
        .joinToString(separator = ",")
    return buildOptions.toArguments(gradleVersion) + buildArguments + listOfNotNull(
        // Required toolchains should be pre-installed via repo. Tests should not download any JDKs
        "-Porg.gradle.java.installations.auto-download=false",
        "-Porg.gradle.java.installations.paths=$jdkLocations",
        // Decreasing Gradle daemon idle timeout to 1 min from default 3 hours.
        // This should help with OOM on CI when agents do not have enough free memory available.
        "-Dorg.gradle.daemon.idletimeout=60000",
        if (enableGradleDaemonMemoryLimitInMb != null) {
            // Limiting Gradle daemon heap size to reduce memory pressure on CI agents
            "-Dorg.gradle.jvmargs=-Xmx${enableGradleDaemonMemoryLimitInMb}m"
        } else null,
        if (enableKotlinDaemonMemoryLimitInMb != null) {
            // Limiting Kotlin daemon heap size to reduce memory pressure on CI agents
            "-Pkotlin.daemon.jvmargs=-Xmx${enableKotlinDaemonMemoryLimitInMb}m"
        } else null,
        if (enableBuildCacheDebug) "-Dorg.gradle.caching.debug=true" else null,
        if (enableBuildScan) "--scan" else null,
        kotlinDaemonDebugPort?.let {
            // Note that we pass "server=n", meaning that we'll need to let the debugger start listening at this port first *before* the
            // Kotlin daemon is launched. That is usually easier than trying to attach the debugger when the Kotlin daemon is launched
            // (currently if we don't attach fast enough, the Kotlin daemon will fail to launch).
            "-Pkotlin.daemon.jvmargs=-agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=$it"
        }
    )
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
 * This property is configured reade konan from specific directory, which in teamcity will be filled with k/n built from master.
 * NOTE: On changing test konan dir location update related location in kotlin-teamcity-build repository
 */
val konanDir
    get() =
        System.getProperty("konanDataDirForIntegrationTests")?.let {
            Paths.get(it)
        } ?: Paths.get(".")
            .resolve("../../../.kotlin")
            .resolve("konan-for-gradle-tests")

/**
 * On changing test kit dir location update related location in 'cleanTestKitCache' task.
 */
internal val testKitDir get() = Paths.get(".").resolve("build").resolve("testKitCache")

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
) {
    addPluginManagementToSettings()
    when (dependencyManagement) {
        is DependencyManagement.DefaultDependencyManagement -> {
            // we cannot switch to dependencyManagement before Gradle 8.1 because of KT-65708
            if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_8_1)) {
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

private fun String.insertBlockToBuildScriptAfterPluginsAndImports(blockToInsert: String): String {
    val importsPattern = Regex("^import.*$", RegexOption.MULTILINE)
    val pluginsBlockPattern = Regex("plugins\\s*\\{[^}]*}", RegexOption.DOT_MATCHES_ALL)

    val lastImportIndex = importsPattern.findAll(this).map { it.range.last }.maxOrNull()
    val pluginBlockEndIndex = pluginsBlockPattern.find(this)?.range?.last

    val insertionIndex = listOfNotNull(lastImportIndex, pluginBlockEndIndex).maxOrNull() ?: return blockToInsert + this

    return StringBuilder(this).insert(insertionIndex + 1, "\n$blockToInsert\n").toString()
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
        .first { it.contains("https://gradle.com/s/") }
        .replaceBefore("https://gradle", "")
    println("Build scan url: $buildScanUrl")
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
    // Path relative to the current gradle module project dir
    val redirectorScript = Paths.get("../../../repo/scripts/cache-redirector.settings.gradle.kts")
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
    projectPath.toFile().walk()
        .filter { it.isFile && (it.name == "build.gradle.kts" || it.name == "build.gradle") }
        .forEach { file ->
            file.modify {
                it.replace(SINGLE_NATIVE_TARGET_PLACEHOLDER, preset)
            }
        }
}

private fun GradleProject.configureLocalRepository(localRepoDir: Path) {
    projectPath.toFile().walkTopDown()
        .filter { it.isFile && it.name in buildFileNames }
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
 * Kotlin Multiplatform Projects have dedicated configurations for source files resolution of all source set dependencies
 * This helper can be useful for cases when you want to resolve a bunch of configurations and don't want to see any unexpected failures
 * coming from *DependencySources configurations. Because by nature not every published library has sources variants that can be resolved
 * via gradle Configurations.
 */
internal fun TestProject.suppressDependencySourcesConfigurations() {
    if (buildGradleKts.exists()) {
        buildGradleKts.appendText(
            """
                allprojects {
                    configurations.configureEach {
                        if (name.endsWith("DependencySources") || name.endsWith("dependencySources")) {
                            incoming.beforeResolve { setExtendsFrom(emptySet()) }                            
                        }
                    }            
                }
            """.trimIndent()
        )
    } else if (buildGradle.exists()) {
        """
            allprojects {
                configurations {
                    configureEach {
                        if (name.endsWith("DependencySources") || name.endsWith("dependencySources")) {
                            incoming.beforeResolve { setExtendsFrom([]) }
                        }
                    }
                }
            }
        """.trimIndent()
    }
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