/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout

/**
 * Will detect usage of "Android Style" source directories (like 'src/main/kotlin') and emit a warning
 */
internal object MultiplatformLayoutV2AndroidStyleSourceDirUsageChecker : KotlinAndroidSourceSetLayoutChecker {

    private val logger = Logging.getLogger(this.javaClass)

    override fun checkCreatedSourceSet(
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        val androidStyleSourceDir = target.project.file("src/${androidSourceSet.name}/kotlin")
        if (androidStyleSourceDir in kotlinSourceSet.kotlin.srcDirs && androidStyleSourceDir.exists()) {
            logger.warn(
                """
                    w: ${layout.name}: Usage of 'Android Style' source directory $androidStyleSourceDir is deprecated.
                    Use ${target.project.file("src/${kotlinSourceSet.name}/kotlin")} instead.
                """.trimIndent()
            )
        }
    }
}
