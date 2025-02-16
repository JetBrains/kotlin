/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.abi.tools.test.api

import java.io.*
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language

public val API_DIR: String = "api"

private val koverEnabled: Boolean = System.getProperty("kover.enabled").toBoolean()

internal fun BaseKotlinGradleTest.test(
    gradleVersion: String = "8.11",
    fn: BaseKotlinScope.() -> Unit
): GradleRunner {
    val baseKotlinScope = BaseKotlinScope()
    fn(baseKotlinScope)

    baseKotlinScope.files.forEach { scope ->
        val fileWriteTo = rootProjectDir.resolve(scope.filePath)
            .apply {
                parentFile.mkdirs()
                createNewFile()
            }

        scope.files.forEach {
            val fileContent = readFileList(it)
            fileWriteTo.appendText(fileContent)
        }
    }

    rootProjectDir.patchSettings()
    rootProjectDir.patchBuildFile()

    val runner = GradleRunner.create()
        .withProjectDir(rootProjectDir)
        .withArguments(baseKotlinScope.runner.arguments)
        .withGradleVersion(gradleVersion)

    if (koverEnabled) {
        // In debug mode, tests will be running inside the same JVM.
        // That will allow collection coverage info by the Kover.
        runner.withDebug(true)
    }

    return runner
}

/**
 * same as [file][FileContainer.file], but prepends "src/${sourceSet}/kotlin" before given `classFileName`
 */
internal fun FileContainer.kotlin(
    classFileName: String,
    sourceSet: String = "main",
    fn: AppendableScope.() -> Unit,
) {
    require(classFileName.endsWith(".kt")) {
        "ClassFileName must end with '.kt'"
    }

    val fileName = "src/${sourceSet}/kotlin/$classFileName"
    file(fileName, fn)
}

/**
 * same as [file][FileContainer.file], but prepends "src/${sourceSet}/java" before given `classFileName`
 */
internal fun FileContainer.java(
    classFileName: String,
    sourceSet: String = "main",
    fn: AppendableScope.() -> Unit,
) {
    require(classFileName.endsWith(".java")) {
        "ClassFileName must end with '.java'"
    }

    val fileName = "src/${sourceSet}/java/$classFileName"
    file(fileName, fn)
}

/**
 * Shortcut for creating a `build.gradle.kts` by using [file][FileContainer.file]
 */
internal fun FileContainer.buildGradleKts(fn: AppendableScope.() -> Unit) {
    val fileName = "build.gradle.kts"
    file(fileName, fn)
}

/**
 * Shortcut for creating a `settings.gradle.kts` by using [file][FileContainer.file]
 */
internal fun FileContainer.settingsGradleKts(fn: AppendableScope.() -> Unit) {
    val fileName = "settings.gradle.kts"
    file(fileName, fn)
}

/**
 * Declares a directory with the given [dirName] inside the current container.
 * All calls creating files within this scope will create the files nested in this directory.
 *
 * Note that it is valid to call this method multiple times at the same level with the same [dirName].
 * Files declared within 2 independent calls to [dir] will be added to the same directory.
 */
internal fun FileContainer.dir(dirName: String, fn: DirectoryScope.() -> Unit) {
    DirectoryScope(dirName, this).fn()
}

/**
 * Shortcut for creating a `api/<project>.api` descriptor by using [file][FileContainer.file]
 */
internal fun FileContainer.apiFile(projectName: String, fn: AppendableScope.() -> Unit) {
    dir(API_DIR) {
        file("$projectName.api", fn)
    }
}

/**
 * Shortcut for creating a `api/<target>/<project>.klib.api` descriptor using [file][FileContainer.file]
 */
internal fun FileContainer.abiFile(projectName: String, target: String, fn: AppendableScope.() -> Unit) {
    dir(API_DIR) {
        dir(target) {
            file("$projectName.klib.api", fn)
        }
    }
}

internal fun FileContainer.abiFile(projectName: String, fn: AppendableScope.() -> Unit) {
    dir(API_DIR) {
        file("$projectName.klib.api", fn)
    }
}

// not using default argument in apiFile for clarity in tests (explicit "empty" in the name)
/**
 * Shortcut for creating an empty `api/<project>.api` descriptor by using [file][FileContainer.file]
 */
internal fun FileContainer.emptyApiFile(projectName: String) {
    apiFile(projectName) {}
}

internal fun BaseKotlinScope.runner(withConfigurationCache: Boolean = true, fn: Runner.() -> Unit) {
    val runner = Runner(withConfigurationCache)
    fn(runner)

    this.runner = runner
}

internal fun AppendableScope.resolve(@Language("file-reference") fileName: String) {
    this.files.add(fileName)
}

internal interface FileContainer {
    fun file(fileName: String, fn: AppendableScope.() -> Unit)
}

internal class BaseKotlinScope : FileContainer {
    var files: MutableList<AppendableScope> = mutableListOf()
    var runner: Runner = Runner()

    override fun file(fileName: String, fn: AppendableScope.() -> Unit) {
        val appendableScope = AppendableScope(fileName)
        fn(appendableScope)
        files.add(appendableScope)
    }
}

internal class DirectoryScope(
    val dirPath: String,
    val parent: FileContainer
) : FileContainer {

    override fun file(fileName: String, fn: AppendableScope.() -> Unit) {
        parent.file("$dirPath/$fileName", fn)
    }
}

internal class AppendableScope(val filePath: String) {
    val files: MutableList<String> = mutableListOf()
}

internal class Runner(withConfigurationCache: Boolean = true) {
    val arguments: MutableList<String> = mutableListOf<String>().apply {
        add("--stacktrace")
        if (!koverEnabled && withConfigurationCache) {
            // Configuration cache is incompatible with javaagents being enabled for Gradle
            // See https://github.com/gradle/gradle/issues/25979
            add("--configuration-cache")
        }
    }
}

internal fun readFileList(@Language("file-reference") fileName: String): String {
    val resource = BaseKotlinGradleTest::class.java.getResource(fileName)
        ?: throw IllegalStateException("Could not find resource '$fileName'")
    return File(resource.toURI()).readText()
}

private fun File.patchSettings() {
    val settingsFile = resolve("settings.gradle.kts")
    val content = if (settingsFile.exists()) {
        settingsFile.readText()
    } else {
        ""
    }

    settingsFile.appendText(PLUGIN_MANAGEMENT)
    settingsFile.appendText(content)
}

private fun File.patchBuildFile() {
    val buildFile = resolve("build.gradle.kts")
    buildFile.appendText(LOCAL_REPOSITORY)
}

private fun GradleRunner.addPluginTestRuntimeClasspath() = apply {
    val cpResource = javaClass.classLoader.getResourceAsStream("plugin-classpath.txt")
        ?.let { InputStreamReader(it) }
        ?: throw IllegalStateException("Could not find classpath resource")

    val pluginClasspath = pluginClasspath + cpResource.readLines().map { File(it) }
    withPluginClasspath(pluginClasspath)
}

internal val commonNativeTargets = listOf(
    "linuxX64",
    "linuxArm64",
    "mingwX64",
    "androidNativeArm32",
    "androidNativeArm64",
    "androidNativeX64",
    "androidNativeX86"
)

internal val appleNativeTarget = listOf(
    "macosX64",
    "macosArm64",
    "iosX64",
    "iosArm64",
    "iosSimulatorArm64",
    "tvosX64",
    "tvosArm64",
    "tvosSimulatorArm64",
    "watchosArm32",
    "watchosArm64",
    "watchosX64",
    "watchosSimulatorArm64",
    "watchosDeviceArm64",
)

private val PLUGIN_MANAGEMENT = """
pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.jvm") useVersion("$KOTLIN_VERSION")
            if (requested.id.id == "org.jetbrains.kotlin.multiplatform") useVersion("$KOTLIN_VERSION")
            if (requested.id.id == "org.jetbrains.kotlin.android") useVersion("$KOTLIN_VERSION")
        }
    }
    
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

"""


private val LOCAL_REPOSITORY = """

repositories {
    mavenLocal()
}

"""

