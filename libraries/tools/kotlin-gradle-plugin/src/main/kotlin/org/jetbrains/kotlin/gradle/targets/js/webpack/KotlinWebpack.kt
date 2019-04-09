/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.stream.JsonWriter
import org.gradle.api.DefaultTask
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.*
import org.gradle.process.internal.ExecHandleFactory
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationToRunnableFiles
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask
import org.jetbrains.kotlin.gradle.testing.internal.reportsDir
import org.jetbrains.kotlin.gradle.utils.injected
import java.io.File
import java.io.StringWriter
import javax.inject.Inject

open class KotlinWebpack : DefaultTask() {
    @get:Inject
    open val fileResolver: FileResolver
        get() = injected

    @get:Inject
    open val execHandleFactory: ExecHandleFactory
        get() = injected

    @Input
    @SkipWhenEmpty
    open lateinit var entry: File

    open val configFile: File
        @OutputFile get() = project.buildDir.resolve("webpack.config.js")

    @Input
    var saveConfigFileEffective: Boolean = true

    open val configFileEffective: File
        @OutputFile get() = project.buildDir.resolve("webpack.config.evaluated.js")

    open val outputPath: File
        @OutputDirectory get() = project.buildDir.resolve("lib")

    open val configDirectory: File
        @InputDirectory get() = project.projectDir.resolve("webpack.config.d")

    @Input
    var report: Boolean = false

    open val reportDir: File
        @OutputDirectory get() = project.reportsDir.resolve("webpack").resolve(entry.nameWithoutExtension)

    @TaskAction
    fun execute() {
        check(entry.isFile) {
            "${this}: Entry file not existed \"$entry\""
        }

        NpmResolver.resolve(project)

        val npmProjectLayout = NpmProject[project]

        configFile.writeText(buildConfig())

        val execFactory = execHandleFactory.newExec()
        npmProjectLayout.useTool(
            execFactory,
            ".bin/webpack",
            "--config", configFile.absolutePath
        )
        val exec = execFactory.build()
        exec.start()
        exec.waitForFinish()
    }

    private fun buildConfig(): String {
        return buildString {
            //language=JavaScript 1.8
            append(
                """ 
    const config = {
      mode: 'development',
      entry: '${entry.canonicalPath}',
      output: {
        path: '${outputPath.canonicalPath}',
        filename: '${entry.name}'
      },
      resolve: {
        modules: [
          "node_modules"
        ]
      },
      plugins: [],
      module: {
        rules: []
      }
    };
                    """
            )

            if (saveConfigFileEffective) {
                //language=JavaScript 1.8
                append(
                    """
    const util = require('util');
    const fs = require("fs");
    const evaluatedConfig = util.inspect(config, {showHidden: false, depth: null, compact: false});
    fs.writeFile(${jsQuotedString(configFileEffective.canonicalPath)}, evaluatedConfig, (err) => {});                    
                        """
                )
            }

            if (report) {
                //language=JavaScript 1.8
                append(
                    """
    const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin; 
    config.plugins.push(new BundleAnalyzerPlugin({
            analyzerMode: "static",
            reportFilename: "${entry.name}.report.html",
            openAnalyzer: false,
            generateStatsFile: true,
            statsFilename: "${entry.name}.stats.json"
        })
    )                        
                        """
                )
            }

            loadConfigs(configDirectory)

            append("module.exports = config")
        }
    }

    private fun jsQuotedString(str: String) = StringWriter().also {
        JsonWriter(it).value(str)
    }.toString()

    private fun StringBuilder.loadConfigs(confDir: File) {
        if (confDir.isDirectory) confDir
            .listFiles()
            ?.toList()
            ?.filter { it.name.endsWith(".js") }
            ?.forEach {
                append(it.readText())
                appendln()
                appendln()
            }
    }

    companion object {
        fun configure(compilation: KotlinCompilationToRunnableFiles<*>) {
            val project = compilation.target.project
            val npmProject = project.npmProject

            compilation.dependencies {
                runtimeOnly(npm("webpack", "4.29.6"))
                runtimeOnly(npm("webpack-cli", "3.3.0"))
                runtimeOnly(npm("webpack-bundle-analyzer", "3.3.2"))
            }

            project.createOrRegisterTask<KotlinWebpack>("webpack") {
                val compileKotlinTask = compilation.compileKotlinTask
                compileKotlinTask as Kotlin2JsCompile

                it.dependsOn(compileKotlinTask)

                it.entry = npmProject.moduleOutput(compileKotlinTask)
            }
        }
    }
}