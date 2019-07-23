/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.plugins

import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver

internal interface RootResolverPlugin {
    fun createCompilationResolverPlugins(resolver: KotlinCompilationNpmResolver): List<CompilationResolverPlugin>
    fun close(resolution: KotlinRootNpmResolution)
}