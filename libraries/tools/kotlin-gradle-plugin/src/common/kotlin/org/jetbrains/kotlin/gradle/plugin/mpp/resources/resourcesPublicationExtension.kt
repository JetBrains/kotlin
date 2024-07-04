/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.utils.getOrPut

val KotlinMultiplatformExtension.resourcesPublicationExtension: KotlinTargetResourcesPublicationImpl?
    get() {
        if (!project.kotlinPropertiesProvider.mppResourcesPublication) return null
        return project.extraProperties.getOrPut(KotlinTargetResourcesPublication.EXTENSION_NAME) {
            project.objects.newInstance(
                KotlinTargetResourcesPublicationImpl::class.java,
                project,
            )
        }
    }

internal val RegisterMultiplatformResourcesPublicationExtensionAction = KotlinProjectSetupAction {
    multiplatformExtension.resourcesPublicationExtension
}