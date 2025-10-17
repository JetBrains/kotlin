/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_UKLIB_FALLBACK_VARIANT
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.utils.copyAttributesTo
import org.jetbrains.kotlin.gradle.utils.getAttributeSafely
import org.jetbrains.kotlin.gradle.utils.isAllGradleProjectsEvaluated
import org.jetbrains.kotlin.gradle.utils.maybeCreateDependencyScope
import org.jetbrains.kotlin.gradle.utils.projectPathCompat
import org.jetbrains.kotlin.tooling.core.UnsafeApi
import kotlin.collections.forEach

internal fun KotlinTargetSoftwareComponent(
    target: AbstractKotlinTarget,
    kotlinComponent: KotlinTargetComponent,
): KotlinTargetSoftwareComponent {
    val softwareComponentFactory = (target.project as ProjectInternal).services.get(SoftwareComponentFactory::class.java)
    val adhocVariant = softwareComponentFactory.adhoc(kotlinComponent.name)

    /* Launch configuration */
    target.project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
        kotlinComponent.internal.usages.forEach { kotlinUsageContext ->
            /* Explicitly typing 'Project' to avoid smart cast from 'target.project as ProjectInternal' */
            val project: Project = target.project
            val publishedConfigurationName = publishedConfigurationName(kotlinUsageContext.name)
            val configuration = project.configurations.maybeCreateDependencyScope(publishedConfigurationName) {
                isVisible = false
                if (project.kotlinPropertiesProvider.kmpResolutionStrategy == KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs) {
                    filterOutNonResolvableDependenciesForStandardKmpResolution(project, kotlinUsageContext)
                } else {
                    extendsFrom(project.configurations.getByName(kotlinUsageContext.dependencyConfigurationName))
                }
                artifacts.addAll(kotlinUsageContext.artifacts)
                // KT-64789: workaround for missing 'org.gradle.libraryelements' attribute on the kotlinUsageContext returned keys Set
                // So far I don't know the reason why it appears only on the second call to `keySet()`
                // Test failing without it - KotlinAndroidMppIT#testMppAndroidLibFlavorsPublication
                kotlinUsageContext.attributes.keySet()
                kotlinUsageContext.copyAttributesTo(
                    project.providers,
                    dest = this
                )
            }

            adhocVariant.addVariantsFromConfiguration(configuration) { configurationVariantDetails ->
                val mavenScope = kotlinUsageContext.mavenScope
                if (mavenScope != null) {
                    val mavenScopeString = when (mavenScope) {
                        KotlinUsageContext.MavenScope.COMPILE -> "compile"
                        KotlinUsageContext.MavenScope.RUNTIME -> "runtime"
                    }
                    configurationVariantDetails.mapToMavenScope(mavenScopeString)
                }
            }
        }
    }

    @OptIn(UnsafeApi::class)
    return KotlinTargetSoftwareComponentImpl(adhocVariant, kotlinComponent)
}

/**
 * With UKlib lenient resolution it is possible that this publisher were able to resolve some dependencies
 * that will not be resolvable with regular KMP resolution. So for regular KMP variants these dependencies
 * should be filtered out. This way build will not fail, but as a consequence, some transitive dependencies will be missing.
 */
private fun Configuration.filterOutNonResolvableDependenciesForStandardKmpResolution(
    project: Project,
    kotlinUsageContext: KotlinUsageContext,
) {
    val resolvableConfiguration = project.configurations.getByName(kotlinUsageContext.compilation.compileDependencyConfigurationName)
    val consumableConfiguration = project.configurations.getByName(kotlinUsageContext.dependencyConfigurationName)
    val isJvmOrAndroidPublication = kotlinUsageContext.compilation.platformType !in setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)

    // TODO: Included builds
    dependencies.addAllLater(project.provider {
        val directDependencies = consumableConfiguration.allDependencies.groupBy {
            when (it) {
                is ProjectDependency -> "project_" + it.projectPathCompat
                is ModuleDependency -> "module_${it.group}:${it.name}"
                else -> "unknown" // group all unknown deps, and don't filter them out
            }
        }.toMutableMap()

        if (!project.isAllGradleProjectsEvaluated) {
            project.logger.warn(
                """
                |Kotlin Gradle Plugin can't filter out non resolvable dependencies for standard KMP resolution.
                | Resolution of ${project.path}:${resolvableConfiguration.name} can't happen when not all projects are evaluated.
                """.trimMargin()
            )
            return@provider directDependencies.values.flatten()
        }

        resolvableConfiguration.incoming.resolutionResult.root.dependencies.forEach {
            if (it !is ResolvedDependencyResult) return@forEach
            val unresolvableInStandardKmp = it.selected.variants.any { variantResult ->
                val usageValue = variantResult.attributes.getAttributeSafely(Usage.USAGE_ATTRIBUTE) ?: return@any false
                // requesting platform dependencies but got either metadata or uklib fallback -> unresolvable in standard KMP.
                val isMetadataFallback = usageValue == KotlinUsages.KOTLIN_METADATA || usageValue == KOTLIN_UKLIB_FALLBACK_VARIANT
                if (isMetadataFallback) return@any true

                // requesting platform dependencies in non jvm compilations, but got kotlin/jvm variant -> unresolvable in standard KMP.
                val kotlinPlatformTypeOfResolvedVariant = variantResult.attributes.getAttributeSafely(KotlinPlatformType.attribute)
                if (isJvmOrAndroidPublication) {
                    return@any kotlinPlatformTypeOfResolvedVariant == KotlinPlatformType.jvm.name
                }

                false
            }

            if (unresolvableInStandardKmp) {
                val requestedId = it.requested
                when (requestedId) {
                    is ModuleComponentSelector -> directDependencies -= "module_${requestedId.group}:${requestedId.module}"
                    is ProjectComponentSelector -> directDependencies -= "project_${requestedId.projectPath}"
                    else -> Unit
                }
            }
        }
        directDependencies.values.flatten()
    })
}

/* Smaller Utils functions */

private val publishedConfigurationNameSuffix = "-published"

internal fun originalVariantNameFromPublished(publishedConfigurationName: String): String? =
    publishedConfigurationName.takeIf { it.endsWith(publishedConfigurationNameSuffix) }?.removeSuffix(publishedConfigurationNameSuffix)

internal fun publishedConfigurationName(originalVariantName: String) = originalVariantName + publishedConfigurationNameSuffix

/* KotlinTargetSoftwareComponent Implementation */

internal class KotlinTargetSoftwareComponentImpl @UnsafeApi constructor(
    private val adhocComponent: AdhocComponentWithVariants,
    internal val kotlinComponent: KotlinTargetComponent,
) : KotlinTargetSoftwareComponent() {

    override fun getCoordinates() =
        (kotlinComponent as? ComponentWithCoordinates)?.coordinates ?: error("kotlinComponent is not ComponentWithCoordinates")

    override fun getVariants(): Set<SoftwareComponent> =
        (kotlinComponent as? KotlinVariantWithMetadataVariant)?.variants.orEmpty()

    override fun getName(): String = adhocComponent.name
    override fun getUsages(): MutableSet<out UsageContext> = (adhocComponent as SoftwareComponentInternal).usages
}
