/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmDependencyGraph
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmDependencyGraphNode
import org.jetbrains.kotlin.project.model.KpmModule
import org.jetbrains.kotlin.tooling.core.withClosure

internal val GradleKpmDependencyGraph.allDependencyNodes: Iterable<GradleKpmDependencyGraphNode>
    get() = root.withClosure { it.dependenciesByFragment.values.flatten() }

internal val GradleKpmDependencyGraph.allDependencyModules: Iterable<KpmModule>
    get() = allDependencyNodes.map { it.module }
