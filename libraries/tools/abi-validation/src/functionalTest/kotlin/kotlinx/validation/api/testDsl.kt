/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import kotlinx.validation.API_DIR
import org.gradle.testkit.runner.*
import java.io.*

internal fun BaseKotlinGradleTest.test(fn: BaseKotlinScope.() -> Unit): GradleRunner {
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

    return GradleRunner.create() //
        .withProjectDir(rootProjectDir)
        .withPluginClasspath()
        .withArguments(baseKotlinScope.runner.arguments)
    // disabled because of: https://github.com/gradle/gradle/issues/6862
    // .withDebug(baseKotlinScope.runner.debug)
}

/**
 * same as [file][FileContainer.file], but prepends "src/main/java" before given `classFileName`
 */
internal fun FileContainer.kotlin(classFileName: String, fn: AppendableScope.() -> Unit) {
    require(classFileName.endsWith(".kt")) {
        "ClassFileName must end with '.kt'"
    }

    val fileName = "src/main/java/$classFileName"
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

internal fun AppendableScope.resolve(fileName: String) {
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
): FileContainer {

    override fun file(fileName: String, fn: AppendableScope.() -> Unit) {
        parent.file("$dirPath/$fileName", fn)
    }
}

internal class AppendableScope(val filePath: String) {
    val files: MutableList<String> = mutableListOf()
}

internal class Runner {
    val arguments: MutableList<String> = mutableListOf()
}

internal fun readFileList(fileName: String): String {
    val resource = BaseKotlinGradleTest::class.java.classLoader.getResource(fileName)
        ?: throw IllegalStateException("Could not find resource '$fileName'")
    return File(resource.toURI()).readText()
}

