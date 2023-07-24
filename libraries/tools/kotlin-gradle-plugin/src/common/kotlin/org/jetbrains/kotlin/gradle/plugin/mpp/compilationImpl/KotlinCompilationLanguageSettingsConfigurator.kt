/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory.KotlinCompilationImplFactory
import org.jetbrains.kotlin.gradle.plugin.sources.AbstractKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder

/**
 * Wires compilations compiler options into source sets [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.languageSettings].
 *
 * Compiler options will be added into [KotlinCompilationImpl.defaultSourceSet] as main,
 * and into default source set [KotlinSourceSet.dependsOn] direct parents as additional.
 * This configuration should be fully available in [KotlinPluginLifecycle.Stage.AfterFinaliseCompilations] stage.
 */
internal object KotlinCompilationLanguageSettingsConfigurator : KotlinCompilationImplFactory.PreConfigure {
    override fun configure(compilation: KotlinCompilationImpl) {
        val defaultSourceSet = compilation.defaultSourceSet
        (defaultSourceSet.languageSettings as DefaultLanguageSettingsBuilder).compilationCompilerOptions =
            compilation.compilerOptions.options

        compilation.project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges) {
            val sourceSetTree = buildTree(compilation.defaultSourceSet)

            sourceSetTree.nodes
                .single { it.sourceSet == compilation.defaultSourceSet }
                .parents
                .forEach {
                    // In the case of "diamond" hierarchy, the source set may have more than one direct parent.
                    it.sourceSet.addDependentCompilerOptions(compilation.compilerOptions.options)
                }
        }
    }

    private fun KotlinSourceSet.addDependentCompilerOptions(
        compilerOptions: KotlinCommonCompilerOptions
    ) {
        (languageSettings as DefaultLanguageSettingsBuilder)
            .dependentCompilerOptions
            .add(compilerOptions)
    }

    private fun buildTree(leafSourceSet: KotlinSourceSet): SourceSetTree {
        val tree = SourceSetTree()
        tree.populateNodes(SourceSetTreeNode(leafSourceSet))
        tree.cleanupEdges()

        return tree
    }
}

private class SourceSetTree {
    val nodes: MutableSet<SourceSetTreeNode> = mutableSetOf()

    fun populateNodes(
        childNode: SourceSetTreeNode
    ) {
        nodes.add(childNode)

        (childNode.sourceSet as AbstractKotlinSourceSet).dependsOnClosure
            .forAll { sourceSet ->
                val parentNode = SourceSetTreeNode(sourceSet)
                parentNode.addChild(childNode)
                populateNodes(parentNode)
            }
    }

    fun cleanupEdges() {
        // Algorithm:
        // 1. Find root nodes with no parents
        // 2. Remove this node from parents in subtree except direct children
        // 3. Repeat steps 2 and 3 for child node
        nodes
            .filter { it.parents.isEmpty() }
            .forEach { rootNode ->
                rootNode.children.forEach {
                    it.cleanUpSubTree(rootNode)
                }
            }
    }

    private fun SourceSetTreeNode.cleanUpSubTree(parent: SourceSetTreeNode) {
        children.forEach { it.removeParentDeep(parent) }
        children.forEach { it.cleanUpSubTree(this) }
    }

    private fun SourceSetTreeNode.removeParentDeep(parentToRemove: SourceSetTreeNode) {
        parents.remove(parentToRemove)
        children.forEach { it.removeParentDeep(parentToRemove) }
    }
}

private class SourceSetTreeNode(
    val sourceSet: KotlinSourceSet,
) {
    val parents: MutableSet<SourceSetTreeNode> = mutableSetOf()
    val children: MutableSet<SourceSetTreeNode> = mutableSetOf()

    fun addChild(child: SourceSetTreeNode) {
        children.add(child)
        child.parents.add(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SourceSetTreeNode

        return sourceSet == other.sourceSet
    }

    override fun hashCode(): Int {
        return sourceSet.hashCode()
    }

    override fun toString(): String {
        return "SourceSet tree node for ${sourceSet.name}"
    }
}
