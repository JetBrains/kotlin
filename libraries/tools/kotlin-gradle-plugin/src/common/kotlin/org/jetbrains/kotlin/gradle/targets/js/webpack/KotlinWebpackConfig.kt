/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NodeJsCodingAssistanceForCoreModules", "JSUnresolvedFunction")

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWebpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackMajorVersion.Companion.choose
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
import java.io.File
import java.io.Serializable
import java.io.StringWriter

@Suppress("MemberVisibilityCanBePrivate")
data class KotlinWebpackConfig(
    @Internal
    val npmProjectDir: Provider<File>? = null,
    @Input
    var mode: Mode = Mode.DEVELOPMENT,
    @Internal
    var entry: File? = null,
    @Nested
    @Optional
    var output: KotlinWebpackOutput? = null,
    @Internal
    var outputPath: File? = null,
    @Input
    @Optional
    var outputFileName: String? = entry?.name,
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:Optional
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputDirectory
    var configDirectory: File? = null,
    @Input
    @Optional
    var devServer: DevServer? = null,
    @Input
    var experiments: MutableSet<String> = mutableSetOf(),
    @Nested
    override val rules: KotlinWebpackRulesContainer,
    @Input
    @Optional
    var devtool: String? = WebpackDevtool.EVAL_SOURCE_MAP,
    @Input
    var showProgress: Boolean = false,
    @Input
    var sourceMaps: Boolean = false,
    @Input
    var export: Boolean = true,
    @Input
    var progressReporter: Boolean = false,
    @Internal
    @Optional
    var progressReporterPathFilter: File? = null,
    @Input
    var resolveFromModulesFirst: Boolean = false,
    @Input
    val webpackMajorVersion: WebpackMajorVersion = WebpackMajorVersion.V5
) : WebpackRulesDsl {

    @get:Input
    @get:Optional
    val entryInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> entry?.relativeOrAbsolute(npmProjectDir) }

    @get:Input
    @get:Optional
    val outputPathInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> outputPath?.relativeOrAbsolute(npmProjectDir) }

    @get:Input
    @get:Optional
    val progressReporterPathFilterInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> progressReporterPathFilter?.relativeOrAbsolute(npmProjectDir) }

    fun getRequiredDependencies(versions: NpmVersions) =
        mutableSetOf<RequiredKotlinJsDependency>().also {
            it.add(versions.kotlinJsTestRunner)
            it.add(
                webpackMajorVersion.choose(
                    versions.webpack,
                    versions.webpack4
                )
            )
            it.add(
                webpackMajorVersion.choose(
                    versions.webpackCli,
                    versions.webpackCli3
                )
            )
            it.add(versions.formatUtil)

            if (sourceMaps) {
                it.add(
                    webpackMajorVersion.choose(
                        versions.sourceMapLoader,
                        versions.sourceMapLoader1
                    )
                )
            }

            if (devServer != null) {
                it.add(
                    webpackMajorVersion.choose(
                        versions.webpackDevServer,
                        versions.webpackDevServer3
                    )
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
        if (devServer == null) return

        appendLine("// dev server")
        appendLine("config.devServer = ${json(devServer!!)};")
        appendLine()
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
                webpackMajorVersion.choose(
                    "config.ignoreWarnings = [/Failed to parse source map/]",
                    """
                config.stats = config.stats || {}
                Object.assign(config.stats, config.stats, {
                    warningsFilter: [/Failed to parse source map/]
                })
                """
                )
            }
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendEntry() {
        if (
            entry == null
            || outputPath == null
            || output == null
        )
            return

        val multiEntryOutput = "${outputFileName!!.removeSuffix(".js")}-[name].js"

        //language=JavaScript 1.8
        appendLine(
            """
                // entry
                config.entry = {
                    main: [require('path').resolve(__dirname, ${entryInput!!.jsQuoted()})]
                };
                
                config.output = {
                    path: require('path').resolve(__dirname, ${outputPathInput!!.jsQuoted()}),
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
