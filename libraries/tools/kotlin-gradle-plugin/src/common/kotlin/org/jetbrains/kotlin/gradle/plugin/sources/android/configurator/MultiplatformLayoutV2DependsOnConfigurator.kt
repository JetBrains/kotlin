/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidBaseSourceSetName
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type
import org.jetbrains.kotlin.gradle.plugin.sources.android.variantType

internal object MultiplatformLayoutV2DependsOnConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        val androidBaseSourceSetName = AndroidBaseSourceSetName.byName(androidSourceSet.name) ?: return
        setDefaultDependsOn(target, kotlinSourceSet, androidBaseSourceSetName.variantType)
    }

    override fun configureWithVariant(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, variant: BaseVariant) {
        setDefaultDependsOn(target, kotlinSourceSet, variant.type)
    }

    private fun setDefaultDependsOn(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, variantType: AndroidVariantType) {
        /* Add default dependency on 'commonMain' or 'commonTest' */
        val commonMain = target.project.kotlinExtension.sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME)
        val commonTest = target.project.kotlinExtension.sourceSets.getByName(COMMON_TEST_SOURCE_SET_NAME)

        when (variantType) {
            AndroidVariantType.Main -> kotlinSourceSet.dependsOn(commonMain)
            AndroidVariantType.UnitTest -> kotlinSourceSet.dependsOn(commonTest)
            AndroidVariantType.InstrumentedTest, AndroidVariantType.Unknown -> Unit
        }
    }
}
