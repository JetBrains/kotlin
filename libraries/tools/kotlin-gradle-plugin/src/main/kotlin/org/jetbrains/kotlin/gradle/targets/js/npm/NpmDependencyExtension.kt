/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import org.jetbrains.kotlin.gradle.plugin.DEFAULT_GENERATE_KOTLIN_EXTERNALS
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency.Scope.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.io.File

interface NpmDependencyExtension {
    @Deprecated("Declaring NPM dependency without version is forbidden")
    operator fun invoke(name: String): NpmDependency

    operator fun invoke(name: String, version: String): NpmDependency
}

interface NpmDirectoryDependencyExtension : NpmDependencyExtension {
    operator fun invoke(name: String, directory: File): NpmDependency

    operator fun invoke(directory: File): NpmDependency
}

internal fun Project.addNpmDependencyExtension() {
    val extensions = (dependencies as ExtensionAware).extensions

    values()
        .forEach { scope ->
            val scopePrefix = scope.name
                .removePrefix(NORMAL.name)
                .toLowerCase()

            val type = when (scope) {
                NORMAL, DEV, OPTIONAL -> NpmDirectoryDependencyExtension::class.java
                PEER -> NpmDependencyExtension::class.java
            }

            val extension = when (scope) {
                NORMAL, OPTIONAL -> DefaultNpmDependencyExtension(
                    this,
                    scope,
                    DEFAULT_GENERATE_KOTLIN_EXTERNALS
                )
                DEV -> DefaultNpmDependencyExtension(
                    this,
                    scope,
                    false
                )
                PEER -> NpmDependencyWithoutDirectoryExtension(
                    this,
                    scope,
                    false
                )
            }

            extensions
                .add(
                    TypeOf.typeOf<NpmDependencyExtension>(type),
                    lowerCamelCaseName(scopePrefix, "npm"),
                    extension
                )
        }
}

private abstract class AbstractNpmDependencyExtension(
    protected val project: Project,
    protected val scope: NpmDependency.Scope,
    protected val defaultGenerateKotlinExternals: Boolean
) : NpmDependencyExtension,
    NpmDirectoryDependencyExtension,
    Closure<NpmDependency>(project.dependencies) {
    override fun invoke(name: String): NpmDependency =
        onlyNameNpmDependency(name)

    override operator fun invoke(name: String, version: String): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            scope = scope,
            generateKotlinExternals = defaultGenerateKotlinExternals
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
            else -> processNonStringFirstArgument(args)
        }
    }

    protected abstract fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency

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
            else -> processNonStringNameArgument(name, arg, args)
        }
    }

    protected abstract fun processNonStringNameArgument(
        name: String,
        arg: Any?,
        vararg args: Any?
    ): NpmDependency

    protected fun npmDeclarationException(args: Array<out Any?>): Nothing {
        throw IllegalArgumentException(
            """
                            |Unable to add NPM with scope '$scope' dependency by ${args.joinToString()}
                            |Possible variants:
                            |${possibleVariants().joinToString("\n") { "- ${it.first} -> ${it.second}" }}
            """.trimMargin()
        )
    }

    protected open fun possibleVariants(): List<Pair<String, String>> {
        return listOf("npm('name', 'version')" to "name:version")
    }
}

private class DefaultNpmDependencyExtension(
    project: Project,
    scope: NpmDependency.Scope,
    defaultGenerateKotlinExternals: Boolean
) : AbstractNpmDependencyExtension(
    project,
    scope,
    defaultGenerateKotlinExternals
) {
    override fun invoke(name: String): NpmDependency =
        onlyNameNpmDependency(name)

    override operator fun invoke(name: String, directory: File): NpmDependency =
        directoryNpmDependency(
            project = project,
            name = name,
            directory = directory,
            scope = scope,
            generateKotlinExternals = defaultGenerateKotlinExternals
        )

    override operator fun invoke(directory: File): NpmDependency =
        invoke(
            name = moduleName(directory),
            directory = directory
        )

    override fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency {
        return when (arg) {
            is File -> invoke(arg)
            else -> npmDeclarationException(args)
        }
    }

    override fun processNonStringNameArgument(
        name: String,
        arg: Any?,
        vararg args: Any?
    ): NpmDependency {
        return when (arg) {
            is File -> invoke(
                name = name,
                directory = arg
            )
            else -> npmDeclarationException(args)
        }
    }

    override fun possibleVariants(): List<Pair<String, String>> {
        return super.possibleVariants() + listOf(
            "npm(File)" to "File.name:File",
            "npm('name', File)" to "name:File"
        )
    }
}

private class NpmDependencyWithoutDirectoryExtension(
    project: Project,
    scope: NpmDependency.Scope,
    defaultGenerateKotlinExternals: Boolean
) : AbstractNpmDependencyExtension(
    project,
    scope,
    defaultGenerateKotlinExternals
) {
    override fun invoke(name: String, directory: File): NpmDependency =
        npmDeclarationException(arrayOf(name, directory))

    override fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency =
        npmDeclarationException(args)

    override fun processNonStringNameArgument(name: String, arg: Any?, vararg args: Any?): NpmDependency =
        npmDeclarationException(args)
}