/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val KotlinMultiplatformExtension.resourcesPublicationExtension: KotlinTargetResourcesPublication
    get() {
        (this as ExtensionAware)
        if (extensions.findByName(KotlinTargetResourcesPublication.EXTENSION_NAME) == null) {
            extensions.add(
                Any::class.java,
                KotlinTargetResourcesPublication.EXTENSION_NAME,
                project.objects.newInstance(KotlinTargetResourcesPublication::class.java, project)
            )
        }
        return extensions.getByName(KotlinTargetResourcesPublication.EXTENSION_NAME) as KotlinTargetResourcesPublication
    }