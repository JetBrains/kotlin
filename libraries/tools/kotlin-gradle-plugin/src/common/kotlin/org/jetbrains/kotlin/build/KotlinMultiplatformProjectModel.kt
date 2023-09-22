/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.gradle.api.Project
import org.jetbrains.kotlin.build.targets.AllTargetEventListeners
import org.jetbrains.kotlin.build.targets.GradleTargetEventListener
import org.jetbrains.kotlin.build.targets.TargetsHandler
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.utils.getOrPut

internal class KotlinMultiplatformProjectModel(
    private val eventListener: KotlinMultiplatformProjectEventListener,
    val targetsHandler: TargetsHandler
) {
    fun apply() {
        eventListener.onConfigurationStart()
    }
}

internal fun createKotlinProjectModelForGradle(
    project: Project
): KotlinMultiplatformProjectModel {
    val gradleTargetEventListener = GradleTargetEventListener(project, project.multiplatformExtension)

    val targetsHandler = TargetsHandler(
        eventsListener = AllTargetEventListeners(gradleTargetEventListener)
    )

    return KotlinMultiplatformProjectModel(
        eventListener = AllKotlinMultiplatformProjectEventListeners(
            gradleTargetEventListener
        ),
        targetsHandler = targetsHandler
    )
}

internal val Project.kotlinMultiplatformProjectModel: KotlinMultiplatformProjectModel get() = extensions
        .extraProperties
        .getOrPut("org.jetbrains.kotlin.gradle.plugin.kotlinMultiplatformProjectModel") {
            createKotlinProjectModelForGradle(project)
        }

internal interface KotlinMultiplatformProjectEventListener {
    fun onConfigurationStart()
}

internal class AllKotlinMultiplatformProjectEventListeners(
    vararg initialListeners: KotlinMultiplatformProjectEventListener
) : KotlinMultiplatformProjectEventListener {
    private val all: MutableSet<KotlinMultiplatformProjectEventListener> = mutableSetOf()

    init {
        all.addAll(initialListeners)
    }

    fun register(listener: KotlinMultiplatformProjectEventListener) = all.add(listener)
    override fun onConfigurationStart() = all.forEach { it.onConfigurationStart() }
}