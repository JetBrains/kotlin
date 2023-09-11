/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action

/**
 * A plugin DSL extension for configuring kapt annotation processing.
 *
 * Use the extension in your build script in the `kapt` block:
 * ```kotlin
 * kapt {
 *    // Your extension configuration
 * }
 * ```
 *
 * See also [Kapt compiler plugin documentation](https://kotlinlang.org/docs/kapt.html).
 */
interface KaptExtensionConfig {

    /**
     * Also loads annotation processors from compile classpath.
     *
     * Default: `null`
     */
    var includeCompileClasspath: Boolean?

    /**
     * Skips analyzing code bodies, if possible.
     *
     * Default: `true`
     */
    var useLightAnalysis: Boolean

    /**
     * Replaces any generated error types with error types from the generated sources.
     *
     * Default: `false`
     */
    var correctErrorTypes: Boolean

    /**
     * Adds initializers to fields whose corresponding primary constructor parameters have a default value specified.
     *
     * Default: `false`
     */
    var dumpDefaultParameterValues: Boolean

    /**
     * Maps diagnostics reported on kapt stubs to their original locations in Kotlin sources.
     *
     * Default: `false`
     */
    var mapDiagnosticLocations: Boolean

    /**
     * Reports any incompatibility errors found during stub generation.
     *
     * Default: `false`
     */
    var strictMode: Boolean

    /**
     * Strips `@Metadata` annotations from stubs.
     *
     * Default: `false`
     */
    var stripMetadata: Boolean

    /**
     * Shows annotation processor statistics in the verbose kapt log output.
     *
     * Default: `false`
     */
    var showProcessorStats: Boolean

    /**
     * Detects memory leaks in annotation processors.
     *
     * Possible values: "default", "paranoid", "none".
     *
     * Default: `default`
     */
    var detectMemoryLeaks: String

    /**
     * Uses the [Gradle build cache](https://docs.gradle.org/current/userguide/build_cache.html) feature for kapt tasks.
     *
     * Set to `false` only when annotation processors used by this project are:
     *   * suspected of using other sources asides from the task inputs in their processing logic
     *   * not guaranteed to produce the same output on subsequent runs without input changes.
     *
     * Default: `true`
     */
    var useBuildCache: Boolean

    /**
     * Keeps annotation processors that are added via the `annotationProcessor(..)` configuration for javac java-files compilation
     *
     * Default: `false`
     */
    var keepJavacAnnotationProcessors: Boolean

    /**
     * Adds annotation processor with the specified [fqName] to the list of processors to run.
     */
    fun annotationProcessor(fqName: String)

    /**
     * Adds annotation processors with the specified [fqName] to the list of processors to run.
     */
    fun annotationProcessors(vararg fqName: String)

    /**
     * Configure [KaptArguments] used for annotation processing.
     */
    fun arguments(action: KaptArguments.() -> Unit)

    /**
     * Configures the [KaptArguments] used for annotation processing.
     */
    fun arguments(action: Action<KaptArguments>) {
        arguments { action.execute(this) }
    }

    /**
     * Configures the [KaptJavacOption] used for annotation processing.
     */
    fun javacOptions(action: KaptJavacOption.() -> Unit)

    /**
     * Configures the [KaptJavacOption] used for annotation processing.
     */
    fun javacOptions(action: Action<KaptJavacOption>) {
        javacOptions { action.execute(this) }
    }

    /**
     * Gets all the javac options used to run kapt annotation processing.
     */
    fun getJavacOptions(): Map<String, String>
}

/**
 * A DSL to specify arguments that are used during kapt processing.
 */
interface KaptArguments {

    /**
     * Adds argument with the specified name and values.
     *
     * Expected [name] and [values] type is [String].
     */
    fun arg(name: Any, vararg values: Any)
}

/**
 * A DSL to specify javac options that are used during kapt processing.
 */
interface KaptJavacOption {

    /**
     * Adds an option with name and value.
     *
     * Expected [name] and [value] type is [String].
     */
    fun option(name: Any, value: Any)

    /**
     * Adds an option with name only.
     *
     * Expected [name] type is [String].
     */
    fun option(name: Any)
}
