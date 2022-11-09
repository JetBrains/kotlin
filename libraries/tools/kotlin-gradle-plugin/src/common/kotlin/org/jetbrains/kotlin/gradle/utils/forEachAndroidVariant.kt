/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectSet
import org.jetbrains.kotlin.gradle.plugin.BaseVariantWrapper
import org.jetbrains.kotlin.gradle.plugin.VariantWrapper
import org.jetbrains.kotlin.gradle.targets.android.internal.AndroidDependencyResolver.getMethodOrNull

internal fun Project.forAllAndroidVariants(action: (VariantWrapper) -> Unit) {
    val baseAction : (BaseVariant) -> Unit = { variant ->
        action.invoke(BaseVariantWrapper(variant))
    }
    /*val newApiAction : (Variant) -> Unit = { variant ->
        //action.invoke(NewApiVariantWrapper(variant)) //TODO
    }

    val newExtension = this.extensions.getByName("androidComponents")
    if (newExtension is AndroidComponentsExtension<*, *, *>) {
        newExtension.onVariants {
            newApiAction.invoke(it) // hasTestFixtures
        }
    }*/

    val androidExtension = this.extensions.getByName("android")
    when (androidExtension) {
        is AppExtension -> androidExtension.applicationVariants.all(baseAction)
        is LibraryExtension -> {
            androidExtension.libraryVariants.all(baseAction)
            if (androidExtension is FeatureExtension) {
                val getFeature = androidExtension::class.java.getMethodOrNull("getFeatureVariants")
                val featureVariants = getFeature?.invoke(androidExtension) as? DefaultDomainObjectSet<BaseVariant>
                featureVariants?.all(baseAction)
            }
        }

        is TestExtension -> androidExtension.applicationVariants.all(baseAction)
    }
    if (androidExtension is TestedExtension) {
        androidExtension.testVariants.all(baseAction)
        androidExtension.unitTestVariants.all(baseAction)
    }
}
