/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal

object IdeJvmAndAndroidSourceDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (!sourceSet.isJvmAndAndroid) return emptySet()
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()
        return sourceSet.resolveMetadata<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .flatMap { chooseVisibleSourceSets ->
                val projectDependency = chooseVisibleSourceSets.projectDependency ?: return@flatMap emptyList<IdeaKotlinDependency>()
                val kotlin = projectDependency.multiplatformExtensionOrNull ?: return@flatMap emptyList<IdeaKotlinDependency>()
                kotlin.sourceSets
                    .filter { sourceSet -> sourceSet.isJvmAndAndroid }
                    .map { sourceSet -> IdeaKotlinSourceDependency(type = Regular, coordinates = IdeaKotlinSourceCoordinates(sourceSet)) }
            }
            .toSet()
    }

    private val KotlinSourceSet.isJvmAndAndroid: Boolean
        get() = internal.compilations.map { it.platformType }.toSet() == setOf(KotlinPlatformType.androidJvm, KotlinPlatformType.jvm)
}
