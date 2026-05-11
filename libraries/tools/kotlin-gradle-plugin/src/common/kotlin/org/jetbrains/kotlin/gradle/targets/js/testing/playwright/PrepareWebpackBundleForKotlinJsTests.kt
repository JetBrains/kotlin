/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask

abstract class PrepareWebpackBundleForKotlinJsTests : DefaultTask() {

    /**
     * Path (passed to `path.resolve(__dirname, ...)`) of the JS entry file with the tests.
     *
     * The value will be inserted verbatim in place of `{{testsEntryFile}}` in the template,
     * so it must be a valid JS expression — e.g. a quoted string literal like `'kotlin/foo-test.js'`.
     */
    @get:Input
    abstract val testsEntryFile: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputFile
    val webpackConfigFile: Provider<RegularFile> get() = outputDir.file("webpack.config.js")

    @get:OutputFile
    val testHtmlFile: Provider<RegularFile> get() = outputDir.file("test.html")

    @TaskAction
    fun generate() {
        writeWebpackConfigFile()
        writeTestHtmlFile()
    }

    private fun writeWebpackConfigFile() {
        val template = javaClass.classLoader.getResourceAsStream(WEBPACK_TEMPLATE_RESOURCE_PATH)
            ?: error("Webpack config template not found on the classpath: $WEBPACK_TEMPLATE_RESOURCE_PATH")

        val rendered = template.bufferedReader(Charsets.UTF_8).use {
            it.readText()
                .replacePlaceholder("testsEntryFile", testsEntryFile.get())
        }

        val output = webpackConfigFile.get().asFile
        output.parentFile?.mkdirs()
        output.writeText(rendered)
    }

    private fun writeTestHtmlFile() {
        val template = javaClass.classLoader.getResourceAsStream(TEST_HTML_TEMPLATE_RESOURCE_PATH)
            ?: error("Test HTML template not found on the classpath: $TEST_HTML_TEMPLATE_RESOURCE_PATH")

        val rendered = template.bufferedReader(Charsets.UTF_8).use {
            it.readText()
        }

        val output = testHtmlFile.get().asFile
        output.parentFile?.mkdirs()
        output.writeText(rendered)
    }

    private fun String.replacePlaceholder(name: String, value: String): String {
        return replace("{{$name}}", value)
    }

    companion object {
        private const val WEBPACK_TEMPLATE_RESOURCE_PATH =
            "kotlinJsTestsForBrowser/webpack-for-kotlin-js-tests.config.js.template"

        private const val TEST_HTML_TEMPLATE_RESOURCE_PATH =
            "kotlinJsTestsForBrowser/test.html"
    }
}

internal fun KotlinJsIrCompilation.locateOrRegisterPrepareWebpackBundleForBrowserTests(): TaskProvider<PrepareWebpackBundleForKotlinJsTests> {
    val project = this.project
    return project.locateOrRegisterTask<PrepareWebpackBundleForKotlinJsTests>(
        "prepareWebpackBundleForKotlinJsTests",
        invokeWhenRegistered = {
            val binary = binaries.getIrBinaries(
                KotlinJsBinaryMode.DEVELOPMENT
            ).single()
        }
    ) {
        val binary = binaries.getIrBinaries(
            KotlinJsBinaryMode.DEVELOPMENT
        ).single()
    }
}
