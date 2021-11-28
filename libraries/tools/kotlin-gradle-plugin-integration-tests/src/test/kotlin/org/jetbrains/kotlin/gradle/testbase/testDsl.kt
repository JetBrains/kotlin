/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.BaseGradleIT.Companion.acceptAndroidSdkLicenses
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.nio.file.*
import java.nio.file.Files.copy
import java.nio.file.Files.createDirectories
import java.nio.file.attribute.BasicFileAttributes
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
    test: TestProject.() -> Unit
): TestProject {
    val projectPath = setupProjectFromTestResources(
        projectName,
        gradleVersion,
        workingDir,
        projectPathAdditionalSuffix
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

    if (buildJdk != null) testProject.setupNonDefaultJdk(buildJdk)

    testProject.test()
    return testProject
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

open class GradleProject(
    val projectName: String,
    val projectPath: Path
) {
    val buildGradle: Path get() = projectPath.resolve("build.gradle")
    val buildGradleKts: Path get() = projectPath.resolve("build.gradle.kts")
    val settingsGradle: Path get() = projectPath.resolve("settings.gradle")
    val settingsGradleKts: Path get() = projectPath.resolve("settings.gradle.kts")
    val gradleProperties: Path get() = projectPath.resolve("gradle.properties")

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
private val testKitDir get() = Paths.get(".").resolve(".testKitDir")

private val hashAlphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
private fun randomHash(length: Int = 15): String {
    return List(length) { hashAlphabet.random() }.joinToString("")
}

private fun setupProjectFromTestResources(
    projectName: String,
    gradleVersion: GradleVersion,
    tempDir: Path,
    optionalSubDir: String
): Path {
    val testProjectPath = projectName.testProjectPath
    assertTrue("Test project exists") { Files.exists(testProjectPath) }
    assertTrue("Test project path is a directory") { Files.isDirectory(testProjectPath) }

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

private fun Path.addPluginManagementToSettings() {
    val settingsGradle = resolve("settings.gradle")
    val settingsGradleKts = resolve("settings.gradle.kts")
    val fileToUpdate = when {
        Files.exists(settingsGradle) -> settingsGradle
        Files.exists(settingsGradleKts) -> settingsGradleKts
        else -> null
    }

    if (fileToUpdate == null) {
        settingsGradle.toFile().writeText(DEFAULT_GROOVY_SETTINGS_FILE)
    } else {
        val settingsContent = fileToUpdate.toFile().readText()
        if (!settingsContent
                .lines()
                .first { !it.startsWith("//") }
                .startsWith("pluginManagement {")
        ) {
            fileToUpdate.toFile().writeText(
                """
                    $DEFAULT_GROOVY_SETTINGS_FILE
                    
                    $settingsContent
                    """.trimIndent()
            )
        }
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

private fun Path.copyRecursively(dest: Path) {
    Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            createDirectories(dest.resolve(relativize(dir)))
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            copy(file, dest.resolve(relativize(file)))
            return FileVisitResult.CONTINUE
        }
    })
}
