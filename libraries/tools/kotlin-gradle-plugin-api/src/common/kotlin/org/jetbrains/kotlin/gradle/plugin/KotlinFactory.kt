/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtensionConfig

/** An API used by third-party plugins to integration with the Kotlin Gradle plugin. */
interface KotlinFactory {
    /** Instance of DSL object that should be used to configure Kotlin compilation pipeline. */
    val kotlinExtension: KotlinTopLevelExtensionConfig

    /** Adds a compiler plugin dependency to this project. This can be e.g a Maven coordinate or a project included in the build. */
    fun addCompilerPluginDependency(dependency: Provider<Any>)

    /** Returns a [FileCollection] that contains all compiler plugins classpath for this project. */
    fun getCompilerPlugins(): FileCollection
}