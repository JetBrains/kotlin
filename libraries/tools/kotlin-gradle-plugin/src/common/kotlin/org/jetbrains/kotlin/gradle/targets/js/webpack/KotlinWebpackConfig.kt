/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NodeJsCodingAssistanceForCoreModules", "JSUnresolvedFunction")

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWebpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
import java.io.File
import java.io.Serializable
import java.io.StringWriter

@Suppress("MemberVisibilityCanBePrivate")
data class KotlinWebpackConfig(
    val npmProjectDir: Provider<File>? = null,
    var mode: Mode = Mode.DEVELOPMENT,
    var entry: File? = null,
    var output: KotlinWebpackOutput? = null,
    var outputPath: File? = null,
    var outputFileName: String? = entry?.name,
    var configDirectory: File? = null,
    var reportEvaluatedConfigFile: File? = null,
    var devServer: DevServer? = null,
    var watchOptions: WatchOptions? = null,
    var experiments: MutableSet<String> = mutableSetOf(),
    override val rules: KotlinWebpackRulesContainer,
    var devtool: String? = WebpackDevtool.EVAL_SOURCE_MAP,
    var showProgress: Boolean = false,
    var optimization: Optimization? = null,
    var sourceMaps: Boolean = false,
    var export: Boolean = true,
    var progressReporter: Boolean = false,
    var progressReporterPathFilter: File? = null,
    var resolveFromModulesFirst: Boolean = false
) : WebpackRulesDsl {

    val entryInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> entry?.relativeOrAbsolute(npmProjectDir) }

    val outputPathInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> outputPath?.relativeOrAbsolute(npmProjectDir) }

    val progressReporterPathFilterInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> progressReporterPathFilter?.relativeOrAbsolute(npmProjectDir) }

    fun getRequiredDependencies(versions: NpmVersions) =
        mutableSetOf<RequiredKotlinJsDependency>().also {
            it.add(
                versions.webpack
            )
            it.add(
                versions.webpackCli
            )

            if (sourceMaps) {
                it.add(
                    versions.sourceMapLoader
                )
            }

            if (devServer != null) {
                it.add(
                    versions.webpackDevServer
                )
            }

            rules.forEach { rule ->
                if (rule.active) {
                    it.addAll(rule.dependencies(versions))
                }
            }
        }

    enum class Mode(val code: String) {
        DEVELOPMENT("development"),
        PRODUCTION("production")
    }

    @Suppress("unused")
    data class DevServer(
        var open: Any = true,
        var port: Int? = null,
        var proxy: MutableMap<String, Any>? = null,
        var static: MutableList<String>? = null,
        var contentBase: MutableList<String>? = null,
        var client: Client? = null
    ) : Serializable {
        data class Client(
            var overlay: Any /* Overlay | Boolean */
        ) : Serializable {
            data class Overlay(
                var errors: Boolean,
                var warnings: Boolean
            ) : Serializable
        }
    }

    @Suppress("unused")
    data class Optimization(
        var runtimeChunk: Any?,
        var splitChunks: Any?
    ) : Serializable

    @Suppress("unused")
    data class WatchOptions(
        var aggregateTimeout: Int? = null,
        var ignored: Any? = null
    ) : Serializable

    fun save(configFile: File) {
        configFile.writer().use {
            appendTo(it)
        }
    }

    fun appendTo(target: Appendable) {
        with(target) {
            //language=JavaScript 1.8
            appendLine(
                """
                    let config = {
                      mode: '${mode.code}',
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
                    
                """.trimIndent()
            )

            appendEntry()
            appendResolveModules()
            appendSourceMaps()
            appendOptimization()
            appendDevServer()
            appendProgressReporter()
            rules.forEach { rule ->
                if (rule.active) {
                    with(rule) { appendToWebpackConfig() }
                }
            }
            appendErrorPlugin()
            appendFromConfigDir()
            appendExperiments()

            if (export) {
                //language=JavaScript 1.8
                appendLine("module.exports = config")
            }
        }
    }

    private fun Appendable.appendFromConfigDir() {
        if (configDirectory == null || !configDirectory!!.isDirectory) return

        appendLine()
        appendConfigsFromDir(configDirectory!!)
        appendLine()
    }

    private fun Appendable.appendDevServer() {
        if (devServer != null) {

            appendLine("// dev server")
            appendLine("config.devServer = ${json(devServer!!)};")
            appendLine()
        }

        if (watchOptions == null) return

        //language=JavaScript 1.8
        appendLine(
            """
                config.watchOptions = ${json(watchOptions!!)};
            """.trimIndent()
        )
    }

    private fun Appendable.appendExperiments() {
        if (experiments.isEmpty()) return

        appendLine("config.experiments = {")
        for (experiment in experiments.sorted()) {
            appendLine("    $experiment: true,")
        }
        appendLine("}")
    }

    private fun Appendable.appendSourceMaps() {
        if (!sourceMaps) return

        //language=JavaScript 1.8
        appendLine(
            """
                // source maps
                config.module.rules.push({
                        test: /\.js${'$'}/,
                        use: ["source-map-loader"],
                        enforce: "pre"
                });
                config.devtool = ${devtool?.let { "'$it'" } ?: false};
            ${
                "config.ignoreWarnings = [/Failed to parse source map/]"
            }
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendOptimization() {
        if (optimization == null) return

        //language=JavaScript 1.8
        appendLine(
            """
                // optimization
                config.optimization = config.optimization || ${json(optimization!!)};
            """.trimIndent()
        )
    }

    private fun Appendable.appendEntry() {
        if (entry != null) {
            //language=JavaScript 1.8
            appendLine(
                """
                // entry
                config.entry = {
                    main: [require('path').resolve(__dirname, ${entryInput!!.jsQuoted()})]
                };
                """.trimIndent()
            )
        }

        if (output != null) {
            val multiEntryOutput = "${outputFileName!!.removeSuffix(".js")}-[name].js"

            //language=JavaScript 1.8
            appendLine(
                """
                config.output = {
                    filename: (chunkData) => {
                        return chunkData.chunk.name === 'main'
                            ? ${outputFileName!!.jsQuoted()}
                            : ${multiEntryOutput.jsQuoted()};
                    },
                    ${output!!.library?.let { "library: ${it.jsQuoted()}," } ?: ""}
                    ${output!!.libraryTarget?.let { "libraryTarget: ${it.jsQuoted()}," } ?: ""}
                    globalObject: "${output!!.globalObject}"
                };
                """.trimIndent()
            )
        }

        if (
            outputPath == null
        )
            return

        //language=JavaScript 1.8
        appendLine(
            """
                config.output.path = require('path').resolve(__dirname, ${outputPathInput!!.jsQuoted()})
            """.trimIndent()
        )
    }

    private fun Appendable.appendErrorPlugin() {
        //language=ES6
        appendLine(
            """
                // noinspection JSUnnecessarySemicolon
                ;(function(config) {
                    const tcErrorPlugin = require('kotlin-test-js-runner/tc-log-error-webpack');
                    config.plugins.push(new tcErrorPlugin())
                    config.stats = config.stats || {}
                    Object.assign(config.stats, config.stats, {
                        warnings: false,
                        errors: false
                    })
                })(config);
            """.trimIndent()
        )
    }

    private fun Appendable.appendResolveModules() {
        if (!resolveFromModulesFirst || entry == null || entry!!.parent == null) return

        //language=JavaScript 1.8
        appendLine(
            """
                // resolve modules
                config.resolve.modules.unshift(${entry!!.parent.jsQuoted()})
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendProgressReporter() {
        if (!progressReporter) return

        //language=ES6
        appendLine(
            """
                // Report progress to console
                // noinspection JSUnnecessarySemicolon
                ;(function(config) {
                    const webpack = require('webpack');
                    const handler = (percentage, message, ...args) => {
                        const p = percentage * 100;
                        let msg = `${"$"}{Math.trunc(p / 10)}${"$"}{Math.trunc(p % 10)}% ${"$"}{message} ${"$"}{args.join(' ')}`;
                        ${
                if (progressReporterPathFilterInput == null) "" else """
                            msg = msg.replace(require('path').resolve(__dirname, ${progressReporterPathFilterInput!!.jsQuoted()}), '');
                        """.trimIndent()
            };
                        console.log(msg);
                    };
            
                    config.plugins.push(new webpack.ProgressPlugin(handler))
                })(config);
                
            """.trimIndent()
        )
    }

    private fun json(obj: Any) = StringWriter().also {
        GsonBuilder().setPrettyPrinting().create().toJson(obj, it)
    }.toString()
}
