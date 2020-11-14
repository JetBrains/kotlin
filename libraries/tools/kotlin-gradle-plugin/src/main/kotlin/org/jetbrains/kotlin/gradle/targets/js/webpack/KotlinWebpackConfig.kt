/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NodeJsCodingAssistanceForCoreModules", "JSUnresolvedFunction")

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.EXTRACT
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.IMPORT
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.INLINE
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackMajorVersion.Companion.choose
import java.io.File
import java.io.Serializable
import java.io.StringWriter

@Suppress("MemberVisibilityCanBePrivate")
data class KotlinWebpackConfig(
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
    @Internal
    var configDirectory: File? = null,
    @Internal
    var bundleAnalyzerReportDir: File? = null,
    @Internal
    var reportEvaluatedConfigFile: File? = null,
    @Input
    @Optional
    var devServer: DevServer? = null,
    @Nested
    var cssSupport: KotlinWebpackCssSupport = KotlinWebpackCssSupport(),
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
    @Input
    @Optional
    var progressReporterPathFilter: String? = null,
    @Input
    var resolveFromModulesFirst: Boolean = false,
    @Input
    val webpackMajorVersion: WebpackMajorVersion = WebpackMajorVersion.V5
) {
    @get:Input
    @get:Optional
    val entryInput: String?
        get() = entry?.absoluteFile?.normalize()?.absolutePath

    @get:Input
    @get:Optional
    val outputPathInput: String?
        get() = outputPath?.absoluteFile?.normalize()?.absolutePath

    @get:Input
    @get:Optional
    val configDirectoryInput: String?
        get() = configDirectory?.absoluteFile?.normalize()?.absolutePath

    @get:Input
    @get:Optional
    val bundleAnalyzerReportDirInput: String?
        get() = bundleAnalyzerReportDir?.absoluteFile?.normalize()?.absolutePath

    @get:Input
    @get:Optional
    val reportEvaluatedConfigFileInput: String?
        get() = reportEvaluatedConfigFile?.absoluteFile?.normalize()?.absolutePath

    fun getRequiredDependencies(versions: NpmVersions) =
        mutableSetOf<RequiredKotlinJsDependency>().also {
            it.add(versions.kotlinJsTestRunner)
            it.add(
                webpackMajorVersion.choose(
                    versions.webpack,
                    versions.webpack4
                )
            )
            it.add(versions.webpackCli)
            it.add(versions.formatUtil)

            if (bundleAnalyzerReportDir != null) {
                it.add(versions.webpackBundleAnalyzer)
            }

            if (sourceMaps) {
                it.add(
                    webpackMajorVersion.choose(
                        versions.sourceMapLoader,
                        versions.sourceMapLoader1
                    )
                )
            }

            if (devServer != null) {
                it.add(versions.webpackDevServer)
            }

            if (!cssSupport.enabled || cssSupport.rules.isEmpty()) return@also

            it.add(versions.cssLoader)
            cssSupport.rules.forEach { rule ->
                when (rule.mode) {
                    EXTRACT -> it.add(versions.miniCssExtractPlugin)
                    INLINE -> it.add(versions.styleLoader)
                    IMPORT -> it.add(versions.toStringLoader)
                    else -> cssError()
                }
            }

        }

    enum class Mode(val code: String) {
        DEVELOPMENT("development"),
        PRODUCTION("production")
    }

    @Suppress("unused")
    data class BundleAnalyzerPlugin(
        val analyzerMode: String,
        val reportFilename: String,
        val openAnalyzer: Boolean,
        val generateStatsFile: Boolean,
        val statsFilename: String
    ) : Serializable

    @Suppress("unused")
    data class DevServer(
        val inline: Boolean = true,
        val lazy: Boolean = false,
        val noInfo: Boolean = true,
        val open: Any = true,
        val overlay: Any = false,
        val port: Int? = null,
        val proxy: Map<String, Any>? = null,
        val contentBase: List<String>
    ) : Serializable

    fun save(configFile: File) {
        configFile.writer().use {
            appendTo(it)
        }
    }

    fun appendTo(target: Appendable) {
        with(target) {
            //language=JavaScript 1.8
            appendln(
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
            appendReport()
            appendProgressReporter()
            appendCssSupport()
            appendErrorPlugin()
            appendFromConfigDir()
            appendEvaluatedFileReport()

            if (export) {
                //language=JavaScript 1.8
                appendln("module.exports = config")
            }
        }
    }

    private fun Appendable.appendEvaluatedFileReport() {
        if (reportEvaluatedConfigFile == null) return

        val filePath = reportEvaluatedConfigFile!!.canonicalPath.jsQuoted()

        //language=JavaScript 1.8
        appendln(
            """
                // save evaluated config file
                ;(function(config) {
                    const util = require('util');
                    const fs = require('fs');
                    const evaluatedConfig = util.inspect(config, {showHidden: false, depth: null, compact: false});
                    fs.writeFile($filePath, evaluatedConfig, function (err) {});
                })(config);
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendFromConfigDir() {
        if (configDirectory == null || !configDirectory!!.isDirectory) return

        appendln()
        appendConfigsFromDir(configDirectory!!)
        appendln()
    }

    private fun Appendable.appendReport() {
        if (bundleAnalyzerReportDir == null) return

        entry ?: error("Entry should be defined for report")

        val reportBasePath = "${bundleAnalyzerReportDir!!.canonicalPath}/${entry!!.name}"
        val config = BundleAnalyzerPlugin(
            "static",
            "$reportBasePath.report.html",
            false,
            true,
            "$reportBasePath.stats.json"
        )

        //language=JavaScript 1.8
        appendln(
            """
                // save webpack-bundle-analyzer report 
                var BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin; 
                config.plugins.push(new BundleAnalyzerPlugin(${json(config)}));
                
           """.trimIndent()
        )
        appendln()
    }

    private fun Appendable.appendDevServer() {
        if (devServer == null) return

        appendln("// dev server")
        appendln("config.devServer = ${json(devServer!!)};")
        appendln()
    }

    private fun Appendable.appendSourceMaps() {
        if (!sourceMaps) return

        //language=JavaScript 1.8
        appendln(
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
        appendln(
            """
                // entry
                config.entry = {
                    main: [${entry!!.canonicalPath.jsQuoted()}]
                };
                
                config.output = {
                    path: ${outputPath!!.canonicalPath.jsQuoted()},
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

    private fun Appendable.appendCssSupport() {
        if (!cssSupport.enabled || cssSupport.rules.isEmpty())
            return

        appendln(
            """
            // css settings
            ;(function(config) {
            """.trimIndent()
        )

        val extractedCss =
            """
            |       const MiniCssExtractPlugin = require('mini-css-extract-plugin');
            |       use.unshift({
            |           loader: MiniCssExtractPlugin.loader,
            |           options: {}
            |       })
            |       config.plugins.push(new MiniCssExtractPlugin())
            """.trimMargin()

        val inlinedCss =
            """
            |       use.unshift({
            |           loader: 'style-loader',
            |           options: {}
            |       })
            |       
            """.trimMargin()

        val importedCss =
            """
            |       use.unshift({
            |           loader: 'to-string-loader',
            |           options: {}
            |       })
            |       
            """.trimMargin()

        cssSupport.rules.forEach { rule ->
            appendln(
                """
            |    ;(function(config) {
            """.trimMargin()
            )
            appendln(
                """
            |       const use = [
            |           {
            |               loader: 'css-loader',
            |               options: {},
            |           }
            |       ]
            """.trimMargin()
            )

            when (rule.mode) {
                EXTRACT -> appendln(extractedCss)
                INLINE -> appendln(inlinedCss)
                IMPORT -> appendln(importedCss)
                else -> cssError()
            }

            val excluded = rule.exclude.let {
                if (it.isNotEmpty()) {
                    "[${it.joinToString()}]"
                } else null
            }

            val included = rule.include.let {
                if (it.isNotEmpty()) {
                    "[${it.joinToString()}]"
                } else null
            }

            appendln(
                """
            |       config.module.rules.push({
            |           test: /\.css${'$'}/,
            |           use: use,
            |           ${excluded?.let { "exclude: $it," } ?: ""}
            |           ${included?.let { "include: $it" } ?: ""}
            |       })

            """.trimMargin()
            )

            appendln(
                """
            |   })(config);
            
            """.trimMargin()
            )
        }

        appendln(
            """
            })(config);
            
            """.trimIndent()
        )
    }

    private fun Appendable.appendErrorPlugin() {
        //language=ES6
        appendln(
            """
                // noinspection JSUnnecessarySemicolon
                ;(function(config) {
                    const tcErrorPlugin = require('kotlin-test-js-runner/tc-log-error-webpack');
                    config.plugins.push(new tcErrorPlugin(tcErrorPlugin))
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
        appendln(
            """
                // resolve modules
                config.resolve.modules.unshift(${entry!!.parent.jsQuoted()})
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendProgressReporter() {
        if (!progressReporter) return

        //language=ES6
        appendln(
            """
                // Report progress to console
                // noinspection JSUnnecessarySemicolon
                ;(function(config) {
                    const webpack = require('webpack');
                    const handler = (percentage, message, ...args) => {
                        const p = percentage * 100;
                        let msg = `${"$"}{Math.trunc(p / 10)}${"$"}{Math.trunc(p % 10)}% ${"$"}{message} ${"$"}{args.join(' ')}`;
                        ${
                if (progressReporterPathFilter == null) "" else """
                            msg = msg.replace(new RegExp(${progressReporterPathFilter!!.jsQuoted()}, 'g'), '');
                        """.trimIndent()
            };
                        console.log(msg);
                    };
            
                    config.plugins.push(new webpack.ProgressPlugin(handler))
                })(config);
                
            """.trimIndent()
        )
    }

    private fun cssError() {
        throw IllegalStateException(
            """
                    Possible values for cssSupport.mode:
                    - EXTRACT
                    - INLINE
                    - IMPORT
                """.trimIndent()
        )
    }


    private fun json(obj: Any) = StringWriter().also {
        GsonBuilder().setPrettyPrinting().create().toJson(obj, it)
    }.toString()
}
