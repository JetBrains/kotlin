/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.*

internal object MultiplatformLayoutV1SourceDirConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
    ) {
        /*
        Mitigate ambiguity!
        Example: disambiguationClassifier="android"
        Source Directory "src/androidTest/kotlin"
            -- could be claimed by kotlin {android}Test (unit test)
            -- could be claimed by Android androidTest (instrumented test)

        The Kotlin source set would win in this scenario.
         */
        if (!androidSourceSet.name.startsWith(target.disambiguationClassifier)) {
            kotlinSourceSet.kotlin.srcDir("src/${androidSourceSet.name}/kotlin")
        }
    }
}