/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

interface ModuleDependencyResolver {
    fun resolveDependency(moduleDependency: ModuleDependency): KotlinModule?
}

interface DependencyDiscovery {
    // TODO return dependency graph rather than just iterable?
    // TODO make this a partial function, too
    fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<ModuleDependency>
}