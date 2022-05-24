/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KpmGradleDependencyGraph
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleDependencyGraphNode
import org.jetbrains.kotlin.project.model.KpmModule
import org.jetbrains.kotlin.tooling.core.withClosure

internal val KpmGradleDependencyGraph.allDependencyNodes: Iterable<GradleDependencyGraphNode>
    get() = root.withClosure { it.dependenciesByFragment.values.flatten() }

internal val KpmGradleDependencyGraph.allDependencyModules: Iterable<KpmModule>
    get() = allDependencyNodes.map { it.module }
