/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger

/**
 * Contains all the configurable Kotlin dependencies for a Kotlin DSL entity, like an instance of `KotlinSourceSet`.
 */
interface HasKotlinDependencies {

    /**
     * Configures all dependencies for this entity.
     */
    fun dependencies(configure: KotlinDependencyHandler.() -> Unit)

    /**
     * Configures all dependencies for this entity.
     */
    fun dependencies(configure: Action<KotlinDependencyHandler>)

    /**
     * The name of the Gradle [Configuration]
     * that contains [`api`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types) dependencies.
     *
     * The Gradle `api` configuration should be used to declare dependencies which are exported by the project API.
     *
     * This Gradle configuration is not meant to be resolved.
     */
    val apiConfigurationName: String

    /**
     * The name of the Gradle [Configuration]
     * that contains [`implementation`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types)
     * dependencies.
     *
     * The Gradle `implementation` configuration should be used to declare dependencies which are internal to the component (internal APIs).
     *
     * This Gradle configuration is not meant to be resolved.
     */
    val implementationConfigurationName: String

    /**
     * The name of the Gradle [Configuration]
     * containing [`compileOnly`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types)
     * dependencies.
     *
     * The Gradle `compileOnly` configuration should be used to declare dependencies that participate in compilation,
     * but who need to be added explicitly by consumers at runtime.
     *
     * This Gradle configuration is not meant to be resolved.
     */
    val compileOnlyConfigurationName: String

    /**
     * The name of the Gradle [Configuration]
     * containing [`runtimeOnly`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types)
     * dependencies.
     *
     * The Gradle `runtimeOnly` configuration should be used to declare dependencies that don't participate in compilation,
     * but who are added at runtime.
     *
     * This Gradle configuration is not meant to be resolved.
     */
    val runtimeOnlyConfigurationName: String
}

// Kept in this file to not break API binary compatibility
/**
 * @suppress
 */
@Deprecated(
    message = "Do not use in your build script",
    level = DeprecationLevel.ERROR
)
fun warnNpmGenerateExternals(logger: Logger) {
    logger.warn(
        """
        |
        |==========
        |Please note, Dukat integration in Gradle plugin does not work now.
        |It is in redesigning process.
        |==========
        |
        """.trimMargin()
    )
}
