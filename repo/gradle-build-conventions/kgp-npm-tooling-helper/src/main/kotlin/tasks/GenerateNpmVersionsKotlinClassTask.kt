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
            dependencyVersions = dependenciesWithActualVersions
        )

        createNpmVersionsFile(npmVersions)
    }

    /**
     * Determine the resolved versions of dependencies of the root package.
     *
     * ### Implementation
     *
     * First, get the names of the root package's dependencies.
     *
     * Ignore the versions of the root package's dependencies:
     * they are the requested versions, which might not match the resolved versions.
     *
     * Next, determine the resolved version for each root package dependency.
     *
     * #### Example
     *
     * For example, the requested version of `is-even` is `^1.0.0` (note the pinned version),
     * but the resolved version is `1.0.1` (which is not pinned).
     *
     * ```json
     * // package-lock.json
     * {
     *   "name": "kotlin-npm-tooling",
     *   "packages": {
     *     "": {
     *       "name": "kotlin-npm-tooling",
     *       "dependencies": {
     *         "is-even": "^1.0.0"
     *       }
     *     "node_modules/is-even": {
     *       "version": "1.0.1"
     *     }
     *   }
     * }
     * ```
     */
    private fun computeDependencyVersions(): Map<String, String> {
        val npmLockFile = npmLockFile.get().asFile

        val packageLockJson = PackageLockJson.decodeFromString(npmLockFile.readText())

        val rootPackage = packageLockJson.packages[""]
        requireNotNull(rootPackage) { "Missing root package in ${npmLockFile.invariantSeparatorsPath}" }

        return rootPackage.dependencies.mapValues { (id, _) ->
            val entry = packageLockJson.packages["node_modules/$id"]
                ?: error("Missing entry for $id in ${npmLockFile.invariantSeparatorsPath}")

            when {
                // Must use the requested version if the dep is resolved from a git url,
                // https://docs.npmjs.com/cli/v11/configuring-npm/package-json#git-urls-as-dependencies
                // because `version` is inaccurate for git URLs
                // (Example: Kotlin Karma has a git tag of v6.4.5, but the package version is 6.4.4.)
                entry.resolved != null && entry.resolved.startsWith("git+") ->
                    packageLockJson.packages[""]?.dependencies?.get(id)
                entry.version != null ->
                    entry.version
                else ->
                    null
            } ?: error("Could not find version for $id in ${npmLockFile.invariantSeparatorsPath}. $entry")
        }
    }

    private fun createNpmVersionsClass(
        dependencyVersions: Map<String, String>,
    ): String {
        class NpmDep(
            val name: String,
            val version: String,
        ) {
            /**
             * Pretty camelCased display name, for use as a Kotlin property name.
             */
            val displayName: String =
                name.removePrefix("@")
                    .split("-", "/")
                    .joinToString("") { it.replaceFirstChar { c -> c.uppercase(Locale.ROOT) } }
                    .replaceFirstChar { it.lowercase(Locale.ROOT) }
        }

        val dependencies = dependencyVersions
            .map { (name, version) ->
                NpmDep(name, version)
            }
            .sortedBy { it.displayName }

        return buildString {
            appendLine(copyrightHeader.get().asFile.readText())
            appendLine("package org.jetbrains.kotlin.gradle.targets.js")
            appendLine()
            appendLine("import java.io.Serializable")
            appendLine()
            appendLine("/**")
            appendLine(" * Versions of npm dependencies used by Kotlin Gradle plugin.")
            appendLine(" */")
            appendLine("// Generated class. Do not modify directly!")
            appendLine("class $npmVersionsClassName : Serializable {")
            dependencies.forEach { dep ->
                appendLine("    val ${dep.displayName} = NpmPackageVersion(\"${dep.name}\", \"${dep.version}\")")
            }
            appendLine()
            appendLine("    val allDependencies: List<NpmPackageVersion> = listOf(")
            dependencies.forEach { dep ->
                appendLine("        ${dep.displayName},")
            }
            appendLine("    )")
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
