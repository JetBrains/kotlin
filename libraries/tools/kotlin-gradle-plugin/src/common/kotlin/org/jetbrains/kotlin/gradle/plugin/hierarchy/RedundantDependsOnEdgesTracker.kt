/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerProject
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

internal val KotlinMultiplatformExtension.redundantDependsOnEdgesTracker: RedundantDependsOnEdgesTracker
        by extrasLazyProperty { RedundantDependsOnEdgesTracker() }

internal class RedundantDependsOnEdgesTracker {
    /**
     * A collection to track all depends on edges added through hierarchy templates
     * If a source set already has dependsOn edge we should report it as redundant
     */
    private val dependsOnEdgesAppliedThroughTemplates = mutableSetOf<Pair<KotlinSourceSet, KotlinSourceSet>>()
    private val rememberedEdges = mutableSetOf<Pair<KotlinSourceSet, KotlinSourceSet>>()

    fun addDependsOnEdgeFromTemplate(from: KotlinSourceSet, to: KotlinSourceSet) {
        val edge = from to to
        dependsOnEdgesAppliedThroughTemplates += edge

        /** There is a code coupling with [KotlinSourceSet.dependsOn] method
         * that should invoke [remember] function upon adding an edge.
         * This is a code smell, but it helps isolate this checker. */
        dontRemember {
            from.dependsOn(to)
        }
    }

    /** When set to true, edges will not be remembered when [KotlinSourceSet.dependsOn] is called */
    private var dontRemember = false
    private fun dontRemember(code: () -> Unit) {
        dontRemember = true
        try {
            code()
        } finally {
            dontRemember = false
        }
    }

    /** Should be called from [KotlinSourceSet.dependsOn] method,
     * so depends on edges are tracked and can be distinguished when they added via [addDependsOnEdgeFromTemplate] */
    fun remember(from: KotlinSourceSet, to: KotlinSourceSet) {
        if (dontRemember) return
        rememberedEdges.add(from to to)
    }

    fun reportRedundantDependsOnEdges(project: Project) {
        val allDependsOnEdges = dependsOnEdgesAppliedThroughTemplates.flatMap { (from, to) ->
            to.internal.withDependsOnClosure.map { from to it }
        }.toSet()
        val redundantEdges = rememberedEdges.intersect(allDependsOnEdges)
        if (redundantEdges.isEmpty()) return

        val redundantEdgesToReport = redundantEdges.map { edge ->
            KotlinToolingDiagnostics.RedundantDependsOnEdgesFound.RedundantEdge(
                from = edge.first.name,
                to = edge.second.name,
            )
        }
        project.reportDiagnosticOncePerProject(KotlinToolingDiagnostics.RedundantDependsOnEdgesFound(redundantEdgesToReport))
    }
}