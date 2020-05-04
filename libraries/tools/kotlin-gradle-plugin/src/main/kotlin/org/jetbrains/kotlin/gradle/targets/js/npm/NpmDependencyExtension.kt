/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import java.io.File

interface NpmDependencyExtension {
    operator fun invoke(name: String, version: String = "*"): NpmDependency

    operator fun invoke(name: String, directory: File): NpmDependency

    operator fun invoke(directory: File): NpmDependency
}

private abstract class AbstractNpmDependencyExtension(
    dependencies: ExtensionAware,
    private val project: Project,
    private val scope: NpmDependency.Scope
) : NpmDependencyExtension, Closure<NpmDependency>(dependencies) {
    override operator fun invoke(name: String, version: String): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            scope = scope
        )

    override operator fun invoke(name: String, directory: File): NpmDependency {
        check(directory.isDirectory) {
            "Dependency on local path should point on directory but $directory found"
        }
        return invoke(
            name = name,
            version = fileVersion(directory)
        )
    }

    override operator fun invoke(directory: File): NpmDependency =
        invoke(
            name = moduleName(directory),
            directory = directory
        )

    override fun call(vararg args: Any?): NpmDependency {
        if (args.size > 2) throw npmDeclarationException(args)

        val arg = args[0]
        return when (arg) {
            is String -> withName(
                name = arg,
                args = *args
            )
            is File -> invoke(arg)
            else -> throw npmDeclarationException(args)
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
            else -> throw npmDeclarationException(args)
        }
    }

    private fun npmDeclarationException(args: Array<out Any?>): IllegalArgumentException {
        return IllegalArgumentException(
            """
                            Unable to add NPM dependency by $args
                            - npm('name') -> name:*
                            - npm('name', 'version') -> name:version
                            - npm('name', File) -> name:File
                            """.trimIndent()
        )
    }
}

private const val NAME_DEPENDENCIES = "npm"
private const val NAME_DEV_DEPENDENCIES = "devNpm"
private const val NAME_PEER_DEPENDENCIES = "peerNpm"

fun Project.addNpmDependencyExtension() {
    val dependencies = this.dependencies as ExtensionAware

    listOf(NpmDependency.Scope.NORMAL, NpmDependency.Scope.DEV, NpmDependency.Scope.PEER).forEach { scope ->
        val name = when (scope) {
            NpmDependency.Scope.DEV -> NAME_DEV_DEPENDENCIES
            NpmDependency.Scope.PEER -> NAME_PEER_DEPENDENCIES
            else -> NAME_DEPENDENCIES
        }

        val dependencyExtension: NpmDependencyExtension = object : AbstractNpmDependencyExtension(dependencies, this, scope) {}

        dependencies
            .extensions
            .add(
                TypeOf.typeOf<NpmDependencyExtension>(NpmDependencyExtension::class.java),
                name,
                dependencyExtension
            )

    }
}
