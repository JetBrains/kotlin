/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal object MultiplatformLayoutV1SourceDirConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
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

        kotlinSourceSet.kotlin.srcDir(target.project.provider { androidSourceSet.java.srcDirs })
    }
}