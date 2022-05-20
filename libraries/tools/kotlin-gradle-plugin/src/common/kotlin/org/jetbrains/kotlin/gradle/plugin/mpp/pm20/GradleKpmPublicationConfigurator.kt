/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

interface GradleKpmPublicationConfigurator<in T : GradleKpmVariant> : GradleKpmFragmentFactory.FragmentConfigurator<T> {

    object NoPublication : GradleKpmPublicationConfigurator<GradleKpmVariant> {
        override fun configure(fragment: GradleKpmVariant) = Unit
    }

    object SingleVariantPublication : GradleKpmPublicationConfigurator<GradleKpmPublishedVariantWithRuntime> {
        override fun configure(fragment: GradleKpmPublishedVariantWithRuntime) {
            GradleKpmVariantPublishingConfigurator.get(fragment.project).configureSingleVariantPublication(fragment)
        }
    }

    object NativeVariantPublication : GradleKpmPublicationConfigurator<GradleKpmNativeVariantInternal> {
        override fun configure(fragment: GradleKpmNativeVariantInternal) {
            GradleKpmVariantPublishingConfigurator.get(fragment.project).configureNativeVariantPublication(fragment)
        }
    }
}
