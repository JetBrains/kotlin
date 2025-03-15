/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
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

open class KotlinAndroidPluginWrapper @Inject constructor(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinAndroidPluginWrapper(registry) {

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
