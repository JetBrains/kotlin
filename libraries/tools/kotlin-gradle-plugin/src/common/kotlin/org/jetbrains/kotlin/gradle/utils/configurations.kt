/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency

const val COMPILE_ONLY = "compileOnly"
const val COMPILE = "compile"
const val IMPLEMENTATION = "implementation"
const val API = "api"
const val RUNTIME_ONLY = "runtimeOnly"
const val RUNTIME = "runtime"
internal const val INTRANSITIVE = "intransitive"

internal fun ConfigurationContainer.createResolvable(name: String): Configuration = create(name).apply {
    isCanBeConsumed = false
}

internal fun ConfigurationContainer.findResolvable(name: String): Configuration? = findByName(name)?.apply {
    // In earlier gradle versions (6.4 for example) some default configurations such as "compile" and "runtime"
    // are both resolvable and consumable by default, here they get fixed to one specific role
    // this prevents from incorrect usage e.g. two consecutive calls findResolvable("foo") findConsumable("foo")
    // would fail and indicate the incorrect usage.
    // Another reason is that before this role-based Configurations API code in KGP sets manually the roles
    // i.e. `findByName("someDefaultConfiguration").isCanBeConsumed = false` now it is just `findResolvable()`
    // In earlier versions of gradle this did make sense i.e. "compileClasspath" configuration was both resolvable and consumable
    // and code above fixes configuration role. However with recent gradle versions these default configurations already come in
    // correct state. But gradle 8.2 reports a warning when you try to "re-set" already set flags on configuration
    // This is why setting a flag is required only when (isCanBeResolved && isCanBeConsumed) are both true.
    if (isCanBeResolved && isCanBeConsumed) {
        isCanBeConsumed = false
    } else {
        check(isCanBeResolved && !isCanBeConsumed) { "$this is not resolvable" }
    }
}

internal fun ConfigurationContainer.maybeCreateResolvable(name: String): Configuration =
    findResolvable(name) ?: createResolvable(name)

internal fun ConfigurationContainer.detachedResolvable(vararg dependencies: Dependency) =
    detachedConfiguration(*dependencies).apply {
        isCanBeConsumed = false
    }

internal fun ConfigurationContainer.createConsumable(name: String): Configuration = create(name).apply {
    isCanBeResolved = false
}

internal fun ConfigurationContainer.findConsumable(name: String): Configuration? = findByName(name)?.apply {
    if (isCanBeResolved && isCanBeConsumed) {
        isCanBeResolved = false
    } else {
        check(!isCanBeResolved && isCanBeConsumed) { "$this is not consumable" }
    }
}

internal fun ConfigurationContainer.maybeCreateConsumable(name: String): Configuration =
    findConsumable(name) ?: createConsumable(name)

internal fun ConfigurationContainer.createDependencyScope(name: String) = create(name).apply {
    isCanBeResolved = false
    isCanBeConsumed = false
}

internal fun ConfigurationContainer.findDependencyScope(name: String): Configuration? = findByName(name)?.apply {
    if (isCanBeResolved && isCanBeConsumed) {
        isCanBeConsumed = false
        isCanBeResolved = false
    } else {
        check(!isCanBeResolved) { "$this is not dependency scope configuration but resolvable one" }
        check(!isCanBeConsumed) { "$this is not dependency scope configuration but consumable one" }
    }
}

internal fun ConfigurationContainer.maybeCreateDependencyScope(name: String): Configuration =
    findDependencyScope(name) ?: createDependencyScope(name)
