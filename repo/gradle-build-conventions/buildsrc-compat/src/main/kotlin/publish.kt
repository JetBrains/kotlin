import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

val NamedDomainObjectContainer<Configuration>.publishedRuntime: NamedDomainObjectProvider<Configuration> get() = named("publishedRuntime")

fun DependencyHandler.publishedRuntime(dependencyNotation: Any): Dependency? =
    add("publishedRuntime", dependencyNotation)

fun DependencyHandler.publishedRuntime(
    dependencyNotation: String,
    dependencyConfiguration: Action<ExternalModuleDependency>
): ExternalModuleDependency =
    addDependencyTo(this, "publishedRuntime", dependencyNotation, dependencyConfiguration)


val NamedDomainObjectContainer<Configuration>.publishedCompile: NamedDomainObjectProvider<Configuration> get() = named("publishedCompile")

fun DependencyHandler.publishedCompile(dependencyNotation: Any): Dependency? =
    add("publishedCompile", dependencyNotation)

fun DependencyHandler.publishedCompile(
    dependencyNotation: String,
    dependencyConfiguration: Action<ExternalModuleDependency>
): ExternalModuleDependency =
    addDependencyTo(this, "publishedCompile", dependencyNotation, dependencyConfiguration)

