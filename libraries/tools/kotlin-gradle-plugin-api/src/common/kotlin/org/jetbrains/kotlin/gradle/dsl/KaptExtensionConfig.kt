/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

/**
 * DSL extension used to configure KAPT stub generation and KAPT annotation processing.
 */
interface KaptExtensionConfig {

    /**
     * If `true`, compile classpath should be used to search for annotation processors.
     */
    var includeCompileClasspath: Boolean?

    /**
     * If `true`, skip body analysis if possible.
     */
    var useLightAnalysis: Boolean

    /**
     * If `true`, replace generated or error types with ones from the generated sources.
     */
    var correctErrorTypes: Boolean

    /**
     * If `true`, put initializers on fields when corresponding primary constructor parameters have a default value specified.
     */
    var dumpDefaultParameterValues: Boolean

    /**
     * If `true`, map diagnostic reported on kapt stubs to original locations in Kotlin sources.
     */
    var mapDiagnosticLocations: Boolean

    /**
     * If `true`, show errors on incompatibilities during stub generation.
     */
    var strictMode: Boolean

    /**
     * If `true`, strip @Metadata annotations from stubs.
     */
    var stripMetadata: Boolean

    /**
     * If `true`, show annotation processor stats.
     */
    var showProcessorStats: Boolean

    /**
     * If `true`, detect memory leaks in annotation processors.
     */
    var detectMemoryLeaks: String

    /**
     * Opt-out switch for Kapt caching. Should be used when annotation processors used by this project are suspected of
     * using anything aside from the task inputs in their logic and are not guaranteed to produce the same
     * output on subsequent runs without input changes.
     */
    var useBuildCache: Boolean

    /**
     * If true keeps annotation processors added via `annotationProcessor(..)` configuration for javac java-files compilation
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
     * Configure [KaptJavacOption] used for annotation processing.
     */
    fun javacOptions(action: KaptJavacOption.() -> Unit)

    /**
     * Gets all javac options used for KAPT.
     */
    fun getJavacOptions(): Map<String, String>
}

/**
 * Interface used to specify arguments that are used during KAPT processing.
 */
interface KaptArguments {

    /**
     * Adds argument with the specified name and values.
     */
    fun arg(name: Any, vararg values: Any)
}

/**
 * Interface used to specify javac options that are used during KAPT processing.
 */
interface KaptJavacOption {

    /**
     * Adds an option with name and value.
     */
    fun option(name: Any, value: Any)

    /**
     * Adds an option with name only (no value associated).
     */
    fun option(name: Any)
}
