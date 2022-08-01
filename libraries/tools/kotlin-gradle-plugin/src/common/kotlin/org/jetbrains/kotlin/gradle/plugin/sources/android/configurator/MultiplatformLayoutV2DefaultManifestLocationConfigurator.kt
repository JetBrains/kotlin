/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal object MultiplatformLayoutV2DefaultManifestLocationConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        /* Default can only be set when the entity is created 'fresh'. Changes here in afterEvaluate might overwrite user setup */
        if (!target.project.state.executed) {
            androidSourceSet.manifest.srcFile("src/${kotlinSourceSet.name}/AndroidManifest.xml")
        }
    }
}
