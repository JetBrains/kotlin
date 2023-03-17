@file:Suppress("PackageDirectoryMismatch")

package org.gradle.kotlin.dsl // for convenience use a default package for gradle.kts scripts

import org.gradle.api.Project
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.DokkaBuildProperties

/*
 * Utility functions for accessing Gradle extensions that are created by convention plugins.
 *
 * (Gradle can't generate the nice DSL accessors for the project that defines them)
 *
 * These functions are not needed outside the convention plugins project and should be marked as
 * `internal`
 */


/**
 * workaround for accessing version-catalog in convention plugins
 *
 * See https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
 */
internal val Project.libs : LibrariesForLibs
    get() = extensions.getByType()

/**
 * Retrieves the [dokkaBuild][org.jetbrains.DokkaBuildProperties] extension.
 */
internal val Project.dokkaBuild: DokkaBuildProperties
    get() = extensions.getByType()

/**
 * Configures the [dokkaBuild][org.jetbrains.DokkaBuildProperties] extension.
 */
internal fun Project.dokkaBuild(configure: DokkaBuildProperties.() -> Unit) =
    extensions.configure(configure)
