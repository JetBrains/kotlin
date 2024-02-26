/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import java.io.File
import javax.inject.Inject

internal abstract class KotlinTargetResourcesPublicationImpl @Inject constructor(
    val project: Project
) : KotlinTargetResourcesPublication {

    internal data class TargetResources(
        val resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceRoot),
        val relativeResourcePlacement: Provider<File>,
    )

    private val targetsThatSupportPublication = listOf(
        KotlinJvmTarget::class,
    )

    private val targetToResourcesMap: MutableMap<KotlinTarget, TargetResources> = mutableMapOf()
    private val targetResourcesSubscribers: MutableMap<KotlinTarget, MutableList<(TargetResources) -> (Unit)>> = mutableMapOf()

    internal fun subscribeOnPublishResources(
        target: KotlinTarget,
        notify: (TargetResources) -> (Unit),
    ) {
        targetToResourcesMap[target]?.let(notify)
        targetResourcesSubscribers.getOrPut(target, { mutableListOf() }).add(notify)
    }


    override fun canPublishResources(target: KotlinTarget): Boolean {
        if (targetsThatSupportPublication.none { it.isInstance(target) }) return false
        return true
    }

    override fun publishResourcesAsKotlinComponent(
        target: KotlinTarget,
        resourcePathForSourceSet: (KotlinSourceSet) -> (KotlinTargetResourcesPublication.ResourceRoot),
        relativeResourcePlacement: Provider<File>,
    ) {
        if (!canPublishResources(target)) {
            target.project.reportDiagnostic(KotlinToolingDiagnostics.ResourceMayNotBePublishedForTarget(target.name))
            return
        }
        if (targetToResourcesMap[target] != null) {
            target.project.reportDiagnostic(KotlinToolingDiagnostics.ResourcePublishedMoreThanOncePerTarget(target.name))
            return
        }

        val resources = TargetResources(
            resourcePathForSourceSet = resourcePathForSourceSet,
            relativeResourcePlacement = relativeResourcePlacement,
        )
        targetToResourcesMap[target] = resources
        targetResourcesSubscribers[target].orEmpty().forEach { notify ->
            notify(resources)
        }
    }
    internal companion object {
        const val MULTIPLATFORM_RESOURCES_DIRECTORY = "kotlin-multiplatform-resources"
    }

}
