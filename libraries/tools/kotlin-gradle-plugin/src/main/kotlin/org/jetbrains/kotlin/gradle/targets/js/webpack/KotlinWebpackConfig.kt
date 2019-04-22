/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NodeJsCodingAssistanceForCoreModules", "JSUnresolvedFunction")

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.Serializable
import java.io.StringWriter

@Suppress("MemberVisibilityCanBePrivate")
class KotlinWebpackConfig(
    val entry: File,
    val reportEvaluatedConfigFile: File?,
    val outputPath: File,
    val configDirectory: File,
    val reportDir: File?,
    var devServer: DevServer? = null,
    val showProgress: Boolean = false
) {
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
        configFile.writeText(build())
    }

    fun build() = buildString {
        //language=JavaScript 1.8
        appendln(
            """  
var config = {
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
};"""
        )
        appendln()

        if (devServer != null) {
            appendln("// dev server")
            appendln("config.devServer = ${json(devServer!!)};")
            appendln()
        }

        if (reportDir != null) {
            val reportBasePath = "${reportDir.canonicalPath}/${entry.name}"
            val config = KotlinWebpackConfig.BundleAnalyzerPlugin(
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
"""
            )
        }

        appendln()
        loadConfigs(configDirectory)
        appendln()

        if (reportEvaluatedConfigFile != null) {
            val filePath = jsQuotedString(reportEvaluatedConfigFile.canonicalPath)

            //language=JavaScript 1.8
            appendln(
                """
// save evaluated config file
var util = require('util');
var fs = require("fs");
var evaluatedConfig = util.inspect(config, {showHidden: false, depth: null, compact: false});
fs.writeFile($filePath, evaluatedConfig, function (err) {});
"""
            )
            appendln()
        }

        appendln("module.exports = config")
    }

    private fun json(obj: Any) = StringWriter().also {
        GsonBuilder().setPrettyPrinting().create().toJson(obj, it)
    }.toString()

    private fun jsQuotedString(str: String) = StringWriter().also {
        JsonWriter(it).value(str)
    }.toString()

    private fun StringBuilder.loadConfigs(confDir: File) {
        if (confDir.isDirectory) confDir
            .listFiles()
            ?.toList()
            ?.filter { it.name.endsWith(".js") }
            ?.forEach {
                appendln("// ${it.name}")
                append(it.readText())
                appendln()
                appendln()
            }
    }
}