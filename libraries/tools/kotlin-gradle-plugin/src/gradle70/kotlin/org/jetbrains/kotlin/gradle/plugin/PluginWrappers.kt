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
import org.jetbrains.kotlin.gradle.plugin.internal.BasePluginConfigurationG70
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessorG70
import org.jetbrains.kotlin.gradle.targets.js.nodejs.UnameExecutor
import javax.inject.Inject

private const val PLUGIN_VARIANT_NAME = "gradle70"

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

@Suppress("unused")
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
    factories[JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory::class] =
        JavaSourceSetsAccessorG70.JavaSourceSetAccessorVariantFactoryG70()
    factories[BasePluginConfiguration.BasePluginConfigurationVariantFactory::class] =
        BasePluginConfigurationG70.BasePluginConfigurationVariantFactoryG70()
    factories[IdeaSyncDetector.IdeaSyncDetectorVariantFactory::class] =
        IdeaSyncDetectorG70.IdeaSyncDetectorVariantFactoryG70()
    factories[ConfigurationTimePropertiesAccessor.ConfigurationTimePropertiesAccessorVariantFactory::class] =
        ConfigurationTimePropertiesAccessorG70.ConfigurationTimePropertiesAccessorVariantFactoryG70()
    factories[MppTestReportHelper.MppTestReportHelperVariantFactory::class] =
        MppTestReportHelperG70.MppTestReportHelperVariantFactoryG70()
    factories[KotlinTestReportCompatibilityHelper.KotlinTestReportCompatibilityHelperVariantFactory::class] =
        KotlinTestReportCompatibilityHelperG70.KotlinTestReportCompatibilityHelperVariantFactoryG70()
    factories[ArtifactTypeAttributeAccessor.ArtifactTypeAttributeAccessorVariantFactory::class] =
        ArtifactTypeAttributeAccessorG70.ArtifactTypeAttributeAccessorVariantFactoryG70()
    factories[ProjectIsolationStartParameterAccessor.Factory::class] =
        ProjectIsolationStartParameterAccessorG70.Factory()
    factories[CompatibilityConventionRegistrar.Factory::class] =
        CompatibilityConventionRegistrarG70.Factory()
    factories[UnameExecutor.UnameExecutorVariantFactory::class] =
        UnameExecutorG70.UnameExecutorVariantFactoryG70()
    factories[ConfigurationCacheStartParameterAccessor.Factory::class] = ConfigurationCacheStartParameterAccessorG70.Factory()
}
