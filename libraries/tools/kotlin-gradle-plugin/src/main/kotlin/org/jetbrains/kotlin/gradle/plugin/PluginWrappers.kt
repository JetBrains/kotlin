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
import org.gradle.api.model.ObjectFactory
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.internal.*
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessorG6
import org.jetbrains.kotlin.gradle.targets.js.nodejs.UnameExecutor
import javax.inject.Inject

private const val PLUGIN_VARIANT_NAME = "main"

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
        val kotlinSourceSet = androidSourceSet.getExtension<SourceDirectorySet>(KOTLIN_DSL_NAME)
            ?: return
        kotlinSourceSet.source(getKotlinSourceDirectorySetSafe(commonSourceSet)!!)
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
    factories[MavenPluginConfigurator.MavenPluginConfiguratorVariantFactory::class] =
        MavenPluginConfiguratorG6.Gradle6MavenPluginConfiguratorVariantFactory()
    factories[JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory::class] =
        JavaSourceSetsAccessorG6.JavaSourceSetAccessorVariantFactoryG6()
    factories[BasePluginConfiguration.BasePluginConfigurationVariantFactory::class] =
        BasePluginConfigurationG6.BasePluginConfigurationVariantFactoryG6()
    factories[IdeaSyncDetector.IdeaSyncDetectorVariantFactory::class] =
        IdeaSyncDetectorG6.IdeaSyncDetectorVariantFactoryG6()
    factories[ConfigurationTimePropertiesAccessor.ConfigurationTimePropertiesAccessorVariantFactory::class] =
        ConfigurationTimePropertiesAccessorG6.ConfigurationTimePropertiesAccessorVariantFactoryG6()
    factories[MppTestReportHelper.MppTestReportHelperVariantFactory::class] =
        MppTestReportHelperG6.MppTestReportHelperVariantFactoryG6()
    factories[KotlinTestReportCompatibilityHelper.KotlinTestReportCompatibilityHelperVariantFactory::class] =
        KotlinTestReportCompatibilityHelperG6.KotlinTestReportCompatibilityHelperVariantFactoryG6()
    factories[ArtifactTypeAttributeAccessor.ArtifactTypeAttributeAccessorVariantFactory::class] =
        ArtifactTypeAttributeAccessorG6.ArtifactTypeAttributeAccessorVariantFactoryG6()
    factories[ProjectIsolationStartParameterAccessor.Factory::class] =
        ProjectIsolationStartParameterAccessorG6.Factory()
    factories[CompatibilityConventionRegistrar.Factory::class] =
        CompatibilityConventionRegistrarG6.Factory()
    factories[UnameExecutor.UnameExecutorVariantFactory::class] =
        UnameExecutorG6.UnameExecutorVariantFactoryG6()
    factories[ConfigurationCacheStartParameterAccessor.Factory::class] = ConfigurationCacheStartParameterAccessorG6.Factory()
}