/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies.impl

import kotlin.script.experimental.dependencies.ExternalDependenciesResolver

fun ExternalDependenciesResolver.Options.value(name: DependenciesResolverOptionsName) =
    value(name.key)

fun ExternalDependenciesResolver.Options.flag(name: DependenciesResolverOptionsName) =
    flag(name.key)

operator fun MutableMap<String, String>.set(key: DependenciesResolverOptionsName, value: String) {
    put(key.key, value)
}

/**
 * These names are for convenience only.
 * They don't have to be implemented in all resolvers.
 */
enum class DependenciesResolverOptionsName(optionName: String? = null) {
    TRANSITIVE,
    SCOPE;

    val key = optionName ?: name.toLowerCase()
}

val ExternalDependenciesResolver.Options.transitive
    get() = flag(DependenciesResolverOptionsName.TRANSITIVE)

val ExternalDependenciesResolver.Options.dependencyScopes
    get() = value(DependenciesResolverOptionsName.SCOPE)?.split(",")
