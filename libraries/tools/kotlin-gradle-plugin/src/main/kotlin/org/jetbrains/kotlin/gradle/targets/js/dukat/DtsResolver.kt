/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency.Scope.NORMAL
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency.Scope.OPTIONAL
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import java.io.File

class DtsResolver(val npmProject: NpmProject) {
    private val typeModules = npmProject.modules.copy(
        // https://www.typescriptlang.org/docs/handbook/declaration-files/publishing.html
        packageJsonEntries = listOf("types", "typings"),
        indexFileSuffixes = listOf(".d.ts")
    )

    fun getAllDts(
        externalNpmDependencies: Collection<NpmDependency>,
        considerGeneratingFlag: Boolean = true
    ): List<Dts> {
        val buildStatsService = KotlinBuildStatsService.getInstance()
        return externalNpmDependencies
            .asSequence()
            .filter { !considerGeneratingFlag || it.generateExternals }
            .filter { it.scope == NORMAL || it.scope == OPTIONAL }
            .mapNotNullTo(mutableSetOf()) { dependency ->
                getDtsFromDependency(dependency, considerGeneratingFlag)
            }
            .sortedBy { it.inputKey }
            .toList()
            .also {
                buildStatsService?.report(BooleanMetrics.JS_GENERATE_EXTERNALS, it.isNotEmpty())
            }
    }

    private fun getDtsFromDependency(
        dependency: NpmDependency,
        considerGeneratingFlag: Boolean
    ): Dts? {
        val dts = typeModules.resolve(dependency.key)
            ?.let { file ->
                Dts(file.canonicalFile, dependency)
            }
        if (dts == null && considerGeneratingFlag) {
            warningOnMissedDTs(dependency)
        }
        return dts
    }

    private fun warningOnMissedDTs(dependency: NpmDependency) {
        npmProject.project.logger.warn(
            """
            No `types` or `typings` found for '$dependency'.
            To find d.ts for dependency, fields `types` and `typings` should be declared in `package.json`
            """.trimIndent()
        )
    }

    class Dts(val file: File, val npmDependency: NpmDependency) {
        val inputKey: String
            get() = npmDependency.key + "@" + npmDependency.resolvedVersion + "#" + npmDependency.integrity

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Dts

            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            return file.hashCode()
        }

        override fun toString(): String = inputKey
    }
}