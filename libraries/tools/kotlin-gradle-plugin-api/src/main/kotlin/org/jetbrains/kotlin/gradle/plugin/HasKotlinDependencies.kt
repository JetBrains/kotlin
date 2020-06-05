/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.util.ConfigureUtil
import java.io.File

interface KotlinDependencyHandler {
    fun api(dependencyNotation: Any): Dependency?
    fun api(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> api(dependency: T, configure: T.() -> Unit): T
    fun api(dependencyNotation: String, configure: Closure<*>) = api(dependencyNotation) { ConfigureUtil.configure(configure, this) }
    fun <T : Dependency> api(dependency: T, configure: Closure<*>) = api(dependency) { ConfigureUtil.configure(configure, this) }

    fun implementation(dependencyNotation: Any): Dependency?
    fun implementation(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> implementation(dependency: T, configure: T.() -> Unit): T
    fun implementation(dependencyNotation: String, configure: Closure<*>) =
        implementation(dependencyNotation) { ConfigureUtil.configure(configure, this) }

    fun <T : Dependency> implementation(dependency: T, configure: Closure<*>) =
        implementation(dependency) { ConfigureUtil.configure(configure, this) }

    fun compileOnly(dependencyNotation: Any): Dependency?
    fun compileOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> compileOnly(dependency: T, configure: T.() -> Unit): T
    fun compileOnly(dependencyNotation: String, configure: Closure<*>) =
        compileOnly(dependencyNotation) { ConfigureUtil.configure(configure, this) }

    fun <T : Dependency> compileOnly(dependency: T, configure: Closure<*>) =
        compileOnly(dependency) { ConfigureUtil.configure(configure, this) }

    fun runtimeOnly(dependencyNotation: Any): Dependency?
    fun runtimeOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> runtimeOnly(dependency: T, configure: T.() -> Unit): T
    fun runtimeOnly(dependencyNotation: String, configure: Closure<*>) =
        runtimeOnly(dependencyNotation) { ConfigureUtil.configure(configure, this) }

    fun <T : Dependency> runtimeOnly(dependency: T, configure: Closure<*>) =
        runtimeOnly(dependency) { ConfigureUtil.configure(configure, this) }

    fun kotlin(simpleModuleName: String): ExternalModuleDependency = kotlin(simpleModuleName, null)
    fun kotlin(simpleModuleName: String, version: String?): ExternalModuleDependency

    fun project(path: String, configuration: String? = null): ProjectDependency =
        project(listOf("path", "configuration").zip(listOfNotNull(path, configuration)).toMap())

    fun project(notation: Map<String, Any?>): ProjectDependency

    @Deprecated("Declaring NPM dependency without version is forbidden")
    fun npm(name: String): Dependency

    fun npm(
        name: String,
        version: String,
        generateExternals: Boolean
    ): Dependency

    fun npm(
        name: String,
        version: String
    ): Dependency = npm(
        name = name,
        version = version,
        generateExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun npm(
        name: String,
        directory: File,
        generateExternals: Boolean
    ): Dependency

    fun npm(
        name: String,
        directory: File
    ): Dependency = npm(
        name = name,
        directory = directory,
        generateExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun npm(
        directory: File,
        generateExternals: Boolean
    ): Dependency

    fun npm(
        directory: File
    ): Dependency = npm(
        directory = directory,
        generateExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun devNpm(
        name: String,
        version: String
    ): Dependency

    fun devNpm(
        name: String,
        directory: File
    ): Dependency

    fun devNpm(
        directory: File
    ): Dependency

    fun optionalNpm(
        name: String,
        version: String,
        generateExternals: Boolean
    ): Dependency

    fun optionalNpm(
        name: String,
        version: String
    ): Dependency = optionalNpm(
        name = name,
        version = version,
        generateExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun optionalNpm(
        name: String,
        directory: File,
        generateExternals: Boolean
    ): Dependency

    fun optionalNpm(
        name: String,
        directory: File
    ): Dependency = optionalNpm(
        name = name,
        directory = directory,
        generateExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun optionalNpm(
        directory: File,
        generateExternals: Boolean
    ): Dependency

    fun optionalNpm(
        directory: File
    ): Dependency = optionalNpm(
        directory = directory,
        generateExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun peerNpm(
        name: String,
        version: String
    ): Dependency
}

interface HasKotlinDependencies {
    fun dependencies(configure: KotlinDependencyHandler.() -> Unit)
    fun dependencies(configureClosure: Closure<Any?>)

    val apiConfigurationName: String
    val implementationConfigurationName: String
    val compileOnlyConfigurationName: String
    val runtimeOnlyConfigurationName: String

    val relatedConfigurationNames: List<String>
        get() = listOf(apiConfigurationName, implementationConfigurationName, compileOnlyConfigurationName, runtimeOnlyConfigurationName)
}
