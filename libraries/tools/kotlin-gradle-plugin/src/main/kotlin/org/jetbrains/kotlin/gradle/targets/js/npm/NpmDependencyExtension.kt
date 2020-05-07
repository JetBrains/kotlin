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

interface BaseNpmDependencyExtension {
    @Deprecated("Declaring NPM dependency without version is forbidden")
    operator fun invoke(name: String): NpmDependency

    operator fun invoke(name: String, version: String): NpmDependency
}

interface NpmDirectoryDependencyExtension {
    operator fun invoke(name: String, directory: File): NpmDependency

    operator fun invoke(directory: File): NpmDependency
}

interface NpmDependencyWithExternalsExtension {
    operator fun invoke(
        name: String,
        version: String,
        generateKotlinExternals: Boolean
    ): NpmDependency

    operator fun invoke(
        name: String,
        directory: File,
        generateKotlinExternals: Boolean
    ): NpmDependency

    operator fun invoke(
        directory: File,
        generateKotlinExternals: Boolean
    ): NpmDependency
}

interface NpmDependencyExtension :
    BaseNpmDependencyExtension,
    NpmDirectoryDependencyExtension,
    NpmDependencyWithExternalsExtension

interface DevNpmDependencyExtension :
    BaseNpmDependencyExtension,
    NpmDirectoryDependencyExtension

interface PeerNpmDependencyExtension :
    BaseNpmDependencyExtension

internal fun Project.addNpmDependencyExtension() {
    val extensions = (dependencies as ExtensionAware).extensions

    values()
        .forEach { scope ->
            val scopePrefix = scope.name
                .removePrefix(NORMAL.name)
                .toLowerCase()

            val type = when (scope) {
                NORMAL, OPTIONAL -> NpmDependencyExtension::class.java
                DEV -> DevNpmDependencyExtension::class.java
                PEER -> PeerNpmDependencyExtension::class.java
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
                    null
                )
                PEER -> NpmDependencyWithoutDirectoryExtension(
                    this,
                    scope,
                    null
                )
            }

            extensions
                .add(
                    TypeOf.typeOf<BaseNpmDependencyExtension>(type),
                    lowerCamelCaseName(scopePrefix, "npm"),
                    extension
                )
        }
}

private abstract class AbstractNpmDependencyExtension(
    protected val project: Project,
    protected val scope: NpmDependency.Scope,
    protected val _defaultGenerateKotlinExternals: Boolean?
) : NpmDependencyExtension,
    DevNpmDependencyExtension,
    PeerNpmDependencyExtension,
    Closure<NpmDependency>(project.dependencies) {
    private val defaultGenerateKotlinExternals: Boolean
        get() = _defaultGenerateKotlinExternals ?: false

    override fun invoke(name: String): NpmDependency =
        onlyNameNpmDependency(name)

    override operator fun invoke(
        name: String,
        version: String,
        generateKotlinExternals: Boolean
    ): NpmDependency =
        NpmDependency(
            project = project,
            name = name,
            version = version,
            scope = scope,
            generateKotlinExternals = generateKotlinExternals
        )

    override fun invoke(name: String, version: String): NpmDependency =
        invoke(
            name = name,
            version = version,
            generateKotlinExternals = defaultGenerateKotlinExternals
        )

    override fun invoke(name: String, directory: File): NpmDependency =
        invoke(
            name = name,
            directory = directory,
            generateKotlinExternals = defaultGenerateKotlinExternals
        )

    override fun invoke(directory: File): NpmDependency =
        invoke(
            directory = directory,
            generateKotlinExternals = defaultGenerateKotlinExternals
        )

    override operator fun invoke(
        directory: File,
        generateKotlinExternals: Boolean
    ): NpmDependency =
        invoke(
            name = moduleName(directory),
            directory = directory,
            generateKotlinExternals = generateKotlinExternals
        )

    override fun call(vararg args: Any?): NpmDependency {
        if (args.size > 3) npmDeclarationException(args)

        val arg = args[0]
        return when (arg) {
            is String -> withName(
                name = arg,
                args = *args
            )
            else -> processNonStringFirstArgument(arg, *args)
        }
    }

    protected abstract fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency

    private fun withName(name: String, vararg args: Any?): NpmDependency {
        val arg1 = if (args.size > 1) args[1] else null
        val generateKotlinExternals = generateKotlinExternalsIfPossible(*args)

        return when (arg1) {
            null -> invoke(
                name = name
            )
            is String -> invoke(
                name = name,
                version = arg1,
                generateKotlinExternals = generateKotlinExternals
            )
            else -> processNamedNonStringSecondArgument(
                name,
                arg1,
                generateKotlinExternals,
                *args
            )
        }
    }

    protected abstract fun processNamedNonStringSecondArgument(
        name: String,
        arg: Any?,
        generateKotlinExternals: Boolean,
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

    protected fun generateKotlinExternalsIfPossible(vararg args: Any?): Boolean {
        val arg2 = (if (args.size > 2) args[2] else null) as? Boolean

        if (arg2 != null && _defaultGenerateKotlinExternals == null) {
            npmDeclarationException(args)
        }

        return arg2 ?: defaultGenerateKotlinExternals
    }
}

private class DefaultNpmDependencyExtension(
    project: Project,
    scope: NpmDependency.Scope,
    defaultGenerateKotlinExternals: Boolean?
) : AbstractNpmDependencyExtension(
    project,
    scope,
    defaultGenerateKotlinExternals
) {
    override operator fun invoke(
        name: String,
        directory: File,
        generateKotlinExternals: Boolean
    ): NpmDependency =
        directoryNpmDependency(
            project = project,
            name = name,
            directory = directory,
            scope = scope,
            generateKotlinExternals = generateKotlinExternals
        )

    override fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency {
        val generateKotlinExternals = generateKotlinExternalsIfPossible(args)

        return when (arg) {
            is File -> invoke(
                directory = arg,
                generateKotlinExternals = generateKotlinExternals
            )
            else -> npmDeclarationException(args)
        }
    }

    override fun processNamedNonStringSecondArgument(
        name: String,
        arg: Any?,
        generateKotlinExternals: Boolean,
        vararg args: Any?
    ): NpmDependency {
        return when (arg) {
            is File -> invoke(
                name = name,
                directory = arg,
                generateKotlinExternals = generateKotlinExternals
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
    defaultGenerateKotlinExternals: Boolean?
) : AbstractNpmDependencyExtension(
    project,
    scope,
    defaultGenerateKotlinExternals
) {
    override fun invoke(
        name: String,
        directory: File,
        generateKotlinExternals: Boolean
    ): NpmDependency =
        npmDeclarationException(arrayOf(name, directory))

    override fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency =
        npmDeclarationException(args)

    override fun processNamedNonStringSecondArgument(
        name: String,
        arg: Any?,
        generateKotlinExternals: Boolean,
        vararg args: Any?
    ): NpmDependency =
        npmDeclarationException(args)
}