/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinTargetSoftwareComponent
import org.jetbrains.kotlin.tooling.core.UnsafeApi
import org.jetbrains.kotlin.gradle.plugin.mpp.isSourcesPublishableFuture



internal fun ExternalKotlinTargetSoftwareComponent(
    target: ExternalKotlinTargetImpl,
): ExternalKotlinTargetSoftwareComponent {
    val softwareComponentFactory = (target.project as ProjectInternal).services.get(SoftwareComponentFactory::class.java)
    val adhocSoftwareComponent = softwareComponentFactory.adhoc(target.targetName)

    adhocSoftwareComponent.addVariantsFromConfiguration(target.apiElementsPublishedConfiguration) { details ->
        details.mapToMavenScope("compile")
    }

    adhocSoftwareComponent.addVariantsFromConfiguration(target.runtimeElementsPublishedConfiguration) { details ->
        details.mapToMavenScope("runtime")
    }

    target.project.launch {
        if (target.isSourcesPublishableFuture.await()) {
            adhocSoftwareComponent.addVariantsFromConfiguration(target.sourcesElementsPublishedConfiguration) { details ->
                details.mapToOptional()
            }
        }
    }

    @OptIn(UnsafeApi::class)
    return ExternalKotlinTargetSoftwareComponent(
        target.project.multiplatformExtension,
        adhocSoftwareComponent as SoftwareComponentInternal,
        target.kotlinTargetComponent,
    )
}

internal class ExternalKotlinTargetSoftwareComponent @UnsafeApi constructor(
    private val multiplatformExtension: KotlinMultiplatformExtension,
    private val adhocSoftwareComponent: SoftwareComponentInternal,
    private val kotlinTargetComponent: ExternalKotlinTargetComponent,
) : KotlinTargetSoftwareComponent() {

    override fun getName(): String = adhocSoftwareComponent.name
    override fun getUsages(): Set<UsageContext> = adhocSoftwareComponent.usages
    override fun getVariants(): Set<SoftwareComponent> = multiplatformExtension.metadata().components
    override fun getCoordinates(): ModuleVersionIdentifier = kotlinTargetComponent.coordinates
}
