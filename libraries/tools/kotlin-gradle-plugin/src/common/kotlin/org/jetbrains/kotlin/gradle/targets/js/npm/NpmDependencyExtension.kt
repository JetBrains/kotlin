/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import groovy.lang.Closure
import groovy.lang.GString
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import org.jetbrains.kotlin.gradle.plugin.warnNpmGenerateExternals
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency.Scope.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

interface BaseNpmDependencyExtension {
    operator fun invoke(name: String, version: String): NpmDependency
}

interface NpmDirectoryDependencyExtension : BaseNpmDependencyExtension {
    operator fun invoke(name: String, directory: File): NpmDependency

    operator fun invoke(directory: File): NpmDependency
}

interface NpmDependencyWithExternalsExtension : BaseNpmDependencyExtension {
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    operator fun invoke(
        name: String,
        version: String,
        generateExternals: Boolean
    ): NpmDependency
}

interface NpmDirectoryDependencyWithExternalsExtension : NpmDirectoryDependencyExtension {
    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    operator fun invoke(
        name: String,
        directory: File,
        generateExternals: Boolean
    ): NpmDependency

    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    operator fun invoke(
        directory: File,
        generateExternals: Boolean
    ): NpmDependency
}

interface NpmDependencyExtension :
    BaseNpmDependencyExtension,
    NpmDependencyWithExternalsExtension,
    NpmDirectoryDependencyWithExternalsExtension,
    NpmDirectoryDependencyExtension

interface DevNpmDependencyExtension :
    BaseNpmDependencyExtension,
    NpmDirectoryDependencyExtension

interface PeerNpmDependencyExtension :
    BaseNpmDependencyExtension

internal fun Project.addNpmDependencyExtension() {
    val extensions = (dependencies as ExtensionAware).extensions

    values()
        .forEach { scope ->
            val type = when (scope) {
                NORMAL, OPTIONAL -> NpmDependencyExtension::class.java
                DEV -> DevNpmDependencyExtension::class.java
                PEER -> PeerNpmDependencyExtension::class.java
            }

            val extension: BaseNpmDependencyExtension = when (scope) {
                NORMAL, OPTIONAL -> DefaultNpmDependencyExtension(
                    this,
                    scope,
                )

                DEV -> DefaultDevNpmDependencyExtension(
                    this
                )

                PEER -> DefaultPeerNpmDependencyExtension(
                    this
                )
            }

            extensions
                .add(
                    TypeOf.typeOf<BaseNpmDependencyExtension>(type),
                    scopePrefix(scope),
                    extension
                )
        }
}

private fun scopePrefix(scope: NpmDependency.Scope): String {
    val scopePrefix = scope.name
        .removePrefix(NORMAL.name)
        .toLowerCaseAsciiOnly()

    return lowerCamelCaseName(scopePrefix, "npm")
}

private abstract class NpmDependencyExtensionDelegate(
    protected val project: Project,
    protected val scope: NpmDependency.Scope,
) : NpmDependencyExtension,
    DevNpmDependencyExtension,
    PeerNpmDependencyExtension,
    Closure<NpmDependency>(project.dependencies) {
    override operator fun invoke(
        name: String,
        version: String,
    ): NpmDependency =
        NpmDependency(
            objectFactory = project.objects,
            name = name,
            version = version,
            scope = scope,
        )

    override fun invoke(directory: File): NpmDependency =
        invoke(
            name = moduleName(directory),
            directory = directory,
        )

    override fun call(vararg args: Any?): NpmDependency {
        if (args.size > 2) npmDeclarationException(args)

        return when (val arg = args[0]) {
            is String -> withName(
                name = arg,
                args = args
            )

            is GString -> withName(
                name = arg.toString(),
                args = args
            )

            else -> processNonStringFirstArgument(arg, *args)
        }
    }

    protected abstract fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency

    private fun withName(name: String, vararg args: Any?): NpmDependency {
        return when (val arg1 = if (args.size > 1) args[1] else null) {
            null -> throw IllegalArgumentException(
                "NPM dependency '$name' doesn't have version. Please, set version explicitly."
            )

            is String -> invoke(
                name = name,
                version = arg1,
            )

            is GString -> invoke(
                name = name,
                version = arg1.toString(),
            )

            else -> processNamedNonStringSecondArgument(
                name,
                arg1,
                *args
            )
        }
    }

    protected abstract fun processNamedNonStringSecondArgument(
        name: String,
        arg: Any?,
        vararg args: Any?
    ): NpmDependency

    protected fun npmDeclarationException(args: Array<out Any?>): Nothing {
        throw IllegalArgumentException(
            """
            |Unable to add NPM dependency with scope '${scope.name.toLowerCaseAsciiOnly()}' by ${args.joinToString { "'$it'" }}
            |Possible variants:
            |${possibleVariants().joinToString("\n") { "- ${it.first} -> ${it.second}" }}
            """.trimMargin()
        )
    }

    protected open fun possibleVariants(): List<Pair<String, String>> {
        return listOf("${scopePrefix(scope)}('name', 'version')" to "name:version")
    }
}

private class DefaultNpmDependencyExtension(
    private val project: Project,
    scope: NpmDependency.Scope,
) : Closure<NpmDependency>(project.dependencies),
    NpmDependencyExtension {
    private val delegate = defaultNpmDependencyDelegate(
        project,
        scope,
    )

    override fun invoke(name: String, version: String): NpmDependency =
        delegate.invoke(name, version)

    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    override fun invoke(name: String, version: String, generateExternals: Boolean): NpmDependency  {
        warnNpmGenerateExternals(project.logger)
        return invoke(name, version)
    }

    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    override fun invoke(name: String, directory: File, generateExternals: Boolean): NpmDependency  {
        warnNpmGenerateExternals(project.logger)
        return invoke(name, directory)
    }

    @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
    override fun invoke(directory: File, generateExternals: Boolean): NpmDependency  {
        warnNpmGenerateExternals(project.logger)
        return invoke(directory)
    }

    override fun invoke(name: String, directory: File): NpmDependency =
        delegate.invoke(name, directory)

    override fun invoke(directory: File): NpmDependency =
        delegate.invoke(directory)

    override fun call(vararg args: Any?): NpmDependency =
        delegate.call(*args)
}

private class DefaultDevNpmDependencyExtension(
    project: Project
) : Closure<NpmDependency>(project.dependencies),
    DevNpmDependencyExtension {
    private val delegate = defaultNpmDependencyDelegate(
        project,
        DEV,
    )

    override fun invoke(name: String, version: String): NpmDependency =
        delegate.invoke(name, version)

    override fun invoke(name: String, directory: File): NpmDependency =
        delegate.invoke(name, directory)

    override fun invoke(directory: File): NpmDependency =
        delegate.invoke(directory)

    override fun call(vararg args: Any?): NpmDependency =
        delegate.call(*args)
}

private fun defaultNpmDependencyDelegate(
    project: Project,
    scope: NpmDependency.Scope,
): NpmDependencyExtensionDelegate {
    return object : NpmDependencyExtensionDelegate(
        project,
        scope,
    ) {
        @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
        override fun invoke(directory: File, generateExternals: Boolean): NpmDependency  {
            warnNpmGenerateExternals(project.logger)
            return invoke(directory)
        }

        @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
        override fun invoke(name: String, version: String, generateExternals: Boolean): NpmDependency  {
            warnNpmGenerateExternals(project.logger)
            return invoke(name, version)
        }

        @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
        override fun invoke(name: String, directory: File, generateExternals: Boolean): NpmDependency  {
            warnNpmGenerateExternals(project.logger)
            return invoke(name, directory)
        }

        override operator fun invoke(
            name: String,
            directory: File,
        ): NpmDependency =
            directoryNpmDependency(
                objectFactory = project.objects,
                scope = scope,
                name = name,
                directory = directory,
            )

        override fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency {
            return when (arg) {
                is File -> invoke(
                    directory = arg,
                )

                else -> npmDeclarationException(args)
            }
        }

        override fun processNamedNonStringSecondArgument(
            name: String,
            arg: Any?,
            vararg args: Any?
        ): NpmDependency {
            return when (arg) {
                is File -> invoke(
                    name = name,
                    directory = arg,
                )

                else -> npmDeclarationException(args)
            }
        }

        override fun possibleVariants(): List<Pair<String, String>> {
            return super.possibleVariants() + listOf(
                "${scopePrefix(scope)}(File)" to "File.name:File",
                "${scopePrefix(scope)}('name', File)" to "name:File"
            )
        }
    }
}

private class DefaultPeerNpmDependencyExtension(
    project: Project
) : Closure<NpmDependency>(project.dependencies),
    PeerNpmDependencyExtension {
    private val delegate: NpmDependencyExtensionDelegate = object : NpmDependencyExtensionDelegate(
        project,
        PEER,
    ) {
        @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
        override fun invoke(directory: File, generateExternals: Boolean): NpmDependency  {
            warnNpmGenerateExternals(project.logger)
            return invoke(directory)
        }

        @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
        override fun invoke(name: String, version: String, generateExternals: Boolean): NpmDependency  {
            warnNpmGenerateExternals(project.logger)
            return invoke(name, version)
        }

        @Deprecated("Dukat integration is in redesigning process. Now it does not work.")
        override fun invoke(name: String, directory: File, generateExternals: Boolean): NpmDependency  {
            warnNpmGenerateExternals(project.logger)
            return invoke(name, directory)
        }

        override fun invoke(
            name: String,
            directory: File,
        ): NpmDependency =
            npmDeclarationException(arrayOf(name, directory))

        override fun processNonStringFirstArgument(arg: Any?, vararg args: Any?): NpmDependency =
            npmDeclarationException(args)

        override fun processNamedNonStringSecondArgument(
            name: String,
            arg: Any?,
            vararg args: Any?
        ): NpmDependency =
            npmDeclarationException(args)
    }

    override fun invoke(name: String, version: String): NpmDependency =
        delegate.invoke(name, version)

    override fun call(vararg args: Any?): NpmDependency =
        delegate.call(*args)
}
