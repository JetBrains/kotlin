/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.tasks.artifact

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.setAttribute
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

abstract class KotlinArtifactConfigImpl(
    override val artifactName: String
) : KotlinArtifactConfig {
    override val modules = mutableSetOf<Any>()
    override fun setModules(vararg project: Any) {
        modules.clear()
        modules.addAll(project)
    }

    override fun addModule(project: Any) {
        modules.add(project)
    }

    protected open fun validate() {
        check(modules.isNotEmpty()) {
            "Native artifact '$artifactName' wasn't configured because it requires at least one module for linking"
        }
    }
}

abstract class KotlinNativeArtifactConfigImpl(artifactName: String) : KotlinArtifactConfigImpl(artifactName), KotlinNativeArtifactConfig {
    override var modes: Set<NativeBuildType> = NativeBuildType.DEFAULT_BUILD_TYPES
    override fun modes(vararg modes: NativeBuildType) {
        this.modes = modes.toSet()
    }

    override var isStatic: Boolean = false
    override var linkerOptions: List<String> = emptyList()

    internal var toolOptionsConfigure: KotlinCommonCompilerToolOptions.() -> Unit = {}
    override fun toolOptions(configure: Action<KotlinCommonCompilerToolOptions>) {
        toolOptionsConfigure = configure::execute
    }

    @Suppress("DEPRECATION")
    internal var kotlinOptionsFn: KotlinCommonToolOptions.() -> Unit = {}

    @Deprecated("Please migrate to toolOptions DSL. More details are here: https://kotl.in/u1r8ln")
    override fun kotlinOptions(
        @Suppress("DEPRECATION") fn: Action<KotlinCommonToolOptions>
    ) {
        kotlinOptionsFn = fn::execute
    }

    internal val binaryOptions: MutableMap<String, String> = mutableMapOf()
    override fun binaryOption(name: String, value: String) {
        binaryOptions[name] = value
    }

    override fun validate() {
        super.validate()
        check(modes.isNotEmpty()) {
            "Native artifact '$artifactName' wasn't configured because it requires at least one build type in modes"
        }
    }
}

internal fun Project.registerLibsDependencies(target: KonanTarget, artifactName: String, deps: Set<Any>): String {
    val librariesConfigurationName = lowerCamelCaseName(target.presetName, artifactName, "linkLibrary")
    configurations.maybeCreateResolvable(librariesConfigurationName).apply {
        isVisible = false
        isTransitive = true
        configureAttributesFor(project, target)
    }
    deps.forEach { dependencies.add(librariesConfigurationName, it) }
    return librariesConfigurationName
}

internal fun Project.registerExportDependencies(target: KonanTarget, artifactName: String, deps: Set<Any>): String {
    val exportConfigurationName = lowerCamelCaseName(target.presetName, artifactName, "linkExport")
    configurations.maybeCreateResolvable(exportConfigurationName).apply {
        isVisible = false
        isTransitive = false
        configureAttributesFor(project, target)
    }
    deps.forEach { dependencies.add(exportConfigurationName, it) }
    return exportConfigurationName
}

private fun Configuration.configureAttributesFor(project: Project, target: KonanTarget) = with (project) {
    attributes.setAttribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    attributes.setAttribute(KotlinNativeTarget.konanTargetAttribute, target.name)
    attributes.setAttribute(Usage.USAGE_ATTRIBUTE, project.objects.named(KotlinUsages.KOTLIN_API))
    if (kotlinPropertiesProvider.enableUnpackedKlibs) {
        attributes.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, unpackedKlibLibraryElements())
    } else {
        attributes.setAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, packedKlibLibraryElements())
    }
}