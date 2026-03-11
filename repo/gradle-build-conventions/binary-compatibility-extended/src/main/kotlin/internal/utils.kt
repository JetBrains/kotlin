/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.bcv.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.getByType

/**
 * Mark this [Configuration] as one that should be used to declare dependencies in
 * [Project.dependencies] block.
 *
 * Declarable Configurations should be extended by [resolvable] and [consumable] Configurations.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = false
 * isCanBeDeclared = true
 * ```
 */
internal fun Configuration.declarable() {
    isCanBeResolved = false
    isCanBeConsumed = false
    isCanBeDeclared = true
}


/**
 * Mark this [Configuration] as one that will be consumed by other subprojects.
 *
 * ```
 * isCanBeResolved = false
 * isCanBeConsumed = true
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.consumable() {
    isCanBeResolved = false
    isCanBeConsumed = true
    isCanBeDeclared = false
}


/**
 * Mark this [Configuration] as one that will consume artifacts from other subprojects (also known as 'resolving')
 *
 * ```
 * isCanBeResolved = true
 * isCanBeConsumed = false
 * isCanBeDeclared = false
 * ```
 */
internal fun Configuration.resolvable() {
    isCanBeResolved = true
    isCanBeConsumed = false
    isCanBeDeclared = false
}


/**
 * Create a new [NamedDomainObjectContainer], using
 * [org.gradle.kotlin.dsl.domainObjectContainer]
 * (but [T] is `reified`).
 *
 * @param[factory] an optional factory for creating elements
 * @see org.gradle.kotlin.dsl.domainObjectContainer
 */
internal inline fun <reified T : Any> ObjectFactory.domainObjectContainer(
    factory: NamedDomainObjectFactory<T>? = null,
): NamedDomainObjectContainer<T> =
    if (factory == null) {
        domainObjectContainer(T::class)
    } else {
        domainObjectContainer(T::class, factory)
    }


/**
 * [Add][ExtensionContainer.add] a value (from [valueProvider]) with [name], and return the value.
 *
 * Adding an extension is especially useful for improving the DSL in build scripts when [T] is a
 * [NamedDomainObjectContainer].
 * Using an extension will allow Gradle to generate
 * [type-safe model accessors](https://docs.gradle.org/current/userguide/kotlin_dsl.html#kotdsl:accessor_applicability)
 * for added types.
 *
 * ([name] should match the property name. This has to be done manually because using a
 * delegated-property provider means Gradle can't introspect the types properly, so it fails to
 * create accessors).
 */
internal inline fun <reified T : Any> ExtensionContainer.adding(
    name: String,
    crossinline valueProvider: () -> T,
): T {
    val value: T = valueProvider()
    add<T>(name, value)
    return value
}

internal val Project.sourceSets: SourceSetContainer
    get() = extensions.getByType()

/** @see org.gradle.util.Path */
internal typealias GradlePath = org.gradle.util.Path

/** Create a new [GradlePath]. */
internal fun GradlePath(path: String): GradlePath = GradlePath.path(path)

private fun Project.isRootProject(): Boolean = this == rootProject

internal val Project.fullPath: String
    get() = when {
        this.isRootProject() -> path + rootProject.name
        else -> name
    }
