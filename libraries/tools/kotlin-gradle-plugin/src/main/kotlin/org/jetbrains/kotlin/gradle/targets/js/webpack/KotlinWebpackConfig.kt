/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NodeJsCodingAssistanceForCoreModules", "JSUnresolvedFunction")

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import java.io.File
import java.io.Serializable
import java.io.StringWriter

@Suppress("MemberVisibilityCanBePrivate")
data class KotlinWebpackConfig(
    val mode: Mode = Mode.DEVELOPMENT,
    val entry: File? = null,
    val outputPath: File? = null,
    val outputFileName: String? = entry?.name,
    val configDirectory: File? = null,
    val bundleAnalyzerReportDir: File? = null,
    val reportEvaluatedConfigFile: File? = null,
    val devServer: DevServer? = null,
    val devtool: Devtool? = Devtool.EVAL_SOURCE_MAP,
    val showProgress: Boolean = false,
    val sourceMaps: Boolean = false,
    val export: Boolean = true,
    val progressReporter: Boolean = false,
    val progressReporterPathFilter: String? = null,
    val resolveFromModulesFirst: Boolean = false
) {
    fun getRequiredDependencies(versions: NpmVersions) =
        mutableListOf<RequiredKotlinJsDependency>().also {
            it.add(versions.webpack)
            it.add(versions.webpackCli)

            if (bundleAnalyzerReportDir != null) {
                it.add(versions.webpackBundleAnalyzer)
            }

            if (sourceMaps) {
                it.add(versions.kotlinSourceMapLoader)
            }

            if (devServer != null) {
                it.add(versions.webpackDevServer)
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
        val port: Int = 8080,
        val proxy: Map<String, Any>? = null,
        val contentBase: List<String>
    ) : Serializable

    enum class Devtool(val code: String) {
        EVAL_SOURCE_MAP("eval-source-map"),
        SOURCE_MAP("source-map")
    }

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
                    var config = {
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
            appendFromConfigDir()
            appendEvaluatedFileReport()
            appendProgressReporter()

            if (export) {
                //language=JavaScript 1.8
                appendln("module.exports = config")
            }
        }
    }

    private fun Appendable.appendEvaluatedFileReport() {
        if (reportEvaluatedConfigFile == null) return

        val filePath = reportEvaluatedConfigFile.canonicalPath.jsQuoted()

        //language=JavaScript 1.8
        appendln(
            """
                // save evaluated config file
                var util = require('util');
                var fs = require("fs");
                var evaluatedConfig = util.inspect(config, {showHidden: false, depth: null, compact: false});
                fs.writeFile($filePath, evaluatedConfig, function (err) {});
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendFromConfigDir() {
        if (configDirectory == null) return

        appendln()
        appendConfigsFromDir(configDirectory)
        appendln()
    }

    private fun Appendable.appendReport() {
        if (bundleAnalyzerReportDir == null) return

        entry ?: error("Entry should be defined for report")

        val reportBasePath = "${bundleAnalyzerReportDir.canonicalPath}/${entry.name}"
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
        appendln("config.devServer = ${json(devServer)};")
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
                        use: ["kotlin-source-map-loader"],
                        enforce: "pre"
                });
                config.devtool = ${devtool?.code?.let { "'$it'" } ?: false};
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendEntry() {
        if (entry == null || outputPath == null) return

        val multiEntryOutput = "${outputFileName!!.removeSuffix(".js")}-[name].js"

        //language=JavaScript 1.8
        appendln(
            """
                // entry
                config.entry = {
                    main: [${entry.canonicalPath.jsQuoted()}]
                };
                
                config.output = {
                    path: ${outputPath.canonicalPath.jsQuoted()},
                    filename: (chunkData) => {
                        return chunkData.chunk.name === 'main'
                            ? ${outputFileName.jsQuoted()}
                            : ${multiEntryOutput.jsQuoted()};
                    }
                };
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendResolveModules() {
        if (!resolveFromModulesFirst || entry == null || entry.parent == null) return

        //language=JavaScript 1.8
        appendln(
            """
                // resolve modules
                config.resolve.modules.unshift(${entry.parent.jsQuoted()})
                
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
                        let p = percentage * 100;
                        let msg = `${"$"}{Math.trunc(p / 10)}${"$"}{Math.trunc(p % 10)}% ${"$"}{message} ${"$"}{args.join(' ')}`;
                        ${if (progressReporterPathFilter == null) "" else """
                            msg = msg.replace(new RegExp(${progressReporterPathFilter.jsQuoted()}, 'g'), '');
                        """.trimIndent()};
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