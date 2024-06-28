/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

fun HasKotlinDependencies.allDependenciesConfigurationNames() = listOfNotNull(
    apiConfigurationName,
    implementationConfigurationName,
    compileOnlyConfigurationName,
    runtimeOnlyConfigurationName
)

fun KotlinCompilation<*>.allCompilationDependenciesConfigurationNames() = allDependenciesConfigurationNames() + listOfNotNull(
    compileDependencyConfigurationName,
    runtimeDependencyConfigurationName,
)