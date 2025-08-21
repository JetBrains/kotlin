/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator

internal class KotlinMetadataTargetPreset(
    project: Project
) : KotlinOnlyTargetPreset<KotlinMetadataTarget, KotlinCompilation<Any>>(project) {
    override val name: String = PRESET_NAME

    override fun createCompilationFactory(
        forTarget: KotlinMetadataTarget
    ): KotlinCompilationFactory<KotlinCompilation<Any>> =
        object : KotlinCompilationFactory<KotlinCompilation<Any>> {
            override val target: KotlinTarget = forTarget

            @Suppress("UNCHECKED_CAST")
            override val itemClass: Class<KotlinCompilation<Any>>
                get() = KotlinCompilation::class.java as Class<KotlinCompilation<Any>>

            override fun create(name: String): InternalKotlinCompilation<Any> = when (name) {
                KotlinCompilation.MAIN_COMPILATION_NAME -> KotlinCommonCompilationFactory(
                    forTarget, getOrCreateDefaultSourceSet(name)
                ).create(name)

                else -> error("Can't create custom metadata compilations by name")
            }
        }

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    companion object {
        const val PRESET_NAME = "metadata"
    }

    override fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinMetadataTarget> =
        KotlinMetadataTargetConfigurator()

    override fun instantiateTarget(name: String): KotlinMetadataTarget {
        return project.objects.newInstance(KotlinMetadataTarget::class.java, project)
    }

    override fun createTargetInternal(name: String): KotlinMetadataTarget =
        super.createTargetInternal(name).apply {
            val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val commonMainSourceSet = project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)

            @Suppress("DEPRECATION_ERROR")
            mainCompilation.addSourceSet(commonMainSourceSet)
        }
}
