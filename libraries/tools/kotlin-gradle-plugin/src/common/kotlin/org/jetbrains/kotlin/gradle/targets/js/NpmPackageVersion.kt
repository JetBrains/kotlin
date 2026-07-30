/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.artifacts.Dependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

data class NpmPackageVersion(
    @Input
    val name: String,
    @Input
    var version: String,
) : RequiredKotlinJsDependency {
    override fun createDependency(objectFactory: ObjectFactory, scope: NpmDependency.Scope): Dependency =
        NpmDependency(objectFactory, scope, name, version)
}

/**
 * Describes an npm dependency, in addition to the requested and resolved versions.
 */
// This class was added because it's hard to modify NpmPackageVersion.
// See KT-88160.
internal data class NpmPackageVersionInternal(
    @Input
    val name: String,
    /**
     * The version originally requested in a `package.json` dependencies block.
     * Typically this will be a dynamic version, e.g. `^1.0.0`.
     *
     * This version is used create a `package.json` in the shared `kotlin-npm-tooling` directory.
     */
    @Input
    val requestedVersion: String,
    /**
     * The exact resolved version, from `package-lock.json`.
     *
     * This is the same version exposed to users in [NpmPackageVersion.version].
     */
    @Input
    val resolvedVersion: String,
)

internal fun NpmVersions.allDependenciesInternal(): List<NpmPackageVersionInternal> =
    allDependencies.map { npv ->
        val requestedVersion = requestedVersions[npv]
            ?: error("NpmVersions is missing requestedVersion for ${npv.name}")
        NpmPackageVersionInternal(
            name = npv.name,
            requestedVersion = requestedVersion,
            resolvedVersion = npv.version,
        )
    }
