/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

/**
 * DSL extension that is used to configure Kotlin options for the entire project.
 */
interface KotlinTopLevelExtensionConfig {
    val experimental: ExperimentalExtensionConfig

    /**
     * Version of the core Kotlin libraries that are added to Kotlin compile classpath, unless there is already a dependency added to this
     * project. By default, this version is the same as the version of the used Kotlin Gradle plugin.
     */
    var coreLibrariesVersion: String

    /**
     * Option that tells the compiler if and how to report issues on all public API declarations without explicit visibility or return type.
     */
    var explicitApi: ExplicitApiMode?

    /**
     * Sets [explicitApi] option to report issues as errors.
     */
    fun explicitApi()

    /**
     * Sets [explicitApi] option to report issues as warnings.
     */
    fun explicitApiWarning()
}

interface ExperimentalExtensionConfig {
    var coroutines: Coroutines?
}

/**
 * Different modes that can be used to set the level of issue reporting for [KotlinTopLevelExtensionConfig.explicitApi] option.
 */
enum class ExplicitApiMode(val cliOption: String) {
    /** Report issues as errors. */
    Strict("strict"),

    /** Report issues as warnings. */
    Warning("warning"),

    /** Disable issues reporting. */
    Disabled("disable");

    fun toCompilerArg() = "-Xexplicit-api=$cliOption"
}

enum class Coroutines {
    ENABLE,
    WARN,
    ERROR,
    DEFAULT;

    companion object {
        fun byCompilerArgument(argument: String): Coroutines? =
            values().firstOrNull { it.name.equals(argument, ignoreCase = true) }
    }
}
