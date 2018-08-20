/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure

interface KotlinDependencyHandler {
    fun api(dependencyNotation: Any)
    fun implementation(dependencyNotation: Any)
    fun compileOnly(dependencyNotation: Any)
    fun runtimeOnly(dependencyNotation: Any)
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
