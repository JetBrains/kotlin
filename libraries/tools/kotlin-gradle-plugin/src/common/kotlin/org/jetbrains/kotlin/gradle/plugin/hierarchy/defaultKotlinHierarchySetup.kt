/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.NativeTargetShortcutTrace.Companion.nativeTargetShortcutTrace
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.FinaliseRefinesEdges
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinDefaultHierarchyFallbackDependsOnUsageDetected
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinDefaultHierarchyFallbackIllegalTargetNames
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.KotlinDefaultHierarchyFallbackNativeTargetShortcutUsageDetected
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.requiredStage
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal suspend fun Project.setupDefaultKotlinHierarchy() = requiredStage(FinaliseRefinesEdges) setup@{
    val extension = project.multiplatformExtensionOrNull ?: return@setup

    /* User configured a target hierarchy explicitly: No need for our defaults here */
    if (extension.hierarchy.appliedTemplates.isNotEmpty()) return@setup

    /* User explicitly disabled the default target hierarchy by Gradle property */
    if (!kotlinPropertiesProvider.mppApplyDefaultHierarchyTemplate) {
        setupPreMultiplatformStableDefaultDependsOnEdges()
        return@setup
    }

    /*
    User used ios(), tvos(), ... shortcuts.
    This will be incompatible with the default template, as it sets dependsOn edges.
    We detect this case manually to give even better diagnostic
     */
    run check@{
        extension.sourceSets
            .mapNotNull { it.nativeTargetShortcutTrace }
            .toSet()
            .ifEmpty { return@check }
            .onEach { trace -> project.reportDiagnostic(KotlinDefaultHierarchyFallbackNativeTargetShortcutUsageDetected(project, trace)) }

        setupPreMultiplatformStableDefaultDependsOnEdges()
        return@setup
    }


    /*
    User manually added a .dependsOn:
    We fall back to the old behaviour and add the commonMain/commonTest default edges
     */
    run check@{
        val sourceSetsWithDependsOnEdges = extension.sourceSets.filter { sourceSet -> sourceSet.dependsOn.isNotEmpty() }
        if (sourceSetsWithDependsOnEdges.isEmpty()) return@check
        val diagnostic = KotlinDefaultHierarchyFallbackDependsOnUsageDetected(project, sourceSetsWithDependsOnEdges)
        kotlinToolingDiagnosticsCollector.report(project, diagnostic)
        setupPreMultiplatformStableDefaultDependsOnEdges()
        return@setup
    }


    /*
    Using a group of the 'defaultTargetHierarchy' as 'target name' will potentially lead to conflicts e.g.:
    linuxX64("linux") or macosX64("native") can lead to confusion of for 'linuxMain' and 'nativeMain' SourceSets
     */
    run check@{
        val illegalTargetNamesUsed = illegalTargetNamesUsed()
        if (illegalTargetNamesUsed.isEmpty()) return@check
        val diagnostic = KotlinDefaultHierarchyFallbackIllegalTargetNames(project, illegalTargetNamesUsed)
        kotlinToolingDiagnosticsCollector.report(project, diagnostic)
        setupPreMultiplatformStableDefaultDependsOnEdges()
        return@setup
    }

    extension.applyDefaultHierarchyTemplate()
}

private suspend fun Project.illegalTargetNamesUsed(): Set<String> {
    val targets = multiplatformExtension.awaitTargets()
    val targetNames = targets.map { it.name }.toSet()
    return targets.flatMap { it.compilations }.mapNotNull { compilation ->
        val hierarchy = KotlinHierarchyTemplate.default.buildHierarchy(compilation) ?: return@mapNotNull null
        val nodeNames = hierarchy.childrenClosure
            .mapNotNull { it.node as? KotlinHierarchy.Node.Group }
            .map { it.name }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)
        targetNames.intersect(nodeNames)
    }.flatten().toSet()
}

/**
 * Before 1.9.20 (and without any targetHierarchy applied), we just added default dependsOn
 * edges from 'main' compilations defaultSourceSets to 'commonMain' and
 * edges from 'test' compilations defaultSourceSets to 'commonTest
 */
private suspend fun Project.setupPreMultiplatformStableDefaultDependsOnEdges() = multiplatformExtension.targets
    .flatMap { target -> target.compilations }
    .forEach { compilation ->
        val sourceSetTree = KotlinSourceSetTree.orNull(compilation) ?: return@forEach
        val commonSourceSetName = lowerCamelCaseName("common", sourceSetTree.name)
        val commonSourceSet = multiplatformExtension.sourceSets.findByName(commonSourceSetName) ?: return@forEach
        compilation.defaultSourceSet.dependsOn(commonSourceSet)
    }

