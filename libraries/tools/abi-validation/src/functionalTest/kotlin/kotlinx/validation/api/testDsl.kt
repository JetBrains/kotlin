/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api

import org.gradle.testkit.runner.GradleRunner

internal fun BaseKotlinGradleTest.test(fn: BaseKotlinScope.() -> Unit): GradleRunner {
    val baseKotlinScope = BaseKotlinScope()
    fn(baseKotlinScope)

    baseKotlinScope.files.forEach { scope ->
        val fileWriteTo = testProjectDir.root.resolve(scope.filePath)
                .apply {
                    parentFile.mkdirs()
                    createNewFile()
                }

        scope.files.forEach {
            val fileContent = readFileList(it)
            fileWriteTo.appendText("\n" + fileContent)
        }
    }

    return GradleRunner.create() //
            .withProjectDir(testProjectDir.root)
            .withPluginClasspath()
            .withArguments(baseKotlinScope.runner.arguments)
    // disabled because of: https://github.com/gradle/gradle/issues/6862
    // .withDebug(baseKotlinScope.runner.debug)
}

internal fun BaseKotlinScope.file(fileName: String, fn: AppendableScope.() -> Unit) {
    val appendableScope = AppendableScope(fileName)
    fn(appendableScope)

    files.add(appendableScope)
}

/**
 * same as [file], but appends "src/main/java" before given `classFileName`
 */
internal fun BaseKotlinScope.kotlin(classFileName: String, fn: AppendableScope.() -> Unit) {
    require(classFileName.endsWith(".kt")) {
        "ClassFileName must end with '.kt'"
    }

    val fileName = "src/main/java/$classFileName"
    file(fileName, fn)
}

/**
 * Shortcut for creating a `build.gradle.kts` by using [file]
 */
internal fun BaseKotlinScope.buildGradleKts(fn: AppendableScope.() -> Unit) {
    val fileName = "build.gradle.kts"
    file(fileName, fn)
}

internal fun BaseKotlinScope.runner(fn: Runner.() -> Unit) {
    val runner = Runner()
    fn(runner)

    this.runner = runner
}

internal fun AppendableScope.resolve(fileName: String) {
    this.files.add(fileName)
}

internal class BaseKotlinScope {
    var files: MutableList<AppendableScope> = mutableListOf()
    var runner: Runner = Runner()
}

internal class AppendableScope(val filePath: String) {
    val files: MutableList<String> = mutableListOf()
}

internal class Runner {
    var debug = false
    val arguments: MutableList<String> = mutableListOf()
}
