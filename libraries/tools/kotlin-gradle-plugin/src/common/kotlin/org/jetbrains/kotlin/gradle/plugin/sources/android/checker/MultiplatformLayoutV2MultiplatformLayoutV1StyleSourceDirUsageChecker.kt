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
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV1

/**
 * Detects and warns about usage of source directories from multiplatform layout version 1
 */
internal object MultiplatformLayoutV2MultiplatformLayoutV1StyleSourceDirUsageChecker : KotlinAndroidSourceSetLayoutChecker {

    private val logger = Logging.getLogger(this.javaClass)

    override fun checkCreatedSourceSet(
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        val v1kotlinSourceSetName = multiplatformAndroidSourceSetLayoutV1.naming.kotlinSourceSetName(
            target.disambiguationClassifier, androidSourceSet.name
        ) ?: return

        /* Layouts did agree on the name of this KotlinSourceSet -> LGTM */
        if (v1kotlinSourceSetName == kotlinSourceSet.name) return

        val v1KotlinSourceDir = target.project.file("src/$v1kotlinSourceSetName/kotlin")
        if (v1KotlinSourceDir.exists()) {
            logger.warn(
                """
                    w: ${layout.name}: Found used source directory $v1KotlinSourceDir
                    This source directory was supported by: ${multiplatformAndroidSourceSetLayoutV1.name}
                    Current KotlinAndroidSourceSetLayout: ${layout.name}
                    New source directory is: ${target.project.file("src/${kotlinSourceSet.name}/kotlin")}
                """.trimIndent()
            )
        }
    }
}
