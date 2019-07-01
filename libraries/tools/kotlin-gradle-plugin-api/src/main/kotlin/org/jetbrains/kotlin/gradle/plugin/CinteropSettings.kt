/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions

interface CInteropSettings : Named {

    interface IncludeDirectories {
        fun allHeaders(vararg includeDirs: Any)
        fun allHeaders(includeDirs: Collection<Any>)

        fun headerFilterOnly(vararg includeDirs: Any)
        fun headerFilterOnly(includeDirs: Collection<Any>)
    }

    // TODO: Provide an interface for native compilations.
    val compilation: KotlinCompilation<KotlinCommonOptions>

    val dependencyConfigurationName: String
    var dependencyFiles: FileCollection

    // DSL.
    fun defFile(file: Any)

    fun packageName(value: String)

    fun header(file: Any) = headers(file)
    fun headers(vararg files: Any)
    fun headers(files: FileCollection)

    fun includeDirs(vararg values: Any)
    fun includeDirs(closure: Closure<Unit>)
    fun includeDirs(action: Action<IncludeDirectories>)
    fun includeDirs(configure: IncludeDirectories.() -> Unit)

    fun compilerOpts(vararg values: String)
    fun compilerOpts(values: List<String>)

    fun linkerOpts(vararg values: String)
    fun linkerOpts(values: List<String>)

    fun extraOpts(vararg values: Any)
    fun extraOpts(values: List<Any>)
}
