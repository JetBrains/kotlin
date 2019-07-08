/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import java.io.File

class DtsResolver(val npmProject: NpmProject) {
    private val typeModules = npmProject.modules.copy(
        packageJsonEntries = listOf("types"),
        indexFileSuffixes = listOf(".d.ts")
    )

    fun getAllDts(externalNpmDependencies: Collection<NpmDependency>): List<Dts> = externalNpmDependencies
        .filter { it.scope != NpmDependency.Scope.DEV }
        .flatMap { it.getDependenciesRecursively() }
        .mapNotNullTo(mutableSetOf()) { typeModules.resolve(it.key)?.let { file -> Dts(file.canonicalFile, it) } }
        .sortedBy { it.inputKey }

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