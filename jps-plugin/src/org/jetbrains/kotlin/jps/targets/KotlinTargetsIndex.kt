/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.targets

import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.java.JpsJavaClasspathKind
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleDependency
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.jps.build.KotlinChunk
import org.jetbrains.kotlin.jps.build.KotlinCompileContext
import org.jetbrains.kotlin.jps.build.ModuleBuildTarget
import org.jetbrains.kotlin.jps.model.platform
import org.jetbrains.kotlin.platform.DefaultIdeTargetPlatformKindProvider
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.platform.impl.isJavaScript
import org.jetbrains.kotlin.platform.impl.isJvm
import kotlin.system.measureTimeMillis

class KotlinTargetsIndex(
    val byJpsTarget: Map<ModuleBuildTarget, KotlinModuleBuildTarget<*>>,
    val chunks: List<KotlinChunk>,
    val chunksByJpsRepresentativeTarget: Map<ModuleBuildTarget, KotlinChunk>
)

internal class KotlinTargetsIndexBuilder internal constructor(
    private val uninitializedContext: KotlinCompileContext
) {
    private val byJpsModuleBuildTarget = mutableMapOf<ModuleBuildTarget, KotlinModuleBuildTarget<*>>()
    private val chunks = mutableListOf<KotlinChunk>()

    fun build(): KotlinTargetsIndex {
        val time = measureTimeMillis {
            val jpsContext = uninitializedContext.jpsContext

            // visit all kotlin build targets
            jpsContext.projectDescriptor.buildTargetIndex.getSortedTargetChunks(jpsContext).forEach { chunk ->
                val moduleBuildTargets = chunk.targets.mapNotNull {
                    if (it is ModuleBuildTarget) ensureLoaded(it)
                    else null
                }

                if (moduleBuildTargets.isNotEmpty()) {
                    val kotlinChunk = KotlinChunk(uninitializedContext, moduleBuildTargets)
                    moduleBuildTargets.forEach {
                        it.chunk = kotlinChunk
                    }

                    chunks.add(kotlinChunk)
                }
            }

            calculateChunkDependencies()
        }

        KotlinBuilder.LOG.info("KotlinTargetsIndex created in $time ms")

        return KotlinTargetsIndex(
            byJpsModuleBuildTarget,
            chunks,
            chunks.associateBy { it.representativeTarget.jpsModuleBuildTarget }
        )
    }

    private fun calculateChunkDependencies() {
        chunks.forEach { chunk ->
            val dependencies = mutableSetOf<KotlinModuleBuildTarget.Dependency>()

            chunk.targets.forEach {
                dependencies.addAll(calculateTargetDependencies(it))
            }

            chunk.dependencies = dependencies.toList()
            chunk.dependencies.forEach { dependency ->
                dependency.target.chunk._dependent!!.add(dependency)
            }
        }

        chunks.forEach {
            it.dependent = it._dependent!!.toList()
            it._dependent = null
        }
    }

    private fun calculateTargetDependencies(srcTarget: KotlinModuleBuildTarget<*>): List<KotlinModuleBuildTarget.Dependency> {
        val compileClasspathKind = JpsJavaClasspathKind.compile(srcTarget.isTests)

        val jpsJavaExtensionService = JpsJavaExtensionService.getInstance()
        return srcTarget.module.dependenciesList.dependencies.asSequence()
            .filterIsInstance<JpsModuleDependency>()
            .mapNotNull { dep ->
                val extension = jpsJavaExtensionService.getDependencyExtension(dep)
                    ?.takeIf { it.scope.isIncludedIn(compileClasspathKind) }
                    ?: return@mapNotNull null
                dep.module
                    ?.let { byJpsModuleBuildTarget[ModuleBuildTarget(it, srcTarget.isTests)] }
                    ?.let { KotlinModuleBuildTarget.Dependency(srcTarget, it, extension.isExported) }
            }
            .toList()
    }

    private fun ensureLoaded(target: ModuleBuildTarget): KotlinModuleBuildTarget<*> {
        return byJpsModuleBuildTarget.computeIfAbsent(target) {
            val platform = target.module.platform?.idePlatformKind ?: DefaultIdeTargetPlatformKindProvider.defaultPlatform.idePlatformKind

            when {
                platform.isCommon -> KotlinCommonModuleBuildTarget(uninitializedContext, target)
                platform.isJavaScript -> KotlinJsModuleBuildTarget(uninitializedContext, target)
                platform.isJvm -> KotlinJvmModuleBuildTarget(uninitializedContext, target)
                else -> KotlinUnsupportedModuleBuildTarget(uninitializedContext, target)
            }
        }
    }
}