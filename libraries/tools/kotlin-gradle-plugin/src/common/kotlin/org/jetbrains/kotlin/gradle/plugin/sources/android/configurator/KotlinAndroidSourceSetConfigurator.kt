/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.*

internal interface KotlinAndroidSourceSetConfigurator {
    /**
     * Called once, when the corresponding KotlinSourceSet is created for a given [DeprecatedAndroidSourceSet].
     * Note, this can also be called in 'afterEvaluate', when Android is finalizing its variants.
     */
    fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
    ) = Unit

    /**
     * Called every time, when a given [KotlinSourceSet] participates in a given Android variant.
     */
    fun configureWithVariant(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") variant: DeprecatedAndroidBaseVariant
    ) = Unit
}

internal fun KotlinAndroidSourceSetConfigurator.onlyIf(
    condition: (target: KotlinAndroidTarget) -> Boolean
): KotlinAndroidSourceSetConfigurator {
    return KotlinAndroidSourceSetConfigurationWithCondition(this, condition)
}

/* Conditional implementation */
private class KotlinAndroidSourceSetConfigurationWithCondition(
    private val underlying: KotlinAndroidSourceSetConfigurator,
    private val condition: (KotlinAndroidTarget) -> Boolean
) : KotlinAndroidSourceSetConfigurator {
    override fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
    ) {
        if (condition(target)) underlying.configure(target, kotlinSourceSet, androidSourceSet)
    }

    override fun configureWithVariant(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") variant: DeprecatedAndroidBaseVariant
    ) {
        if (condition(target)) underlying.configureWithVariant(target, kotlinSourceSet, variant)
    }
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
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
    ) {
        configurators.forEach { configurator ->
            configurator.configure(target, kotlinSourceSet, androidSourceSet)
        }
    }

    override fun configureWithVariant(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") variant: DeprecatedAndroidBaseVariant
    ) {
        configurators.forEach { configurator ->
            configurator.configureWithVariant(target, kotlinSourceSet, variant)
        }
    }
}
