/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.util

import org.jetbrains.kotlin.commonizer.util.transitiveClosure
import org.jetbrains.kotlin.gradle.kpm.GradleDependencyGraph
import org.jetbrains.kotlin.gradle.kpm.GradleDependencyGraphNode
import org.jetbrains.kotlin.project.model.KotlinModule

internal val GradleDependencyGraph.allDependencyNodes: Iterable<GradleDependencyGraphNode>
    get() = transitiveClosure(root) { dependenciesByFragment.values.flatten() }

internal val GradleDependencyGraph.allDependencyModules: Iterable<KotlinModule>
    get() = allDependencyNodes.map { it.module }
