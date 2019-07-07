/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dukat

import org.jetbrains.kotlin.gradle.targets.js.npm.plugins.RootResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.resolved.KotlinRootNpmResolution
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver

internal class DukatRootResolverPlugin(val resolver: KotlinRootNpmResolver) : RootResolverPlugin {
    val compilations = mutableListOf<DukatCompilationResolverPlugin>()

    override fun createCompilationResolverPlugins(resolver: KotlinCompilationNpmResolver): List<DukatCompilationResolverPlugin> {
        val plugin = DukatCompilationResolverPlugin(resolver)
        compilations.add(plugin)
        return listOf(plugin)
    }

    override fun close(resolution: KotlinRootNpmResolution) {
        println("tests")
        if (resolver.forceFullResolve) {
            // inside idea import
            compilations.forEach {
                it.executeDukatIfNeeded(true, resolution)
            }
        }
    }
}