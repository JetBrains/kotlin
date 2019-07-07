/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.plugins

import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver

internal interface CompilationResolverPlugin {
    fun hookDependencies(
        internalDependencies: MutableSet<KotlinCompilationNpmResolver>,
        externalGradleDependencies: MutableSet<KotlinCompilationNpmResolver.ExternalGradleDependency>,
        externalNpmDependencies: MutableSet<NpmDependency>
    )
}