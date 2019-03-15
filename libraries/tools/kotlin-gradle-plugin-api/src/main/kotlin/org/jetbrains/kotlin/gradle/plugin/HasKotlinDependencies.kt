/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.util.ConfigureUtil

interface KotlinDependencyHandler {
    fun api(dependencyNotation: Any): Dependency?
    fun api(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> api(dependency: T, configure: T.() -> Unit): T
    fun api(dependencyNotation: String, configure: Closure<*>) = api(dependencyNotation) { ConfigureUtil.configure(configure, this) }
    fun <T : Dependency> api(dependency: T, configure: Closure<*>) = api(dependency) { ConfigureUtil.configure(configure, this) }

    fun implementation(dependencyNotation: Any): Dependency?
    fun implementation(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> implementation(dependency: T, configure: T.() -> Unit): T
    fun implementation(dependencyNotation: String, configure: Closure<*>) = implementation(dependencyNotation) { ConfigureUtil.configure(configure, this) }
    fun <T : Dependency> implementation(dependency: T, configure: Closure<*>) = implementation(dependency) { ConfigureUtil.configure(configure, this) }

    fun compileOnly(dependencyNotation: Any): Dependency?
    fun compileOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> compileOnly(dependency: T, configure: T.() -> Unit): T
    fun compileOnly(dependencyNotation: String, configure: Closure<*>) = compileOnly(dependencyNotation) { ConfigureUtil.configure(configure, this) }
    fun <T : Dependency> compileOnly(dependency: T, configure: Closure<*>) = compileOnly(dependency) { ConfigureUtil.configure(configure, this) }

    fun runtimeOnly(dependencyNotation: Any): Dependency?
    fun runtimeOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency
    fun <T : Dependency> runtimeOnly(dependency: T, configure: T.() -> Unit): T
    fun runtimeOnly(dependencyNotation: String, configure: Closure<*>) = runtimeOnly(dependencyNotation) { ConfigureUtil.configure(configure, this) }
    fun <T : Dependency> runtimeOnly(dependency: T, configure: Closure<*>) = runtimeOnly(dependency) { ConfigureUtil.configure(configure, this) }

    fun kotlin(simpleModuleName: String): ExternalModuleDependency = kotlin(simpleModuleName, null)
    fun kotlin(simpleModuleName: String, version: String?): ExternalModuleDependency

    fun project(path: String, configuration: String? = null): ProjectDependency =
        project(listOf("path", "configuration").zip(listOfNotNull(path, configuration)).toMap())

    fun project(notation: Map<String, Any?>): ProjectDependency
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
