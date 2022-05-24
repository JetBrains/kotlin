/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.jetbrains.kotlin.project.model.KpmModule
import org.jetbrains.kotlin.project.model.KpmModuleDependency
import org.jetbrains.kotlin.project.model.KpmModuleDependencyResolver
import java.util.*

class GradleKpmCachingModuleDependencyResolver(private val actualResolver: KpmModuleDependencyResolver) : KpmModuleDependencyResolver {
    private val cacheByRequestingModule = WeakHashMap<KpmModule, MutableMap<KpmModuleDependency, KpmModule?>>()

    private fun cacheForRequestingModule(requestingModule: KpmModule) =
        cacheByRequestingModule.getOrPut(requestingModule) { mutableMapOf() }

    override fun resolveDependency(requestingModule: KpmModule, moduleDependency: KpmModuleDependency): KpmModule? =
        cacheForRequestingModule(requestingModule).getOrPut(moduleDependency) {
            actualResolver.resolveDependency(requestingModule, moduleDependency)
        }
}
