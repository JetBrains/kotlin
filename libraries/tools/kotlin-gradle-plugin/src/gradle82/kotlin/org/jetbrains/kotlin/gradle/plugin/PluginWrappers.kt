/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.internal.ConfigurationCacheStartParameterAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.ConfigurationCacheStartParameterAccessorG82
import org.jetbrains.kotlin.gradle.plugin.internal.MavenPublicationComponentAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.MavenPublicationComponentAccessorG82
import org.jetbrains.kotlin.gradle.plugin.internal.ProjectIsolationStartParameterAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.ProjectIsolationStartParameterAccessorG82
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.gradleVersion
import javax.inject.Inject

private const val PLUGIN_VARIANT_NAME = "gradle82"

open class KotlinPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPluginWrapper(registry) {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinCommonPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinCommonPluginWrapper(registry) {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinAndroidPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinAndroidPluginWrapper(registry) {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

@Suppress("DEPRECATION_ERROR")
open class Kotlin2JsPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlin2JsPluginWrapper(registry) {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinMultiplatformPluginWrapper : AbstractKotlinMultiplatformPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinJsPluginWrapper : AbstractKotlinJsPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
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

open class KotlinPlatformCommonPlugin : KotlinPlatformPluginBase("common") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinCommonPluginWrapper>()
        warnAboutKotlin12xMppDeprecation(project)
    }
}

open class KotlinApiPlugin : KotlinBaseApiPlugin() {
    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

private fun Project.registerVariantImplementations() {
    val factories = VariantImplementationFactoriesConfigurator.get(gradle)
    factories[ConfigurationCacheStartParameterAccessor.Factory::class] =
        ConfigurationCacheStartParameterAccessorG82.Factory()
    factories[ProjectIsolationStartParameterAccessor.Factory::class] =
        ProjectIsolationStartParameterAccessorG82.Factory()
    if (gradleVersion < GradleVersion.version("8.3")) { // for versions higher than 8.3 use common implementation
        factories[MavenPublicationComponentAccessor.Factory::class] =
            MavenPublicationComponentAccessorG82.Factory()
    }
}
