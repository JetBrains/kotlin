/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal interface KotlinAndroidSourceSetConfigurator {
    /**
     * Called once, when the corresponding KotlinSourceSet is created for a given [AndroidSourceSet].
     * Note, this can also be called in 'afterEvaluate', when Android is finalizing its variants.
     */
    fun configure(
        target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet
    ) = Unit

    /**
     * Called every time, when a given [KotlinSourceSet] participates in a given Android variant.
     */
    fun configureWithVariant(
        target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, variant: BaseVariant
    ) = Unit
}

/* Composite implementation */

internal fun KotlinAndroidSourceSetConfigurator(
    vararg configurators: KotlinAndroidSourceSetConfigurator?
): KotlinAndroidSourceSetConfigurator {
    return CompositeKotlinAndroidSourceSetConfigurator(configurators.filterNotNull())
}

private class CompositeKotlinAndroidSourceSetConfigurator(
    val configurators: List<KotlinAndroidSourceSetConfigurator>
) : KotlinAndroidSourceSetConfigurator {
    override fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        configurators.forEach { configurator ->
            configurator.configure(target, kotlinSourceSet, androidSourceSet)
        }
    }

    override fun configureWithVariant(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, variant: BaseVariant) {
        configurators.forEach { configurator ->
            configurator.configureWithVariant(target, kotlinSourceSet, variant)
        }
    }
}
