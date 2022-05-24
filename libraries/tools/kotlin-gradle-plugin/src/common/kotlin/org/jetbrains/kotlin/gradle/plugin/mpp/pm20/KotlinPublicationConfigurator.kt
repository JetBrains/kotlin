/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

interface KotlinPublicationConfigurator<in T : KpmGradleVariant> : KotlinGradleFragmentFactory.FragmentConfigurator<T> {

    object NoPublication : KotlinPublicationConfigurator<KpmGradleVariant> {
        override fun configure(fragment: KpmGradleVariant) = Unit
    }

    object SingleVariantPublication : KotlinPublicationConfigurator<KpmGradlePublishedVariantWithRuntime> {
        override fun configure(fragment: KpmGradlePublishedVariantWithRuntime) {
            VariantPublishingConfigurator.get(fragment.project).configureSingleVariantPublication(fragment)
        }
    }

    object NativeVariantPublication : KotlinPublicationConfigurator<KpmNativeVariantInternal> {
        override fun configure(fragment: KpmNativeVariantInternal) {
            VariantPublishingConfigurator.get(fragment.project).configureNativeVariantPublication(fragment)
        }
    }
}
