/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.SourceSetConstraint.Companion.isJvmAndAndroid
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.projectDependency
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object IdeJvmAndAndroidSourceDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (!isJvmAndAndroid(sourceSet)) return emptySet()
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        return sourceSet.resolveMetadata<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            /**
             * See [IdeVisibleMultiplatformSourceDependencyResolver] on why this could happen
             */
            .filter { chooseVisibleSourceSets -> chooseVisibleSourceSets.projectDependency(sourceSet.project) != sourceSet.project }
            .flatMap { chooseVisibleSourceSets ->
                val projectDependency = chooseVisibleSourceSets.projectDependency(sourceSet.project)
                    ?: return@flatMap emptyList<IdeaKotlinDependency>()
                val kotlin = projectDependency.multiplatformExtensionOrNull ?: return@flatMap emptyList<IdeaKotlinDependency>()
                kotlin.sourceSets
                    .filter { sourceSet -> isJvmAndAndroidMain(sourceSet) }
                    .map { sourceSet -> IdeaKotlinSourceDependency(type = Regular, coordinates = IdeaKotlinSourceCoordinates(sourceSet)) }
            }
            .toSet()
    }

    private fun isJvmAndAndroidMain(sourceSet: KotlinSourceSet): Boolean {
        if (!isJvmAndAndroid(sourceSet)) return false
        return sourceSet.internal.compilations.filter { it.platformType != KotlinPlatformType.common }.all { compilation ->
            isJvmMain(compilation) || isAndroidMain(compilation)
        }
    }

    private fun isJvmMain(compilation: KotlinCompilation<*>): Boolean {
        return compilation.platformType == KotlinPlatformType.jvm && compilation.isMain()
    }

    private fun isAndroidMain(compilation: KotlinCompilation<*>): Boolean {
        return compilation is KotlinJvmAndroidCompilation && compilation.androidVariant.type == AndroidVariantType.Main
    }
}
