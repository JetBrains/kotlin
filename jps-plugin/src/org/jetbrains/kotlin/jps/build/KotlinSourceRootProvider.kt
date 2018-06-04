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
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.jps.model.expectedByModules
import java.io.File

/**
 * - adds roots with KotlinSourceRootType as JavaSourceRootDescriptor (see note below)
 * - for Multiplatform Projects: adds all the source roots of the expectedBy modules to the platform modules.
 *
 * Note: `KotlinSourceRootType` cannot be supported directly, since `SourceRootDescriptors` are computed by
 * `ModuleBuildTarget.computeAllTargets`. `ModuleBuildTarget` is required for incremental compilation.
 * We cannot define our own `ModuleBuildTarget` since it is final and `ModuleBuildTarget` supports only `JavaSourceRootDescriptor`.
 * So the only one way to support `KotlinSourceRootType` is to add a fake `JavaSourceRootDescriptor` for each source root with that type.
 */
class KotlinSourceRootProvider : AdditionalRootsProviderService<JavaSourceRootDescriptor>(JavaModuleBuildTargetType.ALL_TYPES) {
    override fun getAdditionalRoots(
        target: BuildTarget<JavaSourceRootDescriptor>,
        dataPaths: BuildDataPaths?
    ): List<JavaSourceRootDescriptor> {
        val moduleBuildTarget = target as? ModuleBuildTarget ?: return listOf()
        val module = moduleBuildTarget.module

        val result = mutableListOf<JavaSourceRootDescriptor>()

        // add source roots with type KotlinSourceRootType
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

        // add source roots of the expectedBy modules
        module.expectedByModules.forEach { commonModule ->
            addCommonModuleSourceRoots(result, commonModule, target)
        }

        return result
    }

    private fun addCommonModuleSourceRoots(
        result: MutableList<JavaSourceRootDescriptor>,
        commonModule: JpsModule,
        target: ModuleBuildTarget
    ) {
        for (commonSourceRoot in commonModule.sourceRoots) {
            val isCommonTestsRootType = commonSourceRoot.rootType.isTestsRootType
            if (isCommonTestsRootType != null && target.isTests == isCommonTestsRootType) {
                val javaSourceRootProperties = commonSourceRoot.properties as? JavaSourceRootProperties

                result.add(
                    KotlinCommonModuleSourceRoot(
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

class KotlinCommonModuleSourceRoot(
    root: File,
    target: ModuleBuildTarget,
    isGenerated: Boolean,
    isTemp: Boolean,
    packagePrefix: String,
    excludes: Set<File>
) : JavaSourceRootDescriptor(root, target, isGenerated, isTemp, packagePrefix, excludes)