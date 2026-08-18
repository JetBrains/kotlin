/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.artifacts.Dependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.utils.newInstance
import java.io.Serializable

data class NpmPackageVersion(
    @Input
    val name: String,
    /**
     * The version that will be used to install this dependency.
     *
     */
    // Defaults to the exact resolved version from the lockfile,
    // not the requested version from a `package.json` (which might be a flexible, ranged version).
    @Input
    var version: String,
) : RequiredKotlinJsDependency {
    override fun createDependency(objectFactory: ObjectFactory, scope: NpmDependency.Scope): Dependency =
        NpmDependency(objectFactory, scope, name, version)
}

/**
 * Describes an npm dependency, in addition to the requested and resolved versions.
 */
// This class was added because it's hard to modify NpmPackageVersion, see KT-88160.
// Also because NpmPackageVersion doesn't support the Provider API KT-77145.
internal abstract class NpmPackageVersionInternal : Serializable {

    /**
     * npm package name.
     *
     * @see NpmPackageVersion.name
     */
    @get:Input
    abstract val name: Property<String>

    /**
     * The version originally requested in a `package.json` dependencies block.
     * Typically this will be a dynamic version, e.g. `^1.0.0`.
     *
     * This version is used create a `package.json` in the shared `kotlin-npm-tooling` directory.
     */
    @get:Input
    abstract val requestedVersion: Property<String>

    /**
     * The exact resolved version, from `package-lock.json`.
     *
     * This is the same version exposed to users in [NpmPackageVersion.version].
     */
    @get:Input
    abstract val resolvedVersion: Property<String>
}

internal fun ObjectFactory.NpmPackageVersionInternal(
    configure: (NpmPackageVersionInternal) -> Unit,
): NpmPackageVersionInternal =
    newInstance<NpmPackageVersionInternal>().apply(configure)

internal fun NpmVersions.allDependenciesInternal(
    objects: ObjectFactory,
    providers: ProviderFactory,
): List<NpmPackageVersionInternal> =
    allDependencies.map { npv ->
        val defaultVersion = NpmVersions.defaultVersions[npv.name]

        val npvVersion = providers.provider { npv.version }

        // Determine the version to use in the `package.json` file.
        val requestedVersion = npvVersion.map { version ->
            if (version != defaultVersion) {
                // If the version doesn't match the default version, then it's been modified by a user.
                // In this case, the modified version should be used instead of the 'requested' version.
                version
            } else {
                // If the version _does_ match the default, then instead use the _requested_ version.
                // Using the requested version is important for Yarn:
                // KGP's embedded `yarn.lock` file contains the _requested_ versions.
                // Yarn will ignore the lockfile if the `package.json` doesn't have exactly the same versions.
                requestedVersions.entries
                    .firstOrNull { (k, _) -> k.name == npv.name }
                    ?.value
                    ?: error("NpmVersions is missing requestedVersion for ${npv.name}. $requestedVersions")
            }
        }

        objects.NpmPackageVersionInternal { npvInternal ->
            npvInternal.name.set(npv.name)
            npvInternal.requestedVersion.set(requestedVersion)
            npvInternal.resolvedVersion.set(npvVersion)
        }
    }
