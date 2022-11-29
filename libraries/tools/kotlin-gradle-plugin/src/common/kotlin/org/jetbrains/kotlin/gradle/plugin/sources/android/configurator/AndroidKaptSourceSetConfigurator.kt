/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.whenKaptEnabled
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal object AndroidKaptSourceSetConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(
        target: KotlinAndroidTarget,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) = target.project.whenKaptEnabled {
        Kapt3GradleSubplugin.createAptConfigurationIfNeeded(target.project, androidSourceSet.name)
    }
}
