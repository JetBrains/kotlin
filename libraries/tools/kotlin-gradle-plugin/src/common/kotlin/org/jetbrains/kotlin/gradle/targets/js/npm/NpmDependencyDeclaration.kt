/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import java.io.Serializable

@InternalKotlinGradlePluginApi
data class NpmDependencyDeclaration(
    @Input
    @Suppress("DEPRECATION")
    val scope: NpmDependency.Scope,
    @Input
    val name: String,
    @Input
    val version: String
) : Serializable

@InternalKotlinGradlePluginApi
fun NpmDependencyDeclaration.uniqueRepresentation() =
    "$scope $name:$version"

internal fun NpmDependencyDeprecated.toNpmDependencyDeclaration(): NpmDependencyDeclaration =
    NpmDependencyDeclaration(
        scope = this.scope,
        name = this.name,
        version = this.version,
    )

internal fun KotlinNpmDependency.toNpmDependencyDeclaration(): NpmDependencyDeclaration =
    NpmDependencyDeclaration(
        scope = this.scope.toDeprecatedScope(),
        name = this.name,
        version = this.version,
    )

internal fun KotlinNpmDependency.Scope.toDeprecatedScope(): NpmDependencyScopeDeprecated =
    when (this) {
        KotlinNpmDependency.Scope.NORMAL -> NpmDependencyScopeDeprecated.NORMAL
        KotlinNpmDependency.Scope.DEV -> NpmDependencyScopeDeprecated.DEV
        KotlinNpmDependency.Scope.OPTIONAL -> NpmDependencyScopeDeprecated.OPTIONAL
        KotlinNpmDependency.Scope.PEER -> NpmDependencyScopeDeprecated.PEER
    }
