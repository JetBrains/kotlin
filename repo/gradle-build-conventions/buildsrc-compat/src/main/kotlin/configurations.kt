/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

val NamedDomainObjectContainer<Configuration>.embedded: NamedDomainObjectProvider<Configuration>
    get() = named("embedded")

fun DependencyHandler.embedded(dependencyNotation: Any): Dependency? =
    add("embedded", dependencyNotation)

val NamedDomainObjectContainer<Configuration>.implicitDependencies: NamedDomainObjectProvider<Configuration>
    get() = named("implicitDependencies")

fun DependencyHandler.implicitDependencies(dependencyNotation: Any, configure: Action<ExternalModuleDependency>? = null): Dependency? =
    add("implicitDependencies", dependencyNotation)?.also {
        if (it is ExternalModuleDependency) {
            configure?.execute(it)
        }
    }
