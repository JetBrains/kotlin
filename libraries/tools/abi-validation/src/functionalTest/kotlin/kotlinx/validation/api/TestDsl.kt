/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import kotlinx.validation.ApiValidationExtension
import java.io.*
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language

public val API_DIR: String = ApiValidationExtension().apiDumpDirectory

private val koverEnabled: Boolean = System.getProperty("kover.enabled").toBoolean()

internal fun BaseKotlinGradleTest.test(
    gradleVersion: String = "8.5",
    injectPluginClasspath: Boolean = true,
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

    val runner = GradleRunner.create()
        .withProjectDir(rootProjectDir)
        .withPluginClasspath()
        .withArguments(baseKotlinScope.runner.arguments)
        .withGradleVersion(gradleVersion)

    if (koverEnabled) {
        // In debug mode, tests will be running inside the same JVM.
        // That will allow collection coverage info by the Kover.
        runner.withDebug(true)
    }

    if (injectPluginClasspath) {
        // The hack dating back to https://docs.gradle.org/6.0/userguide/test_kit.html#sub:test-kit-classpath-injection
        // Currently, some tests won't work without it because some classes are missing on the classpath.
        runner.addPluginTestRuntimeClasspath()
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

internal fun BaseKotlinScope.runner(fn: Runner.() -> Unit) {
    val runner = Runner()
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

internal class Runner {
    val arguments: MutableList<String> = mutableListOf<String>().apply {
        if (!koverEnabled) {
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
