/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources

import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import kotlin.reflect.KClass

internal val KotlinAndroidTarget.androidResourcesPublicationExtension: AndroidKotlinTargetResourcesPublicationImpl
    get() = resourcesPublicationExtension as AndroidKotlinTargetResourcesPublicationImpl

internal val <T> T.resourcesPublicationExtension: KotlinTargetResourcesPublicationImpl<T>
where T : KotlinTarget, T : KotlinTargetWithPublishableMultiplatformResources
    get() {
        (this as ExtensionAware)
        val resourcesPublicationType: KClass<*> = if (this is KotlinAndroidTarget) {
            AndroidKotlinTargetResourcesPublicationImpl::class
        } else {
            KotlinTargetResourcesPublicationImpl::class
        }
        if (extensions.findByName(KotlinTargetResourcesPublicationImpl.EXTENSION_NAME) == null) {
            extensions.add(
                Any::class.java,
                KotlinTargetResourcesPublicationImpl.EXTENSION_NAME,
                project.objects.newInstance(resourcesPublicationType.java, this)
            )
        }
        @Suppress("UNCHECKED_CAST")
        return extensions.getByName(KotlinTargetResourcesPublicationImpl.EXTENSION_NAME) as KotlinTargetResourcesPublicationImpl<T>
    }

internal val <T> T.resourcesResolutionExtension: KotlinTargetResourcesResolutionImpl<T>
where T : KotlinTarget, T : KotlinTargetWithResolvableMultiplatformResources, T : KotlinTargetWithPublishableMultiplatformResources
    get() {
        (this as ExtensionAware)
        if (extensions.findByName(KotlinTargetResourcesResolutionImpl.EXTENSION_NAME) == null) {
            extensions.add(
                Any::class.java,
                KotlinTargetResourcesResolutionImpl.EXTENSION_NAME,
                project.objects.newInstance(KotlinTargetResourcesResolutionImpl::class.java, this)
            )
        }
        @Suppress("UNCHECKED_CAST")
        return extensions.getByName(KotlinTargetResourcesResolutionImpl.EXTENSION_NAME) as KotlinTargetResourcesResolutionImpl<T>
    }