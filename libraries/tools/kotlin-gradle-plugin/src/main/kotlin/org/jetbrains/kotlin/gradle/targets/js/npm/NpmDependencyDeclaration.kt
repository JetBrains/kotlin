/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.tasks.Input

data class NpmDependencyDeclaration(
    @Input
    val scope: NpmDependency.Scope,
    @Input
    val name: String,
    @Input
    val version: String,
    @Input
    val generateExternals: Boolean
)

fun NpmDependencyDeclaration.uniqueRepresentation() =
    "$scope $name:$version, $generateExternals"

internal fun NpmDependency.toDeclaration(): NpmDependencyDeclaration =
    NpmDependencyDeclaration(
        scope = this.scope,
        name = this.name,
        version = this.version,
        generateExternals = this.generateExternals
    )