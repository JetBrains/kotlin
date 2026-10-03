/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import java.io.File
import java.io.Serializable

/**
 * Fake NodeJS module directory created from Gradle external module
 */
data class GradleNodeModule(val name: String, val version: String, val path: File) : Serializable {
    val semver: SemVer
        get() = SemVer.from(version)

    @get:Synchronized
    val dependencies: Set<NpmDependencyDeclaration> by lazy {
        val packageJson = readPackageJsonFile(path.resolve("package.json"))
        mapOf(
            NpmDependency.Scope.NORMAL to packageJson.dependencies,
            NpmDependency.Scope.PEER to packageJson.peerDependencies,
            NpmDependency.Scope.OPTIONAL to packageJson.optionalDependencies,
            NpmDependency.Scope.DEV to packageJson.devDependencies,
        ).flatMap { (scope, dependencies) ->
            dependencies.orEmpty().map { (name, version) -> NpmDependencyDeclaration(scope, name, version) }
        }.toSet()
    }
}
