/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
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

@Suppress("DEPRECATION")
internal fun NpmDependency.toDeclaration(): NpmDependencyDeclaration =
    NpmDependencyDeclaration(
        scope = this.scope,
        name = this.name,
        version = this.version,
    )
