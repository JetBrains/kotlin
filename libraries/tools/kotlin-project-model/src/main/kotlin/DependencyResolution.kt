/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

// TODO ensure that resolvers are pluggable + custom dependency kinds (& result kinds?)
// TODO think about state management: unresolved -> (known dependency graph?) ... -> completely resolved
//      it seems to be important to learn whether or not the model is final
interface ModuleDependencyResolver {
    fun resolveDependency(requestingModule: KotlinModule, moduleDependency: KotlinModuleDependency): KotlinModule?
}

interface KotlinDependencyGraphResolver {
    fun resolveDependencyGraph(requestingModule: KotlinModule): DependencyGraphResolution
}

sealed class DependencyGraphResolution(open val requestingModule: KotlinModule) {
    class Unknown(requestingModule: KotlinModule) : DependencyGraphResolution(requestingModule)
    open class DependencyGraph(requestingModule: KotlinModule, open val root: DependencyGraphNode): DependencyGraphResolution(requestingModule)
}

// TODO: should this be a single graph for all dependency scopes as well, not just for all fragments?
open class DependencyGraphNode(
    open val module: KotlinModule,
    open val dependenciesByFragment: Map<KotlinModuleFragment, Iterable<DependencyGraphNode>>
) {
    override fun toString(): String = "node ${module}"
}