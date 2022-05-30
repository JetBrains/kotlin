/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import javax.inject.Inject

private const val PLUGIN_VARIANT_NAME = "gradle70"

open class KotlinPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPluginWrapper(registry) {
    override val pluginVariant: String = PLUGIN_VARIANT_NAME
}

open class KotlinCommonPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinCommonPluginWrapper(registry) {
    override val pluginVariant: String = PLUGIN_VARIANT_NAME
}

open class KotlinAndroidPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinAndroidPluginWrapper(registry) {
    override val pluginVariant: String = PLUGIN_VARIANT_NAME
}

@Suppress("DEPRECATION_ERROR")
open class Kotlin2JsPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlin2JsPluginWrapper(registry) {
    override val pluginVariant: String = PLUGIN_VARIANT_NAME
}

open class KotlinMultiplatformPluginWrapper : AbstractKotlinMultiplatformPluginWrapper() {
    override val pluginVariant: String = PLUGIN_VARIANT_NAME
}

@Suppress("unused")
open class KotlinJsPluginWrapper : AbstractKotlinJsPluginWrapper() {
    override val pluginVariant: String = PLUGIN_VARIANT_NAME
}

@Suppress("unused")
open class KotlinPm20PluginWrapper @Inject constructor(
    objectFactory: ObjectFactory
) : AbstractKotlinPm20PluginWrapper(objectFactory) {
    override val pluginVariant: String = PLUGIN_VARIANT_NAME
}

open class KotlinPlatformJvmPlugin : KotlinPlatformImplementationPluginBase("jvm") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinPluginWrapper>()
        super.apply(project)
    }
}

open class KotlinPlatformJsPlugin : KotlinPlatformImplementationPluginBase("js") {
    override fun apply(project: Project) {
        @Suppress("DEPRECATION_ERROR")
        project.applyPlugin<Kotlin2JsPluginWrapper>()
        super.apply(project)
    }
}

open class KotlinPlatformAndroidPlugin : KotlinPlatformImplementationPluginBase("android") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinAndroidPluginWrapper>()
        super.apply(project)
    }

    override fun namedSourceSetsContainer(project: Project): NamedDomainObjectContainer<*> =
        (project.extensions.getByName("android") as BaseExtension).sourceSets

    override fun addCommonSourceSetToPlatformSourceSet(commonSourceSet: Named, platformProject: Project) {
        val androidExtension = platformProject.extensions.getByName("android") as BaseExtension
        val androidSourceSet = androidExtension.sourceSets.findByName(commonSourceSet.name) ?: return
        val kotlinSourceSet = androidSourceSet.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet
            ?: return
        kotlinSourceSet.kotlin.source(getKotlinSourceDirectorySetSafe(commonSourceSet)!!)
    }
}

open class KotlinPlatformCommonPlugin : KotlinPlatformPluginBase("common") {
    override fun apply(project: Project) {
        warnAboutKotlin12xMppDeprecation(project)
        project.applyPlugin<KotlinCommonPluginWrapper>()
    }
}
