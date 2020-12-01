/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

// TODO ensure that resolvers are pluggable + custom dependency kinds (& result kinds?)
// TODO think about state management: unresolved -> (known dependency graph?) ... -> completely resolved
//      it seems to be important to learn whether or not the model is final
interface ModuleDependencyResolver {
    fun resolveDependency(moduleDependency: ModuleDependency): KotlinModule?
}

// TODO merge with ModuleDependencyResolver?
//  Semantically, they are close and may use shared caches and other shared state in the implementations
interface DependencyDiscovery {
    // TODO return dependency graph rather than just iterable?
    // TODO make this a partial function, too
    fun discoverDependencies(fragment: KotlinModuleFragment): Iterable<ModuleDependency>
}