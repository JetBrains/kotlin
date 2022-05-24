/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

interface KpmDependencyGraphResolver {
    fun resolveDependencyGraph(requestingModule: KpmModule): KpmDependencyGraphResolution
}

sealed class KpmDependencyGraphResolution(open val requestingModule: KpmModule) {
    class Unknown(requestingModule: KpmModule) : KpmDependencyGraphResolution(requestingModule)
    open class KpmDependencyGraph(
        requestingModule: KpmModule, open val root: KpmDependencyGraphNode
    ) : KpmDependencyGraphResolution(requestingModule)
}

// TODO: should this be a single graph for all dependency scopes as well, not just for all fragments?
open class KpmDependencyGraphNode(
    open val module: KpmModule,
    open val dependenciesByFragment: Map<KpmFragment, Iterable<KpmDependencyGraphNode>>
) {
    override fun toString(): String = "node ${module}"
}
