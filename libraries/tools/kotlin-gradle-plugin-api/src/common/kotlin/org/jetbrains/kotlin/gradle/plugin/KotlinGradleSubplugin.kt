/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File

/**
 * Represents an option for a Kotlin compiler plugin, defined by a key and a value.
 *
 * All options are mapped to the following Kotlin compiler argument format:
 *
 * ```
 * "plugin:$pluginId:$key=$value"
 * ```
 * 
 * `pluginId` is the [KotlinCompilerPluginSupportPlugin.getCompilerPluginId].
 *
 * Options are added to the relevant compilation task inputs. To exclude options from task inputs, use the [InternalSubpluginOption].
 * For options that provide file paths, add the file paths to the [FilesSubpluginOption].
 *
 * @constructor Creates an instance of option with the given key and lazy-initialized value.
 * @param key The key associated with this option.
 * @param lazyValue The lazily-initialized value associated with the key.
 */
open class SubpluginOption(val key: String, private val lazyValue: Lazy<String>) {
    constructor(key: String, value: String) : this(key, lazyOf(value))

    /**
     * The value of this option.
     *
     * The [lazyValue] is realized on first access.
     */
    val value: String get() = lazyValue.value
}

/**
 * Represents a Kotlin compiler plugin option that provides paths to one or many [files][File].
 *
 * @param files the [Iterable] of [files][File] to pass as a Kotlin compiler plugin option.
 * They are passed as an absolute path separated by [File.pathSeparator].
 * @param kind Configures how files are treated in Gradle input/output checks.
 */
class FilesSubpluginOption(
    key: String,
    val files: Iterable<File>,
    val kind: FilesOptionKind = FilesOptionKind.INTERNAL,
    lazyValue: Lazy<String> = lazy { files.joinToString(File.pathSeparator) { it.normalize().absolutePath } }
) : SubpluginOption(key, lazyValue) {

    constructor(
        key: String,
        files: List<File>,
        kind: FilesOptionKind = FilesOptionKind.INTERNAL,
        value: String? = null
    ) : this(key, files, kind, lazy { value ?: files.joinToString(File.pathSeparator) { it.normalize().absolutePath } })
}

/**
 * Represents a Kotlin compiler plugin option where the [key] accepts multiple values. For example:
 *
 * ```
 * plugin:plugin_id:composite_key=value1;value2;value3
 * ```
 *
 * @param lazyValue The combined value for this [key]. From the example above, this is `value1;value2;value3`.
 * @param originalOptions The content of [lazyValue] as a list of [SubpluginOptions][SubpluginOption]
 * which are used for Gradle task input/output checks.
 */
class CompositeSubpluginOption(
    key: String,
    lazyValue: Lazy<String>,
    val originalOptions: List<SubpluginOption>
) : SubpluginOption(key, lazyValue) {
    constructor(key: String, value: String, originalOptions: List<SubpluginOption>) : this(key, lazyOf(value), originalOptions)
}

/**
  * Defines how the [FilesSubpluginOption] is used for Gradle task input/output checks.
 */
enum class FilesOptionKind {
    /**
     * This option is an implementation detail and should not be treated as an input or output of the task.
     *
     * It is similar to the Gradle [Internal][org.gradle.api.tasks.Internal] annotation.
     */
    INTERNAL

    // More options might be added when use cases appear for them,
    // such as output directories, inputs or classpath options.
}

/**
 * Represents a Kotlin compiler plugin option that is excluded from Gradle task input/output checks.
 */
open class InternalSubpluginOption(key: String, value: String) : SubpluginOption(key, value)

/**
 * Represents a container containing all the settings for a specific Kotlin compiler plugin.
 *
 * This container is available for all Kotlin compilation tasks as a [org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile.pluginOptions] input.
 */
open class CompilerPluginConfig {

    @get:Internal
    protected val optionsByPluginId = mutableMapOf<String, MutableList<SubpluginOption>>()

    /**
     * Retrieves all options grouped by their corresponding plugin IDs.
     */
    fun allOptions(): Map<String, List<SubpluginOption>> = optionsByPluginId

    /**
     * Adds a Kotlin compiler plugin option to the collection of options associated with the given plugin ID.
     */
    fun addPluginArgument(pluginId: String, option: SubpluginOption) {
        optionsByPluginId.getOrPut(pluginId) { mutableListOf() }.add(option)
    }

    /**
     * Combines all options for Kotlin compiler plugins into a single map of input arguments for the task.
     */
    @Input
    fun getAsTaskInputArgs(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        optionsByPluginId.forEach { (id, subpluginOptions) ->
            result += computeForSubpluginId(id, subpluginOptions)
        }
        return result
    }

    private fun computeForSubpluginId(subpluginId: String, subpluginOptions: List<SubpluginOption>): Map<String, String> {
        // There might be several options with the same key. We group them together
        // and add an index to the Gradle input property name to resolve possible duplication:
        val result = mutableMapOf<String, String>()
        val pluginOptionsGrouped = subpluginOptions.groupBy { it.key }
        for ((optionKey, optionsGroup) in pluginOptionsGrouped) {
            optionsGroup.forEachIndexed { index, option ->
                val indexSuffix = if (optionsGroup.size > 1) ".$index" else ""
                when (option) {
                    is InternalSubpluginOption -> return@forEachIndexed

                    is CompositeSubpluginOption -> {
                        val subpluginIdWithWrapperKey = "$subpluginId.$optionKey$indexSuffix"
                        result += computeForSubpluginId(subpluginIdWithWrapperKey, option.originalOptions)
                    }

                    is FilesSubpluginOption -> when (option.kind) {
                        FilesOptionKind.INTERNAL -> Unit
                    }.run { /* exhaustive when */ }

                    else -> {
                        result["$subpluginId." + option.key + indexSuffix] = option.value
                    }
                }
            }
        }
        return result
    }
}

/**
 * Gradle plugin implementing support for a Kotlin compiler plugin.
 *
 * All supplemental Gradle plugins for Kotlin compiler plugins must implement this interface and be applied
 * to the project as a regular Gradle [Plugin] before the Kotlin plugin inspects the project model
 * in an [afterEvaluate][org.gradle.api.Project.afterEvaluate] handler.
 *
 * The Kotlin Gradle plugin then uses the [isApplicable] method to check
 * if the Kotlin compiler plugin works for the [KotlinCompilations][KotlinCompilation] of the project.
 * For applicable [KotlinCompilations][KotlinCompilation], the Kotlin Gradle plugin calls [applyToCompilation] later
 * during the configuration phase.
 */
interface KotlinCompilerPluginSupportPlugin : Plugin<Project> {

    /**
     * Apply this plugin to the given target [Project].
     *
     * The default implementation does nothing.
     */
    override fun apply(target: Project) = Unit

    /**
     * Determines if the Kotlin compiler plugin is applicable for the provided [kotlinCompilation].
     *
     * @return `true` if the plugin is applicable to the provided compilation unit, `false` otherwise.
     */
    fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean

    /**
     * Applies this Kotlin compiler plugin to the specified applicable [kotlinCompilation] compilation
     * with the provided compiler plugin options.
     *
     * @return A provider for a list of Kotlin compiler plugin options that affect the Kotlin compilation process.
     * The [Provider][org.gradle.api.provider.Provider] returned by this function may not be queried
     * if the compilation is skipped in the current build.
     */
    fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>>

    /**
     * Retrieves the unique identifier for the Kotlin compiler plugin configuration options.
     *
     * This unique identifier is used to pass Kotlin compiler plugin-specific configuration options to the Kotlin compilation process
     * via the `-P` compiler argument.
     */
    fun getCompilerPluginId(): String

    /**
     * Retrieves the Maven coordinates of the Kotlin compiler plugin associated with this supplemental Gradle plugin.
     *
     * The Kotlin Gradle plugin adds this artifact to the relevant Gradle configurations so it can be automatically provided
     * for compilation.
     */
    fun getPluginArtifact(): SubpluginArtifact

    /**
     * Retrieves the Maven coordinates of the legacy Kotlin/Native-specific compiler plugin associated with this supplemental Gradle plugin.
     *
     * It's used only if Gradle is configured not to use the Kotlin/Native embeddable compiler JAR file.
     * (with `kotlin.native.useEmbeddableCompilerJar=false` project property).
     *
     * Otherwise, [getPluginArtifact] is used by default.
     */
    fun getPluginArtifactForNative(): SubpluginArtifact? = null
}

/**
 * Represents Maven coordinates for the Kotlin compiler plugin artifact.
 *
 * @property groupId The Maven group ID of the artifact.
 * @property artifactId The Maven artifact name.
 * @property version The optional version of the artifact. The default value is `null`. In this case, the version of the Kotlin Gradle plugin is used.
 *
 * @see [KotlinCompilerPluginSupportPlugin.getPluginArtifact]
 */
open class SubpluginArtifact(val groupId: String, val artifactId: String, val version: String? = null)

/**
 * @suppress this class should not be a part of KGP-API
 */
class JetBrainsSubpluginArtifact(artifactId: String) : SubpluginArtifact(groupId = "org.jetbrains.kotlin", artifactId = artifactId)