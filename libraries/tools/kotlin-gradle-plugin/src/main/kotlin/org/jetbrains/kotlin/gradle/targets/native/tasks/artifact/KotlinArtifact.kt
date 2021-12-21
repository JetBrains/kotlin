/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

abstract class KotlinArtifact {
    internal val modules = mutableSetOf<Any>()
    fun setModules(vararg project: Any) {
        modules.clear()
        modules.addAll(project)
    }

    fun addModule(project: Any) {
        modules.add(project)
    }

    internal abstract fun validate(project: Project, name: String): Boolean
    internal abstract fun registerAssembleTask(project: Project, name: String)
}

abstract class KotlinNativeArtifact : KotlinArtifact() {
    var modes: Set<NativeBuildType> = NativeBuildType.DEFAULT_BUILD_TYPES
    fun modes(vararg modes: NativeBuildType) {
        this.modes = modes.toSet()
    }

    @JvmField var isStatic: Boolean = false
    @JvmField var linkerOptions: List<String> = emptyList()

    internal var kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit = {}
    fun kotlinOptions(fn: KotlinCommonToolOptions.() -> Unit) {
        kotlinOptionsFn = fn
    }
    //Groovy script DSL
    fun kotlinOptions(fn: Closure<*>) {
        kotlinOptionsFn = {
            fn.delegate = this
            fn.call()
        }
    }

    internal val binaryOptions: MutableMap<String, String> = mutableMapOf()
    fun binaryOption(name: String, value: String) {
        binaryOptions[name] = value
    }

    override fun validate(project: Project, name: String): Boolean {
        val logger = project.logger
        if (modules.isEmpty()) {
            logger.error("Native library '${name}' wasn't configured because it requires at least one module for linking")
            return false
        }

        if (modes.isEmpty()) {
            logger.error("Native library '${name}' wasn't configured because it requires at least one build type in modes")
            return false
        }

        return true
    }

    fun Project.registerLibsDependencies(target: KonanTarget, artifactName: String, deps: Set<Any>): String {
        val librariesConfigurationName = lowerCamelCaseName(target.presetName, artifactName, "linkLibrary")
        configurations.maybeCreate(librariesConfigurationName).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = true
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.name)
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
        }
        deps.forEach { dependencies.add(librariesConfigurationName, it) }
        return librariesConfigurationName
    }

    fun Project.registerExportDependencies(target: KonanTarget, artifactName: String, deps: Set<Any>): String {
        val exportConfigurationName = lowerCamelCaseName(target.presetName, artifactName, "linkExport")
        configurations.maybeCreate(exportConfigurationName).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
            attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.name)
            attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
        }
        deps.forEach { dependencies.add(exportConfigurationName, it) }
        return exportConfigurationName
    }
}