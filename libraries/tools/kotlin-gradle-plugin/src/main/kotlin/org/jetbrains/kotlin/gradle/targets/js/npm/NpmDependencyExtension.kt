/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

interface NpmDependencyExtension {
    operator fun invoke(name: String, version: String): NpmDependency

    operator fun invoke(name: String, directory: File): NpmDependency

    operator fun invoke(directory: File): NpmDependency
}

interface NpmDependencyWithNameOnlyExtension : NpmDependencyExtension {
    operator fun invoke(name: String): NpmDependency
}

internal fun Project.addNpmDependencyExtension() {
    val extensions = (dependencies as ExtensionAware).extensions

    NpmDependency.Scope.values()
        .forEach { scope ->
            val extension = scope.name
                .removePrefix(NpmDependency.Scope.NORMAL.name)
                .toLowerCase()

            extensions
                .add(
                    TypeOf.typeOf(NpmDependencyWithNameOnlyExtension::class.java),
                    lowerCamelCaseName(extension, "npm"),
                    DefaultNpmDependencyExtension(this, scope)
                )
        }
}

private class DefaultNpmDependencyExtension(
    private val project: Project,
    private val scope: NpmDependency.Scope
) : NpmDependencyExtension,
    NpmDependencyWithNameOnlyExtension,
    Closure<NpmDependency>(project.dependencies) {
    override fun invoke(name: String): NpmDependency =
        onlyNameNpmDependency(name)

    override operator fun invoke(name: String, version: String): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            scope = scope
        )

    override operator fun invoke(name: String, directory: File): NpmDependency =
        directoryNpmDependency(
            project = project,
            name = name,
            directory = directory,
            scope = scope
        )

    override operator fun invoke(directory: File): NpmDependency =
        invoke(
            name = moduleName(directory),
            directory = directory
        )

    override fun call(vararg args: Any?): NpmDependency {
        if (args.size > 2) npmDeclarationException(args)

        val arg = args[0]
        return when (arg) {
            is String -> withName(
                name = arg,
                args = *args
            )
            is File -> invoke(arg)
            else -> npmDeclarationException(args)
        }
    }

    private fun withName(name: String, vararg args: Any?): NpmDependency {
        val arg = if (args.size > 1) args[1] else null
        return when (arg) {
            null -> invoke(
                name = name
            )
            is String -> invoke(
                name = name,
                version = arg
            )
            is File -> invoke(
                name = name,
                directory = arg
            )
            else -> npmDeclarationException(args)
        }
    }

    private fun npmDeclarationException(args: Array<out Any?>): Nothing {
        throw IllegalArgumentException(
            """
                            Unable to add NPM dependency by $args
                            - npm('name', 'version') -> name:version
                            - npm(File) -> File.name:File
                            - npm('name', File) -> name:File
                            """.trimIndent()
        )
    }
}
