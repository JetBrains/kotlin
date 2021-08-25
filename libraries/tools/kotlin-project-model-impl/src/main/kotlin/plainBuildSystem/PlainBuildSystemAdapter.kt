/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.plainBuildSystem

import org.jetbrains.kotlin.project.modelx.*
import org.jetbrains.kotlin.project.modelx.DefaultVariantMatcher
import org.jetbrains.kotlin.project.modelx.compiler.KpmBuildSystemAdapter
import java.nio.file.Path

/**
 * Pre-configured implementation of [KpmBuildSystemAdapter] i.e. uses resolved [moduleDependencies].
 *
 * Can be useful for build systems that doesn't support interactive and dynamic dependency resolution logic.
 */
class PlainBuildSystemAdapter(
    override val module: KotlinModule,
    private val moduleDependencies: Map<ModuleId, KotlinModuleDependency>,
    private val kpmFileStructure: KpmFileStructure
) : KpmBuildSystemAdapter {
    override fun fragmentSources(fragmentId: FragmentId): List<String> = listOf(kpmFileStructure.sourcesRoot.resolve(fragmentId).toString())

    override fun fragmentMetadataClasspath(fragmentId: FragmentId): String = kpmFileStructure
        .metadataOutputDir
        .resolve(fragmentId)
        .toString()

    override fun fragmentDependencyMetadataArtifacts(fragmentDependency: FragmentDependency): List<String> {
        val moduleId = fragmentDependency.module
        val moduleDependency = moduleDependencies[moduleId] ?: error("Module $moduleId not found")

        return fragmentDependency
            .fragments
            .mapNotNull { moduleDependency.fragmentArtifacts[it]?.map(Path::toString) }
            .flatten()
    }

    override fun variantDependencyArtifacts(fragmentDependency: FragmentDependency): List<String> {
        val moduleId = fragmentDependency.module
        val moduleDependency = moduleDependencies[moduleId] ?: error("Module $moduleId not found")

        return fragmentDependency
            .fragments
            .flatMap { moduleDependency.variantArtifacts[it] ?: emptyList() }
            .map(Path::toString)
    }

    override fun dependencyModule(moduleId: String): KotlinModule {
        val moduleDependency = moduleDependencies[moduleId] ?: error("Module $moduleId not found")
        return moduleDependency.module
    }
}

data class KotlinModuleDependency(
    val fragmentArtifacts: Map<FragmentId, List<Path>>,
    val variantArtifacts: Map<FragmentId, List<Path>>,
    val module: KotlinModule
)