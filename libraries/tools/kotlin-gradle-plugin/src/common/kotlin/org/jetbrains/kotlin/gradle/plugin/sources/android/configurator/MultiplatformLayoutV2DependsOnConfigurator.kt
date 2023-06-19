/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidBaseSourceSetName
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type
import org.jetbrains.kotlin.gradle.plugin.sources.android.variantType
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal object MultiplatformLayoutV2DependsOnConfigurator : KotlinAndroidSourceSetConfigurator {
    @Suppress("DEPRECATION")
    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        val androidBaseSourceSetName = AndroidBaseSourceSetName.byName(androidSourceSet.name) ?: return
        setDefaultDependsOn(target, kotlinSourceSet, androidBaseSourceSetName.variantType)
    }

    override fun configureWithVariant(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, variant: BaseVariant) {
        setDefaultDependsOn(target, kotlinSourceSet, variant.type)
    }

    private fun setDefaultDependsOn(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, variantType: AndroidVariantType) {
        target.project.launchInStage(KotlinPluginLifecycle.Stage.FinaliseRefinesEdges) {
            /* Only setup default if no hierarchy template was applied */
            if (target.project.multiplatformExtensionOrNull?.hierarchy?.appliedTemplates.orEmpty().isNotEmpty()) {
                return@launchInStage
            }

            val sourceSetTree = when (variantType) {
                AndroidVariantType.Main -> target.mainVariant.sourceSetTree.awaitFinalValue()
                AndroidVariantType.UnitTest -> target.unitTestVariant.sourceSetTree.awaitFinalValue()
                AndroidVariantType.InstrumentedTest -> target.instrumentedTestVariant.sourceSetTree.awaitFinalValue()
                AndroidVariantType.Unknown -> null
            } ?: return@launchInStage

            val commonSourceSetName = lowerCamelCaseName("common", sourceSetTree.name)
            val commonSourceSet = target.project.kotlinExtension.sourceSets.findByName(commonSourceSetName) ?: return@launchInStage
            kotlinSourceSet.dependsOn(commonSourceSet)
        }
    }
}
