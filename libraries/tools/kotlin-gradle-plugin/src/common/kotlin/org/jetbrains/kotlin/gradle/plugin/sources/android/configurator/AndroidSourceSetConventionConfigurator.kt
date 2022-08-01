/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_DSL_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.addConvention
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

/*
Used by AGP com.android.build.gradle.internal.utils.syncAgpAndKgpSources to automatically pick up sources
from this KotlinSourceSet. This mechanism is deprecated.
*/
@Suppress("DEPRECATION")
internal object AndroidSourceSetConventionConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        androidSourceSet.addConvention(KOTLIN_DSL_NAME, kotlinSourceSet)
    }
}