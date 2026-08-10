/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.kgpnpmtooling.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.NONE
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.build.kgpnpmtooling.internal.PackageLockJson
import java.util.*

/**
 * Generates a Kotlin source file containing the versions of KGP's npm tooling dependencies.
 *
 * Uses the `package-lock.json` file as the source of truth, which contains the resolved versions of all dependencies.
 * (The `package.json` file only contains the _requested_ versions, which may be different.)
 */
@CacheableTask
abstract class GenerateNpmVersionsKotlinClassTask
internal constructor() : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(NONE)
    @get:NormalizeLineEndings
    val npmLockFile: Provider<RegularFile>
        get() = npmToolingProjectDir.file("package-lock.json")

    @get:InputFile
    @get:PathSensitive(NONE)
    @get:NormalizeLineEndings
    abstract val copyrightHeader: RegularFileProperty

    @get:Internal
    abstract val npmToolingProjectDir: DirectoryProperty

    private val npmVersionsClassName = "NpmVersions"

    @TaskAction
    protected fun generate() {
        val dependenciesWithActualVersions = computeDependencyVersions()

        val npmVersions = createNpmVersionsClass(
            dependencies = dependenciesWithActualVersions
        )

        createNpmVersionsFile(npmVersions)
    }

    /**
     * Determine the requested and resolved versions of dependencies of the root package.
     *
     * ### Implementation
     *
     * First, get the names of the root package's dependencies.
     *
     * Next, determine the resolved version for each root package dependency.
     *
     * #### Example
     *
     * For example, the requested version of `is-even` is `^1.0.0` (note the pinned version),
     * but the resolved version is `1.0.1` (which is not pinned).
     *
     * ```json5
     * // package-lock.json
     * {
     *   "name": "kotlin-npm-tooling",
     *   "packages": {
     *     "": {
     *       "name": "kotlin-npm-tooling",
     *       "dependencies": {
     *         "is-even": "^1.0.0"
     *       }
     *     },
     *     "node_modules/is-even": {
     *         "version": "1.0.1"
     *     }
     *   }
     * }
     * ```
     */
    private fun computeDependencyVersions(): SortedSet<NpmDep> {
        val npmLockFile = npmLockFile.get().asFile

        val packageLockJson = PackageLockJson.decodeFromString(npmLockFile.readText())

        val rootPackage = packageLockJson.packages[""]
        requireNotNull(rootPackage) { "Missing root package in ${npmLockFile.invariantSeparatorsPath}" }

        return rootPackage.dependencies.map { (id, requestedVersion) ->

            val entry = packageLockJson.packages["node_modules/$id"]
                ?: error("Missing entry for $id in ${npmLockFile.invariantSeparatorsPath}")

            val resolvedVersion =
                when {
                    // Must use the requested version if the dep is resolved from a git url,
                    // https://docs.npmjs.com/cli/v11/configuring-npm/package-json#git-urls-as-dependencies
                    // because `version` is inaccurate for git URLs
                    // (Example: Kotlin Karma has a git tag of v6.4.5, but the package version is 6.4.4.)
                    entry.resolved != null && entry.resolved.startsWith("git+") ->
                        requestedVersion
                    entry.version != null ->
                        entry.version
                    else ->
                        null
                } ?: error("Could not find version for $id in ${npmLockFile.invariantSeparatorsPath}. $entry")

            NpmDep(
                name = id,
                requestedVersion = requestedVersion,
                resolvedVersion = resolvedVersion,
            )
        }.toSortedSet()
    }

    private class NpmDep(
        val name: String,
        val requestedVersion: String,
        val resolvedVersion: String,
    ) : Comparable<NpmDep> {
        /**
         * Pretty camelCased display name, for use as a Kotlin property name.
         */
        val displayName: String =
            name.removePrefix("@")
                .split("-", "/")
                .joinToString("") { it.replaceFirstChar { c -> c.uppercase(Locale.ROOT) } }
                .replaceFirstChar { it.lowercase(Locale.ROOT) }

        override fun compareTo(other: NpmDep): Int =
            displayName.compareTo(other.displayName)
    }

    private fun createNpmVersionsClass(
        dependencies: SortedSet<NpmDep>,
    ): String {
        return buildString {
            appendLine(copyrightHeader.get().asFile.readText())
            appendLine("package org.jetbrains.kotlin.gradle.targets.js")
            appendLine()
            appendLine("import java.io.Serializable")
            appendLine()
            appendLine("/**")
            appendLine(" * Versions of npm dependencies used by Kotlin Gradle plugin.")
            appendLine(" *")
            appendLine(" * The versions are the resolved versions, extracted from KGP's `kotlin-npm-tooling/package-lock.json`.")
            appendLine(" */")
            appendLine("// Generated class. Do not modify directly!")
            appendLine("class $npmVersionsClassName : Serializable {")
            dependencies.forEach { dep ->
                appendLine("    val ${dep.displayName} = NpmPackageVersion(\"${dep.name}\")")
            }
            appendLine()
            appendLine("    val allDependencies: List<NpmPackageVersion> = listOf(")
            dependencies.forEach { dep ->
                appendLine("        ${dep.displayName},")
            }
            appendLine("    )")
            appendLine()
            appendLine("    /**")
            appendLine("     * The original requested versions from KGP's `kotlin-npm-tooling/package.json`.")
            appendLine("     */")
            appendLine("    internal val requestedVersions: Map<NpmPackageVersion, String> = mapOf(")
            dependencies.forEach { dep ->
                appendLine("        ${dep.displayName} to \"${dep.requestedVersion}\",")
            }
            appendLine("    )")
            appendLine()
            appendLine("    internal companion object {")
            appendLine()
            appendLine("        /**")
            appendLine("         * Create a new [NpmPackageVersion], ")
            appendLine("         * using the default version from [defaultVersions].")
            appendLine("         */")
            appendLine("        private fun NpmPackageVersion(name: String): NpmPackageVersion =")
            appendLine("            NpmPackageVersion(")
            appendLine("                name = name,")
            appendLine("                version = defaultVersions.getValue(name),")
            appendLine("            )")
            appendLine()
            appendLine("        /**")
            appendLine("         * The default versions from KGP's `kotlin-npm-tooling/package.json`.")
            appendLine("         * The values declared in [allDependencies] may be overwritten by users.")
            appendLine("         */")
            appendLine("        internal val defaultVersions: Map<String, String> = mapOf(")
            dependencies.forEach { dep ->
                appendLine("            \"${dep.name}\" to \"${dep.resolvedVersion}\",")
            }
            appendLine("        )")
            appendLine("    }")
            appendLine("}")
        }
    }

    private fun createNpmVersionsFile(content: String) {
        val outputDir = outputDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        outputDir
            .resolve("$npmVersionsClassName.kt")
            .writeText(content)
    }
}
