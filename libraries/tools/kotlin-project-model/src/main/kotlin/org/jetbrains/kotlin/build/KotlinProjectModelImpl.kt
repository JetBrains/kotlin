/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.tooling.core.closure
import org.jetbrains.kotlin.tooling.core.withClosure

class KotlinProjectModel(
    val id: Id
) {
    data class Id(val name: String)

    val name: String get() = id.name

    val sourceSets: Map<KotlinSourceSetModel.Id, KotlinSourceSetModel> get() = _sourceSets
    private val _sourceSets = mutableMapOf<KotlinSourceSetModel.Id, KotlinSourceSetModel>()

    fun addSourceSet(name: String): KotlinSourceSetModel {
        val id = KotlinSourceSetModel.Id(name)
        require(id !in _sourceSets) { "Source set with id $id already exists" }
        return KotlinSourceSetModel(id, this).also { _sourceSets[id] = it }
    }

    val targets: Map<KotlinTargetModel.Id, KotlinTargetModel> get() = _targets
    private val _targets = mutableMapOf<KotlinTargetModel.Id, KotlinTargetModel>()

    fun addTarget(name: String, platformType: KotlinPlatformModel): KotlinTargetModel {
        val id = KotlinTargetModel.Id(name)
        require(id !in _targets) { "Target with id $id already exists" }
        return KotlinTargetModel(id, this, platformType).also { _targets[id] = it }
    }

    val allPlatformCompilations: List<KotlinCompilationModel> get() = targets.flatMap { it.value.compilations.values }
}

class KotlinSourceSetModel internal constructor(
    val id: Id,
    val project: KotlinProjectModel,
) {
    data class Id(val name: String)

    val name: String get() = id.name

    val dependsOn: Set<KotlinSourceSetModel> get() = _dependsOn
    private val _dependsOn = mutableSetOf<KotlinSourceSetModel>()

    fun addDependsOnSourceSet(sourceSet: KotlinSourceSetModel) {
        _dependsOn += sourceSet
    }

    val sharedBetweenPlatformCompilations: Set<KotlinCompilationModel> by lazy { calculateCompilationsContainingThisSourceSet() }
    val isShared: Boolean get() = sharedBetweenPlatformCompilations.size > 1
    val isNativeShared: Boolean get() = sharedBetweenPlatformCompilations.all { it.target.isNative }
    val isAndroidAndJvmShared: Boolean by lazy { calculateIsAndroidAndJvm() }
    val hasMetadataCompilation: Boolean get() = isShared && !isAndroidAndJvmShared
    val isDefaultCompilationSourceSet: Boolean get() = sharedBetweenPlatformCompilations.singleOrNull()?.defaultSourceSet == this

    val platformCompilationForDependencies: KotlinCompilationModel
        get() {
            if (sharedBetweenPlatformCompilations.size == 1) return sharedBetweenPlatformCompilations.single()
            if (isAndroidAndJvmShared) return sharedBetweenPlatformCompilations.single { it.target.isJvm }
            throw IllegalStateException("Source set '$name' is shared between multiple platform compilations.")
        }

    val dependsOnClosure: Set<KotlinSourceSetModel> by lazy { closure { it.dependsOn } }

    val friendSourceSets: Set<KotlinSourceSetModel> by lazy { calculateFriendSourceSets() }

    private fun calculateCompilationsContainingThisSourceSet(): Set<KotlinCompilationModel> = project.targets.values
        .flatMap { it.compilations.values.filter { compilation -> this in compilation.allSourceSets } }
        .toSet()

    private fun calculateFriendSourceSets(): Set<KotlinSourceSetModel> {
        val associatedCompilations = sharedBetweenPlatformCompilations.flatMap { it.associatedCompilations }
        if (associatedCompilations.isEmpty()) return emptySet()

        val friendSourceSets = associatedCompilations
            .map { it.allSourceSets }
            .reduce { acc, sourceSets -> acc intersect sourceSets }
        return friendSourceSets.toSet()
    }

    private fun calculateIsAndroidAndJvm(): Boolean {
        var hasJvm = false
        var hasAndroid = false
        for (compilation in sharedBetweenPlatformCompilations) {
            val target = compilation.target
            if (target.isJvm) { hasJvm = true; continue }
            if (target.isAndroid) { hasAndroid = true; continue }
            return false
        }

        return hasJvm && hasAndroid
    }
}

class KotlinTargetModel internal constructor(
    val id: Id,
    val project: KotlinProjectModel,
    val platformType: KotlinPlatformModel,
) {
    data class Id(val name: String)

    val name: String get() = id.name

    val compilations: Map<KotlinCompilationModel.Id, KotlinCompilationModel> get() = _compilations
    private val _compilations = mutableMapOf<KotlinCompilationModel.Id, KotlinCompilationModel>()

    fun addCompilation(name: String, defaultSourceSet: KotlinSourceSetModel): KotlinCompilationModel {
        val id = KotlinCompilationModel.Id(name)
        require(id !in _compilations) { "Compilation with name $name already exists" }
        return KotlinCompilationModel(id, this, defaultSourceSet).also { _compilations[id] = it }
    }

    val isNative: Boolean get() = platformType is KotlinNativePlatformModel

    val isAndroid: Boolean get() = platformType.isAndroid
    val isJvm: Boolean get() = platformType.isJvm

}

class KotlinCompilationModel internal constructor(
    val id: Id,
    val target: KotlinTargetModel,
    val defaultSourceSet: KotlinSourceSetModel,
) {
    data class Id(val name: String)

    val name: String get() = id.name

    val associatedCompilations: Set<KotlinCompilationModel> get() = _associatedCompilations
    private val _associatedCompilations = mutableSetOf<KotlinCompilationModel>()

    fun associateWith(compilation: KotlinCompilationModel) {
        _associatedCompilations += compilation
    }

    val allSourceSets: Set<KotlinSourceSetModel> get() = defaultSourceSet.withClosure { it.dependsOn }
}

interface KotlinPlatformModel {
    val isJvm: Boolean
    val isAndroid: Boolean
}
interface KotlinNativePlatformModel : KotlinPlatformModel