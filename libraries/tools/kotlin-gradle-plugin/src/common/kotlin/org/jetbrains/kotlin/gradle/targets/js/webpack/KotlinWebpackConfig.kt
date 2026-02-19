/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NodeJsCodingAssistanceForCoreModules", "JSUnresolvedFunction")

package org.jetbrains.kotlin.gradle.targets.js.webpack

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWebpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl
import org.jetbrains.kotlin.gradle.targets.js.internal.appendConfigsFromDir
import org.jetbrains.kotlin.gradle.targets.js.internal.jsQuoted
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File
import java.io.Serializable
import java.io.StringWriter
import java.lang.reflect.Type

/**
 * Configuration options used to generate [webpack](https://webpack.js.org/)
 * configuration used to bundle Kotlin JS and Wasm targets.
 *
 * Some options are directly translated to webpack configuration options,
 * while others are specific to Kotlin.
 *
 * Be aware that changing these options can result in a broken bundle,
 * or produce subtly broken JS code.
 * If you are not sure: do not configure these options,
 * the defaults should work for most use cases.
 *
 * For more information about how Kotlin JS and Wasm use Webpack, see
 * https://kotl.in/js-project-setup/webpack-bundling
 *
 * @see org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
 */
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
    var resolveFromModulesFirst: Boolean = false,
    var resolveLoadersFromKotlinToolingDir: Boolean = false,
) : WebpackRulesDsl {

    val entryInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> entry?.relativeTo(npmProjectDir)?.invariantSeparatorsPath }

    val outputPathInput: String?
        get() = npmProjectDir?.get()?.let { npmProjectDir -> outputPath?.relativeTo(npmProjectDir)?.invariantSeparatorsPath }

    fun getRequiredDependencies(versions: NpmVersions) =
        mutableSetOf<RequiredKotlinJsDependency>().also {
            it.add(
                versions.webpack
            )
            it.add(
                versions.webpackCli
            )

            it.add(
                versions.kotlinWebHelpers
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
        var proxy: MutableList<Proxy>? = null,
        @Deprecated("Use 'static' fun instead")
        var static: MutableList<String>? = null,
        var contentBase: MutableList<String>? = null,
        var client: Client? = null,
        private val mutableStatics: MutableList<Static> = mutableListOf(),
    ) : Serializable {

        @Deprecated("Use constructor with static list instead", level = DeprecationLevel.HIDDEN)
        constructor(
            open: Any = true,
            port: Int? = null,
            proxy: MutableList<Proxy>? = null,
            static: MutableList<String>? = null,
            contentBase: MutableList<String>? = null,
            client: Client? = null,
        ) : this(
            open,
            port,
            proxy,
            static,
            contentBase,
            client,
            mutableListOf()
        )

        /**
         * Adds a static directory to the devServer configuration.
         *
         * Use this to instruct webpack-dev-server that the given [directory] should be
         * included as a static asset source. When [watch] is true, the directory is
         * watched for changes and the dev server will reflect updates automatically.
         *
         * This maps to the `devServer.static` option in the generated webpack config.
         *
         * @param directory path to a directory to be served as static content.
         * @param watch whether the directory should be watched for changes (default: false).
         *
         * https://webpack.js.org/configuration/dev-server/#devserverstatic
         */
        fun static(directory: String, watch: Boolean = false) {
            mutableStatics.add(Static(directory, watch))
        }

        /**
         * Snapshot of all static entries configured via [static].
         */
        val statics: List<Static>
            get() = mutableStatics.toList()

        /**
         * - actually encoded, combined from user input from [static] and [mutableStatics].
         * - 'static' serialized name is to match webpack json config
         * - The type is [Any], because it can be either a [String] or a [Static] instance.
         *
         * https://webpack.js.org/configuration/dev-server/#devserverstatic
         */
        @Suppress("DEPRECATION")
        @get:SerializedName("static")
        internal val actualStatic: List<Any>?
            get() {
                return buildList {
                    addAll(static.orEmpty())
                    addAll(mutableStatics)
                }.takeIf { it.isNotEmpty() }
            }

        internal object DevServerAdapter : JsonSerializer<DevServer> {
            override fun serialize(
                src: DevServer,
                typeOfSrc: Type,
                ctx: JsonSerializationContext,
            ): JsonElement {
                val obj = GsonBuilder().create()
                    .toJsonTree(src, typeOfSrc).asJsonObject
                obj.remove("static")
                obj.remove("mutableStatics")
                obj.add("static", ctx.serialize(src.actualStatic))
                return obj
            }
        }

        data class Client(
            var overlay: Any, /* Overlay | Boolean */
        ) : Serializable {
            data class Overlay(
                var errors: Boolean,
                var warnings: Boolean,
            ) : Serializable
        }

        data class Proxy(
            val context: MutableList<String>,
            val target: String,
            val pathRewrite: MutableMap<String, String>? = null,
            val secure: Boolean? = null,
            val changeOrigin: Boolean? = null,
        ) : Serializable

        /**
         * https://webpack.js.org/configuration/dev-server/#devserverstatic
         */
        class Static(
            val directory: String,
            val watch: Boolean = false,
        ) {
            internal object StaticSerializer : JsonSerializer<Static> {
                override fun serialize(
                    src: Static,
                    typeOfSrc: Type,
                    context: JsonSerializationContext,
                ): JsonElement {
                    val obj = JsonObject()
                    obj.addProperty(
                        "directory",
                        src.directory.quoteRawJsRelativePath()
                    )
                    obj.addProperty("watch", src.watch)
                    return obj
                }
            }

        }
    }

    @Suppress("unused")
    data class Optimization(
        var runtimeChunk: Any?,
        var splitChunks: Any?,
    ) : Serializable

    @Suppress("unused")
    data class WatchOptions(
        var aggregateTimeout: Int? = null,
        var ignored: Any? = null,
    ) : Serializable

    fun save(configFile: File) {
        configFile.writer().use {
            appendTo(it)
        }
    }

    fun appendTo(target: Appendable) {
        with(target) {
            val resolveLoaders = if (resolveLoadersFromKotlinToolingDir) {
                """
                resolveLoader: {
                  modules: ['node_modules', process.env['KOTLIN_TOOLING_DIR']]
                }
                """.trimIndent()
            } else ""

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
                      },
                      $resolveLoaders
                    };
                    
                """.trimIndent()
            )

            appendEntry()
            appendResolveModules()
            appendSourceMaps()
            appendOptimization()
            appendDevServer()
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
            appendLine("config.devServer = ${json(devServer!!).unquoteRawJsRelativePath()};")
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
                        test: /\.m?js${'$'}/,
                        use: ["source-map-loader"],
                        enforce: "pre"
                });
                config.devtool = ${devtool?.let { "'$it'" } ?: false};
                config.ignoreWarnings = [
                    /Failed to parse source map/,
                    /Accessing import\.meta directly is unsupported \(only property access or destructuring is supported\)/
                ]
                
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
                    ${output!!.clean?.let { "clean: $it," } ?: ""}
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
                config.output = config.output || {}
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
                    const tcErrorPlugin = require('kotlin-web-helpers/dist/tc-log-error-webpack');
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

    private fun json(obj: Any) = StringWriter().also {
        GsonBuilder()
            .registerTypeAdapter(DevServer::class.java, DevServer.DevServerAdapter)
            .registerTypeAdapter(DevServer.Static::class.java, DevServer.Static.StaticSerializer)
            .setPrettyPrinting()
            .create()
            .toJson(obj, it)
    }.toString()
}

/**
 * Marks a string value as a raw JavaScript expression to be embedded into the
 * generated webpack.config.js.
 *
 * Gson can only produce JSON (strings, numbers, objects, arrays). However, the
 * file we generate is an executable JavaScript file, and in some places we need
 * to emit JavaScript expressions rather than quoted JSON strings. This helper
 * wraps the receiver string with a special marker understood by [unquoteRawJsRelativePath].
 *
 * The typical flow is:
 * - Kotlin objects are serialized to JSON using Gson.
 * - Certain string fields that must become JS expressions are pre-wrapped using
 *   [quoteRawJsRelativePath].
 * - After serialization, the resulting JSON text is post-processed with
 *   [unquoteRawJsRelativePath] to replace the markers with actual JS code.
 *
 * Note: The current implementation is used for path-like values that will be
 * converted into `require('path').resolve(__dirname, "…")` calls by
 * [unquoteRawJsRelativePath]. The string inside the marker should therefore represent a
 * path relative to the webpack config directory.
 */
internal fun String.quoteRawJsRelativePath(): String {
    return "__RAW_JS__($this)__"
}

/**
 * Replaces special markers produced by [quoteRawJsRelativePath] in a JSON string with real
 * JavaScript expressions suitable for webpack.config.js.
 *
 * Specifically, occurrences of a quoted marker
 * "__RAW_JS__(<value>)__" in the serialized JSON are replaced with
 * `require('path').resolve(__dirname, "<value>")` so that the final output is
 * an executable JS expression instead of a plain string.
 *
 * This is applied to the JSON produced by Gson right before writing the
 * webpack configuration file to disk. It allows parts of the configuration to
 * contain dynamic, executable JavaScript where needed, while still leveraging
 * Gson for the bulk of the serialization.
 */
private fun String.unquoteRawJsRelativePath(): String {
    return replace("\"__RAW_JS__\\((.*?)\\)__\"".toRegex()) { match ->
        "require('path').resolve(__dirname, \"" + match.groupValues[1] + "\")"
    }
}
