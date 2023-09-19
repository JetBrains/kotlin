/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency.Type.Regular
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImport.SourceSetConstraint.Companion.isJvmAndAndroid
import org.jetbrains.kotlin.gradle.plugin.ide.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.projectDependency
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.copyAttributes
import org.jetbrains.kotlin.gradle.utils.toMap

/**
 * ### Special Resolver for the JVM + Android use case:
 * This resolver takes care of resolving the 'source dependencies' (aka dependencies to other projects' SourceSets)
 * for SourceSets sharing code across a JVM and Android target.
 *
 * There are two special cases this resolver will archive its purpose:
 *
 * #### 1: 'This' multiplatform project depends on another multiplatform project:
 * In this case, the metadata dependency resolution will return a 'ChooseVisibleSourceSets'.
 * However, since we do not compile jvm+android into metadata, the resolution will not contain the desired source sets.
 * Instead, the algorithm here will resolve the dependency project, look into it and will pick the corresponding 'jvm + android'
 * SourceSets that it can find
 *
 * #### 2: 'This' multiplatform project depends on a **non** multiplatform dependency project:
 * In this case the algorithm will use another approach to determine desirable SourceSets to resolve in the IDE:
 * It will use the 'PlatformLike' dependency resolution mechanics to resolve the project dependencies in an 'as if this was jvm' mode.
 * In order to set up attributes, the algorithm will copy the jvm platform attributes from the corresponding jvm compilations
 * of the current SourceSet.
 */
internal object IdeJvmAndAndroidSourceDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        if (!isJvmAndAndroid(sourceSet)) return emptySet()
        if (sourceSet !is DefaultKotlinSourceSet) return emptySet()

        return sourceSet.resolveMetadata<MetadataDependencyResolution>()
            /**
             * See [IdeVisibleMultiplatformSourceDependencyResolver] on why this could happen
             */
            .filter { metadataDependencyResolution -> metadataDependencyResolution.projectDependency(sourceSet.project) != sourceSet.project }
            .flatMap { metadataDependencyResolution ->

                when (metadataDependencyResolution) {
                    /* Dependency project is multiplatform */
                    is MetadataDependencyResolution.ChooseVisibleSourceSets -> resolveMultiplatformSourceSets(
                        metadataDependencyResolution.projectDependency(sourceSet.project) ?: return@flatMap emptyList()
                    )

                    /* Dependency project is not multiplatform (jvm/android only) */
                    is MetadataDependencyResolution.KeepOriginalDependency -> resolveJvmSourceSets(sourceSet)
                    else -> emptyList()
                }
            }
            .toSet()
    }

    private fun resolveMultiplatformSourceSets(dependencyProject: Project): Iterable<IdeaKotlinDependency> {
        val kotlin = dependencyProject.multiplatformExtensionOrNull ?: return emptyList()
        return kotlin.sourceSets
            .filter { sourceSet -> isJvmAndAndroidMain(sourceSet) }
            .map { sourceSet -> IdeaKotlinSourceDependency(type = Regular, coordinates = IdeaKotlinSourceCoordinates(sourceSet)) }
    }

    /**
     * Pretend that this [sourceSet] is 'jvm' and resolve binaries.
     * #### Setting up attributes:
     * In order to set up the 'platform like' / 'jvm like' dependency resolution, this algorithm
     * will look at all 'jvm' based compilations, uses their 'compileDependencyConfiguration' as reference and
     * then uses the intersection of all available attributes
     *
     * #### componentFilter:
     * This resolver will just care about resolving project dependencies.
     * Therefore, a componentFilter is added to only resolve project dependencies.
     * We expect to resolve project artifact dependencies which can then be matched to the corresponding
     * SourceSets on IDE side.
     */
    private fun resolveJvmSourceSets(sourceSet: KotlinSourceSet): Iterable<IdeaKotlinDependency> {
        return IdeBinaryDependencyResolver(
            binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
            artifactResolutionStrategy = IdeBinaryDependencyResolver.ArtifactResolutionStrategy.PlatformLikeSourceSet(
                setupPlatformResolutionAttributes = {
                    sourceSet.internal.compilations.filter { it.platformType == KotlinPlatformType.jvm }
                        .map { compilation -> compilation.internal.configurations.compileDependencyConfiguration.attributes }
                        .map { attributes -> attributes.toMap().toList().toSet() }
                        .reduceOrNull { acc, next -> acc intersect next }
                        .orEmpty()
                        .forEach { (key, value) -> @Suppress("UNCHECKED_CAST") attribute(key as Attribute<Any>, value as Any) }
                },
                componentFilter = { id -> id is ProjectComponentIdentifier }
            )
        ).resolve(sourceSet)
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
