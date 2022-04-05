/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BaseGradleIT.Companion.acceptAndroidSdkLicenses
import org.jetbrains.kotlin.gradle.model.ModelContainer
import org.jetbrains.kotlin.gradle.model.ModelFetcherBuildAction
import org.jetbrains.kotlin.gradle.native.disableKotlinNativeCaches
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.nio.file.*
import kotlin.io.path.*
import kotlin.test.assertTrue

/**
 * Create new test project.
 *
 * @param [projectName] test project name in 'src/test/resources/testProject` directory.
 * @param [buildOptions] common Gradle build options
 * @param [buildJdk] path to JDK build should run with. *Note* Only append to 'gradle.properties'!
 */
fun KGPBaseTest.project(
    projectName: String,
    gradleVersion: GradleVersion,
    buildOptions: BuildOptions = defaultBuildOptions,
    forceOutput: Boolean = false,
    enableBuildScan: Boolean = false,
    addHeapDumpOptions: Boolean = true,
    enableGradleDebug: Boolean = false,
    projectPathAdditionalSuffix: String = "",
    buildJdk: File? = null,
    localRepoDir: Path? = null,
    test: TestProject.() -> Unit = {}
): TestProject {
    val projectPath = setupProjectFromTestResources(
        projectName,
        gradleVersion,
        workingDir,
        projectPathAdditionalSuffix,
    )
    projectPath.addDefaultBuildFiles()
    projectPath.enableCacheRedirector()
    projectPath.enableAndroidSdk()

    if (addHeapDumpOptions) projectPath.addHeapDumpOptions()

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
        enableGradleDebug,
        forceOutput,
        enableBuildScan
    )
    localRepoDir?.let { testProject.configureLocalRepository(localRepoDir) }
    if (buildJdk != null) testProject.setupNonDefaultJdk(buildJdk)

    testProject.test()
    return testProject
}

/**
 * Create new test project with configuring single native target.
 *
 * @param [projectName] test project name in 'src/test/resources/testProject` directory.
 * @param [buildOptions] common Gradle build options
 * @param [buildJdk] path to JDK build should run with. *Note* Only append to 'gradle.properties'!
 */
fun KGPBaseTest.nativeProject(
    projectName: String,
    gradleVersion: GradleVersion,
    buildOptions: BuildOptions = defaultBuildOptions,
    forceOutput: Boolean = false,
    enableBuildScan: Boolean = false,
    addHeapDumpOptions: Boolean = true,
    enableGradleDebug: Boolean = false,
    projectPathAdditionalSuffix: String = "",
    buildJdk: File? = null,
    localRepoDir: Path? = null,
    test: TestProject.() -> Unit = {}
): TestProject {
    val project = project(
        projectName = projectName,
        gradleVersion = gradleVersion,
        buildOptions = buildOptions,
        forceOutput = forceOutput,
        enableBuildScan = enableBuildScan,
        addHeapDumpOptions = addHeapDumpOptions,
        enableGradleDebug = enableGradleDebug,
        projectPathAdditionalSuffix = projectPathAdditionalSuffix,
        buildJdk = buildJdk,
        localRepoDir = localRepoDir,
    )
    project.configureSingleNativeTarget()
    project.disableKotlinNativeCaches()
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
    enableBuildCacheDebug: Boolean = false,
    enableBuildScan: Boolean = this.enableBuildScan,
    buildOptions: BuildOptions = this.buildOptions,
    assertions: BuildResult.() -> Unit = {}
) {
    if (enableBuildScan) agreeToBuildScanService()

    val allBuildArguments = commonBuildSetup(
        buildArguments.toList(),
        buildOptions,
        enableBuildCacheDebug,
        enableBuildScan,
        gradleVersion
    )
    val gradleRunnerForBuild = gradleRunner
        .also { if (forceOutput) it.forwardOutput() }
        .withDebug(enableGradleDebug)
        .withArguments(allBuildArguments)
    withBuildSummary(allBuildArguments) {
        val buildResult = gradleRunnerForBuild.build()
        if (enableBuildScan) buildResult.printBuildScanUrl()
        assertions(buildResult)
    }
}

/**
 * Trigger test project build with given [buildArguments] and assert build is failed.
 */
fun TestProject.buildAndFail(
    vararg buildArguments: String,
    forceOutput: Boolean = this.forceOutput,
    enableGradleDebug: Boolean = this.enableGradleDebug,
    enableBuildCacheDebug: Boolean = false,
    enableBuildScan: Boolean = this.enableBuildScan,
    buildOptions: BuildOptions = this.buildOptions,
    assertions: BuildResult.() -> Unit = {}
) {
    if (enableBuildScan) agreeToBuildScanService()

    val allBuildArguments = commonBuildSetup(
        buildArguments.toList(),
        buildOptions,
        enableBuildCacheDebug,
        enableBuildScan,
        gradleVersion
    )
    val gradleRunnerForBuild = gradleRunner
        .also { if (forceOutput) it.forwardOutput() }
        .withDebug(enableGradleDebug)
        .withArguments(allBuildArguments)
    withBuildSummary(allBuildArguments) {
        val buildResult = gradleRunnerForBuild.buildAndFail()
        if (enableBuildScan) buildResult.printBuildScanUrl()
        assertions(buildResult)
    }

}

internal inline fun <reified T> TestProject.getModels(
    crossinline assertions: ModelContainer<T>.() -> Unit
) {

    val allBuildArguments = commonBuildSetup(
        emptyList(),
        buildOptions,
        false,
        enableBuildScan,
        gradleVersion
    )

    val connector = GradleConnector
        .newConnector()
        .useGradleUserHomeDir(testKitDir.toAbsolutePath().toFile())
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
    buildCacheLocation: Path
) {
    // language=Groovy
    settingsGradle.append(
        """
        buildCache {
            local {
                directory = '${buildCacheLocation.toUri()}'
            }
        }
        """.trimIndent()
    )
}

fun TestProject.enableStatisticReports(
    type: BuildReportType,
    url: String?
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

open class GradleProject(
    val projectName: String,
    val projectPath: Path
) {
    val buildGradle: Path get() = projectPath.resolve("build.gradle")
    val buildGradleKts: Path get() = projectPath.resolve("build.gradle.kts")
    val settingsGradle: Path get() = projectPath.resolve("settings.gradle")
    val settingsGradleKts: Path get() = projectPath.resolve("settings.gradle.kts")
    val gradleProperties: Path get() = projectPath.resolve("gradle.properties")
    val buildFileNames: Set<String> get() = setOf("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")

    fun classesDir(
        sourceSet: String = "main",
        language: String = "kotlin"
    ): Path = projectPath.resolve("build/classes/$language/$sourceSet/")

    fun kotlinClassesDir(
        sourceSet: String = "main"
    ): Path = classesDir(sourceSet, language = "kotlin")

    fun javaClassesDir(
        sourceSet: String = "main"
    ): Path = classesDir(sourceSet, language = "java")

    fun kotlinSourcesDir(
        sourceSet: String = "main"
    ): Path = projectPath.resolve("src/$sourceSet/kotlin")

    fun javaSourcesDir(
        sourceSet: String = "main"
    ): Path = projectPath.resolve("src/$sourceSet/java")

    fun relativeToProject(
        files: List<Path>
    ): List<Path> = files.map { projectPath.relativize(it) }
}

class TestProject(
    val gradleRunner: GradleRunner,
    projectName: String,
    projectPath: Path,
    val buildOptions: BuildOptions,
    val gradleVersion: GradleVersion,
    val enableGradleDebug: Boolean,
    val forceOutput: Boolean,
    val enableBuildScan: Boolean
) : GradleProject(projectName, projectPath) {
    fun subProject(name: String) = GradleProject(name, projectPath.resolve(name))

    fun includeOtherProjectAsSubmodule(
        otherProjectName: String,
        pathPrefix: String
    ) {
        val otherProjectPath = "$pathPrefix/$otherProjectName".testProjectPath
        otherProjectPath.copyRecursively(projectPath.resolve(otherProjectName))

        settingsGradle.append(
            """
            
            include ':$otherProjectName'
            """.trimIndent()
        )
    }

    fun includeOtherProjectAsIncludedBuild(
        otherProjectName: String,
        pathPrefix: String
    ) {
        val otherProjectPath = "$pathPrefix/$otherProjectName".testProjectPath
        otherProjectPath.copyRecursively(projectPath.resolve(otherProjectName))

        projectPath.resolve(otherProjectName).addDefaultBuildFiles()

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
    gradleVersion: GradleVersion
): List<String> {
    val buildOptionsArguments = buildOptions.toArguments(gradleVersion)
    val buildCacheDebugOption = if (enableBuildCacheDebug) "-Dorg.gradle.caching.debug=true" else null
    val buildScanOption = if (enableBuildScan) "--scan" else null
    return buildOptionsArguments +
            buildArguments +
            listOfNotNull(
                "--full-stacktrace",
                buildCacheDebugOption,
                buildScanOption
            )
}

private fun TestProject.withBuildSummary(
    buildArguments: List<String>,
    run: () -> Unit
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
 * On changing test kit dir location update related location in 'cleanTestKitCache' task.
 */
private val testKitDir get() = Paths.get(".").resolve("build").resolve("testKitCache")

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

private fun Path.addDefaultBuildFiles() {
    addPluginManagementToSettings()

    val buildSrc = resolve("buildSrc")
    if (Files.exists(buildSrc)) {
        buildSrc.addPluginManagementToSettings()
    }
}

internal fun Path.addPluginManagementToSettings() {
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
        else -> settingsGradle.toFile().writeText(DEFAULT_GROOVY_SETTINGS_FILE)
    }

    if (Files.exists(resolve("buildSrc"))) {
        resolve("buildSrc").addPluginManagementToSettings()
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

@OptIn(ExperimentalPathApi::class)
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

@OptIn(ExperimentalPathApi::class)
internal fun Path.enableCacheRedirector() {
    // Path relative to the current gradle module project dir
    val redirectorScript = Paths.get("../../../gradle/cacheRedirector.gradle.kts")
    assert(redirectorScript.exists()) {
        "$redirectorScript does not exist! Please provide correct path to 'cacheRedirector.gradle.kts' file."
    }
    val gradleDir = resolve("gradle").also { it.createDirectories() }
    redirectorScript.copyTo(gradleDir.resolve("cacheRedirector.gradle.kts"))

    val projectCacheRedirectorStatus = Paths
        .get("../../../gradle.properties")
        .readText()
        .lineSequence()
        .first { it.startsWith("cacheRedirectorEnabled") }

    resolve("gradle.properties")
        .also { if (!it.exists()) it.createFile() }
        .appendText(
            """

            $projectCacheRedirectorStatus

            """.trimIndent()
        )

    val projectDir = toFile()
    projectDir.walk().forEach {
        when (it.name) {
            "build.gradle" -> {
                it.appendText(
                    """

                        def cacheRedirectorFile = "${'$'}rootDir/gradle/cacheRedirector.gradle.kts"
                        if (new File(cacheRedirectorFile).exists()) {
                            apply(from: cacheRedirectorFile)
                        }

                    """.trimIndent()
                )
            }
            "build.gradle.kts" -> {
                it.appendText(
                    """

                        val cacheRedirectorFile = "${'$'}rootDir/gradle/cacheRedirector.gradle.kts"
                        if (File(cacheRedirectorFile).exists()) {
                            apply(from = cacheRedirectorFile)
                        }

                    """.trimIndent()
                )
            }
        }
    }
}

@OptIn(ExperimentalPathApi::class)
private fun Path.addHeapDumpOptions() {
    val propertiesFile = resolve("gradle.properties")
    if (!propertiesFile.exists()) propertiesFile.createFile()

    val propertiesContent = propertiesFile.readText()
    val (existingJvmArgsLine, otherLines) = propertiesContent
        .lines()
        .partition {
            it.trim().startsWith("org.gradle.jvmargs")
        }

    val heapDumpOutOfErrorStr = "-XX:+HeapDumpOnOutOfMemoryError"
    val heapDumpPathStr = "-XX:HeapDumpPath=\"${System.getProperty("user.dir")}${File.separatorChar}build\""

    if (existingJvmArgsLine.isEmpty()) {
        propertiesFile.writeText(
            """
            |# modified in addHeapDumpOptions
            |org.gradle.jvmargs=$heapDumpOutOfErrorStr $heapDumpPathStr
            | 
            |$propertiesContent
            """.trimMargin()
        )
    } else {
        val argsLine = existingJvmArgsLine.first()
        val appendedOptions = buildString {
            if (!argsLine.contains("HeapDumpOnOutOfMemoryError")) append(" $heapDumpOutOfErrorStr")
            if (!argsLine.contains("HeapDumpPath")) append(" $heapDumpPathStr")
        }

        if (appendedOptions.isNotEmpty()) {
            propertiesFile.writeText(
                """
                # modified in addHeapDumpOptions
                $argsLine$appendedOptions
                
                ${otherLines.joinToString(separator = "\n")}
                """.trimIndent()
            )
        } else {
            println("<=== Heap dump options are already exists! ===>")
        }
    }
}

private const val SINGLE_NATIVE_TARGET_PLACEHOLDER = "<SingleNativeTarget>"
private const val LOCAL_REPOSITORY_PLACEHOLDER = "<localRepo>"

private fun TestProject.configureSingleNativeTarget(preset: String = HostManager.host.presetName) {
    val buildScript = if (buildGradle.exists()) buildGradle else buildGradleKts
    buildScript.modify {
        it.replace(SINGLE_NATIVE_TARGET_PLACEHOLDER, preset)
    }
}

private fun TestProject.configureLocalRepository(localRepoDir: Path) {
    projectPath.toFile().walkTopDown()
        .filter { it.isFile && it.name in buildFileNames }
        .forEach { file ->
            file.modify { it.replace(LOCAL_REPOSITORY_PLACEHOLDER, localRepoDir.absolutePathString().replace("\\", "\\\\")) }
        }
}

internal fun TestProject.disableKotlinNativeCaches() = gradleProperties.toFile().disableKotlinNativeCaches()
