/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.builders.AdditionalRootsProviderService
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.jps.model.expectedByModules
import org.jetbrains.kotlin.jps.model.sourceSetModules
import java.io.File

class KotlinSourceRootProvider : AdditionalRootsProviderService<JavaSourceRootDescriptor>(JavaModuleBuildTargetType.ALL_TYPES) {
    override fun getAdditionalRoots(
        target: BuildTarget<JavaSourceRootDescriptor>,
        dataPaths: BuildDataPaths?
    ): List<JavaSourceRootDescriptor> {
        val moduleBuildTarget = target as? ModuleBuildTarget ?: return listOf()
        val module = moduleBuildTarget.module

        val result = mutableListOf<JavaSourceRootDescriptor>()

        // Add source roots with type KotlinSourceRootType.
        //
        // Note: `KotlinSourceRootType` cannot be supported directly, since `SourceRootDescriptors` are computed by
        // `ModuleBuildTarget.computeAllTargets`. `ModuleBuildTarget` is required for incremental compilation.
        // We cannot define our own `ModuleBuildTarget` since it is final and `ModuleBuildTarget` supports only `JavaSourceRootDescriptor`.
        // So the only one way to support `KotlinSourceRootType` is to add a fake `JavaSourceRootDescriptor` for each source root with that type.
        val kotlinSourceRootType = if (target.isTests) KotlinSourceRootType.TestSource else KotlinSourceRootType.Source
        module.getSourceRoots(kotlinSourceRootType).forEach {
            result.add(
                JavaSourceRootDescriptor(
                    it.file,
                    target,
                    false,
                    false,
                    it.properties.packagePrefix,
                    setOf()
                )
            )
        }

        // new multiplatform model support:
        module.sourceSetModules.forEach { sourceSetModule ->
            addModuleSourceRoots(result, sourceSetModule, target, false)
        }

        // legacy multiplatform model support:
        module.expectedByModules.forEach { commonModule ->
            // At the moment, incremental compilation is not supported by K2Metadata compiler.
            // To avoid long running compilation of common modules, we do not run K2Metadata at all:
            // instead all the common source roots are transitively added to the final platform modules.
            val isRecursive = true

            addModuleSourceRoots(result, commonModule, target, isRecursive)
        }

        return result
    }

    private fun addModuleSourceRoots(
        result: MutableList<JavaSourceRootDescriptor>,
        module: JpsModule,
        target: ModuleBuildTarget,
        recursive: Boolean = false
    ) {
        for (commonSourceRoot in module.sourceRoots) {
            val isCommonTestsRootType = commonSourceRoot.rootType.isTestsRootType
            if (isCommonTestsRootType != null && target.isTests == isCommonTestsRootType) {
                val javaSourceRootProperties = commonSourceRoot.properties as? JavaSourceRootProperties

                result.add(
                    KotlinIncludedModuleSourceRoot(
                        commonSourceRoot.file,
                        target,
                        javaSourceRootProperties?.isForGeneratedSources ?: false,
                        false,
                        javaSourceRootProperties?.packagePrefix ?: "",
                        setOf()
                    )
                )
            }
        }

        if (recursive) {
            JpsJavaExtensionService.dependencies(module)
                .also {
                    if (!target.isTests) it.productionOnly()
                }
                .processModules {
                    addModuleSourceRoots(
                        result,
                        it,
                        ModuleBuildTarget(it, target.targetType as JavaModuleBuildTargetType),
                        true
                    )
                }
        }
    }
}

private val JpsModuleSourceRootType<*>.isTestsRootType
    get() = when (this) {
        is KotlinSourceRootType -> this == KotlinSourceRootType.TestSource
        is KotlinResourceRootType -> this == KotlinResourceRootType.TestResource
        // for compatibility:
        is JavaSourceRootType -> this == JavaSourceRootType.TEST_SOURCE
        is JavaResourceRootType -> this == JavaResourceRootType.TEST_RESOURCE
        else -> null
    }

class KotlinIncludedModuleSourceRoot(
    root: File,
    target: ModuleBuildTarget,
    isGenerated: Boolean,
    isTemp: Boolean,
    packagePrefix: String,
    excludes: Set<File>
) : JavaSourceRootDescriptor(root, target, isGenerated, isTemp, packagePrefix, excludes)