/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NodeJsCodingAssistanceForCoreModules", "JSUnresolvedFunction")

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import org.jetbrains.kotlin.gradle.targets.js.appendConfigsFromDir
import java.io.File
import java.io.Serializable
import java.io.StringWriter

@Suppress("MemberVisibilityCanBePrivate")
data class KotlinWebpackConfigWriter(
    val mode: Mode = Mode.DEVELOPMENT,
    val entry: File? = null,
    val outputPath: File? = null,
    val outputFileName: String? = entry?.name,
    val configDirectory: File? = null,
    val bundleAnalyzerReportDir: File? = null,
    val reportEvaluatedConfigFile: File? = null,
    var devServer: DevServer? = null,
    val showProgress: Boolean = false,
    val sourceMaps: Boolean = false,
    val sourceMapsRuntime: Boolean = false,
    val export: Boolean = true,
    val progressReporter: Boolean = false,
    val progressReporterPathFilter: String? = null
) {
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
            appendSourceMaps()
            appendSourceMapsRuntime()
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

        val filePath = jsQuotedString(reportEvaluatedConfigFile.canonicalPath)

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
        appendln("config.devServer = ${json(devServer!!)};")
        appendln()
    }

    private fun Appendable.appendSourceMapsRuntime() {
        if (!sourceMapsRuntime) return

        //language=JavaScript 1.8
        appendln(
            """
                // source maps runtime
                if (!config.entry) config.entry = [];
                config.entry.push('source-map-support/browser-source-map-support.js');
                
            """.trimIndent()
        )
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
                config.module.rules.push({test: /\.js${'$'}/, use: ['source-map-loader'], enforce: 'pre'});
                config.devtool = 'eval-source-map';
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendEntry() {
        if (entry == null || outputPath == null) return

        //language=JavaScript 1.8
        appendln(
            """
                // entry
                if (!config.entry) config.entry = [];
                config.entry.push(${jsQuotedString(entry.canonicalPath)});
                config.output = {
                    path: ${jsQuotedString(outputPath.canonicalPath)},
                    filename: ${jsQuotedString(outputFileName!!)}
                };
                
            """.trimIndent()
        )
    }

    private fun Appendable.appendProgressReporter() {
        if (!progressReporter) return

        appendln(
            """
                // Report progress to console
                (function(config) {
                    const webpack = require('webpack');
                    const handler = (percentage, message, ...args) => {
                        const p = percentage*100;
                        let msg = Math.trunc(p/100) + Math.trunc(p%100) + '% ' + message + ' ' + args.join(' ');
                        ${if (progressReporterPathFilter == null) "" else """
                            msg = msg.replace(new RegExp(${jsQuotedString(progressReporterPathFilter ?: "")}, 'g'), '');
                        """.trimIndent()}
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

    private fun jsQuotedString(str: String) = StringWriter().also {
        JsonWriter(it).value(str)
    }.toString()
}