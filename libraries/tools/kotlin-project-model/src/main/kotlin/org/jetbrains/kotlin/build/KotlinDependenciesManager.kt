/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

interface KotlinLibrariesModel
interface KotlinPlatformDependenciesModel : KotlinLibrariesModel
interface KotlinPlatformCompilationOutputModel : KotlinLibrariesModel

interface KotlinMetadataLibrariesModel : KotlinLibrariesModel
interface KotlinCommonizedNativeDistributionModel : KotlinMetadataLibrariesModel
interface TransformedMetadataLibrariesModel : KotlinMetadataLibrariesModel
interface CommonizedCinteropMetadataLibrariesModel : KotlinMetadataLibrariesModel
interface KotlinMetadataCompilationOutputModel : KotlinMetadataLibrariesModel

interface KotlinSourcesContainerModel

interface KotlinDependencyScopeModel
interface KotlinCompileDependencyScopeModel : KotlinDependencyScopeModel

interface KotlinProjectModelBuildToolBridge {
    fun resolvePlatformDependencies(compilation: KotlinCompilationModel, scope: KotlinDependencyScopeModel): KotlinPlatformDependenciesModel
    fun commonizedNativeDistribution(nativeCompilations: Set<KotlinCompilationModel>): KotlinCommonizedNativeDistributionModel
    fun transformedMetadataDependencies(sourceSet: KotlinSourceSetModel, transitive: Boolean): TransformedMetadataLibrariesModel
    fun commonizedCinteropDependencies(sourceSet: KotlinSourceSetModel): CommonizedCinteropMetadataLibrariesModel

    fun metadataCompilationOutput(sourceSet: KotlinSourceSetModel): KotlinMetadataCompilationOutputModel
    fun platformCompilationOutput(compilation: KotlinCompilationModel): KotlinPlatformCompilationOutputModel

    fun sourcesContainer(sourceSet: KotlinSourceSetModel): KotlinSourcesContainerModel
}

class KotlinDependenciesManager(
    private val buildToolBridge: KotlinProjectModelBuildToolBridge
) {
    fun sourceSetDependencies(
        sourceSetModel: KotlinSourceSetModel,
        scope: KotlinDependencyScopeModel,
        withDependsOnDependencies: Boolean,
    ): List<KotlinLibrariesModel> {
        if (sourceSetModel.hasMetadataCompilation) {
            return metadataDependencies(sourceSetModel, withDependsOnDependencies)
        }
        val platformCompilation = sourceSetModel.platformCompilationForDependencies
        return listOf(buildToolBridge.resolvePlatformDependencies(platformCompilation, scope))
    }

    fun metadataDependencies(
        sourceSetModel: KotlinSourceSetModel,
        withDependsOnDependencies: Boolean,
    ): List<KotlinMetadataLibrariesModel> {
        require(sourceSetModel.hasMetadataCompilation) {
            "Source set ${sourceSetModel.name} does not have metadata compilation"
        }

        val platformCompilations = sourceSetModel.sharedBetweenPlatformCompilations
        return buildList {
            if (sourceSetModel.isNativeShared) {
                val commonizedNativeDistributionDependencies = buildToolBridge.commonizedNativeDistribution(platformCompilations)
                add(commonizedNativeDistributionDependencies)

                val commonizedCinteropDependencies = buildToolBridge.commonizedCinteropDependencies(sourceSetModel)
                add(commonizedCinteropDependencies)
            }

            if (withDependsOnDependencies) {
                val dependsOnDependencies = sourceSetModel.dependsOnClosure.map(buildToolBridge::metadataCompilationOutput)
                addAll(dependsOnDependencies)
            }

            val transformedMetadataDependencies = buildToolBridge.transformedMetadataDependencies(
                sourceSetModel,
                transitive = false
            )
            add(transformedMetadataDependencies)
        }
    }

    fun friendDependencies(
        sourceSetModel: KotlinSourceSetModel,
    ): List<KotlinLibrariesModel> {
        val friends = sourceSetModel.friendSourceSets
        val friendDependencies = friends.map { friendSourceSet ->
            if (friendSourceSet.hasMetadataCompilation) {
                buildToolBridge.metadataCompilationOutput(friendSourceSet)
            } else {
                val platformCompilation = friendSourceSet.platformCompilationForDependencies
                buildToolBridge.platformCompilationOutput(platformCompilation)
            }
        }

        return friendDependencies
    }
}