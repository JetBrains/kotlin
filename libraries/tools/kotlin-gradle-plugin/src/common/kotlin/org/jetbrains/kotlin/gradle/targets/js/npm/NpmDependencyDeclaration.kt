/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.encodedVersionSpec
import org.jetbrains.kotlin.gradle.targets.web.npm.ObsoleteNpmDependencyModel
import java.io.Serializable

@InternalKotlinGradlePluginApi
@ObsoleteNpmDependencyModel
data class NpmDependencyDeclaration(
    @Input
    @Suppress("DEPRECATION")
    val scope: NpmDependency.Scope,
    @Input
    val name: String,
    @Input
    val version: String,
) : Serializable

@InternalKotlinGradlePluginApi
@ObsoleteNpmDependencyModel
fun NpmDependencyDeclaration.uniqueRepresentation() =
    "$scope $name:$version"

@ObsoleteNpmDependencyModel
internal fun NpmDependencyDeprecated.toNpmDependencyDeclaration(): NpmDependencyDeclaration =
    NpmDependencyDeclaration(
        scope = this.scope,
        name = this.name,
        version = this.version,
    )

@ObsoleteNpmDependencyModel
internal fun KotlinNpmDependency.toNpmDependencyDeclaration(): Provider<NpmDependencyDeclaration> {
    return encodedVersionSpec().map { version ->
        NpmDependencyDeclaration(
            scope = this.dependencyType.toDeprecatedScope(),
            name = this.name,
            version = version,
        )
    }
}

internal fun KotlinNpmDependency.DependencyType.toDeprecatedScope(): NpmDependencyScopeDeprecated =
    when (this) {
        KotlinNpmDependency.DependencyType.NORMAL -> NpmDependencyScopeDeprecated.NORMAL
        KotlinNpmDependency.DependencyType.DEV -> NpmDependencyScopeDeprecated.DEV
        KotlinNpmDependency.DependencyType.OPTIONAL -> NpmDependencyScopeDeprecated.OPTIONAL
        KotlinNpmDependency.DependencyType.PEER -> NpmDependencyScopeDeprecated.PEER
    }

internal fun NpmDependencyDeprecated.toKotlinNpmDependency(
    providers: ProviderFactory,
): KotlinNpmDependency =
    KotlinNpmDependency(
        name = name,
        version = providers.provider { version },
        dependencyType = when (scope) {
            NpmDependencyScopeDeprecated.NORMAL -> KotlinNpmDependency.DependencyType.NORMAL
            NpmDependencyScopeDeprecated.DEV -> KotlinNpmDependency.DependencyType.DEV
            NpmDependencyScopeDeprecated.OPTIONAL -> KotlinNpmDependency.DependencyType.OPTIONAL
            NpmDependencyScopeDeprecated.PEER -> KotlinNpmDependency.DependencyType.PEER
        },
    )
