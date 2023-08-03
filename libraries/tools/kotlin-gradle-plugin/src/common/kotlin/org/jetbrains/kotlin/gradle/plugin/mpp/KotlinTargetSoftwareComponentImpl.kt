/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.tooling.core.UnsafeApi

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
            val configuration = project.configurations.findByName(publishedConfigurationName)
                ?: project.configurations.create(publishedConfigurationName).also { publishedConfiguration ->
                    publishedConfiguration.isCanBeConsumed = false
                    publishedConfiguration.isCanBeResolved = false
                    publishedConfiguration.extendsFrom(project.configurations.getByName(kotlinUsageContext.dependencyConfigurationName))
                    publishedConfiguration.artifacts.addAll(kotlinUsageContext.artifacts)
                    copyAttributes(from = kotlinUsageContext.attributes, to = publishedConfiguration.attributes)
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


/* Smaller Utils functions */

private val publishedConfigurationNameSuffix = "-published"

internal fun originalVariantNameFromPublished(publishedConfigurationName: String): String? =
    publishedConfigurationName.takeIf { it.endsWith(publishedConfigurationNameSuffix) }?.removeSuffix(publishedConfigurationNameSuffix)

internal fun publishedConfigurationName(originalVariantName: String) = originalVariantName + publishedConfigurationNameSuffix

/* KotlinTargetSoftwareComponent Implementation */

internal class KotlinTargetSoftwareComponentImpl @UnsafeApi constructor(
    private val adhocComponent: AdhocComponentWithVariants,
    private val kotlinComponent: KotlinTargetComponent,
) : KotlinTargetSoftwareComponent() {

    override fun getCoordinates() =
        (kotlinComponent as? ComponentWithCoordinates)?.coordinates ?: error("kotlinComponent is not ComponentWithCoordinates")

    override fun getVariants(): Set<SoftwareComponent> =
        (kotlinComponent as? KotlinVariantWithMetadataVariant)?.variants.orEmpty()

    override fun getName(): String = adhocComponent.name
    override fun getUsages(): MutableSet<out UsageContext> = (adhocComponent as SoftwareComponentInternal).usages
}
