/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal object MultiplatformLayoutV2DefaultManifestLocationConfigurator : KotlinAndroidSourceSetConfigurator {
    private const val DEFAULT_FILE_NAME = "AndroidManifest.xml"
    private val logger = Logging.getLogger(javaClass)

    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        val defaultManifestLocation = target.project.file("src/${androidSourceSet.name}/$DEFAULT_FILE_NAME")
        if (androidSourceSet.manifest.srcFile != defaultManifestLocation) {
            logger.debug(
                """
                    ${androidSourceSet.name}: Default Manifest location was already changed
                    Expected: $defaultManifestLocation, Found: ${androidSourceSet.manifest.srcFile}
                """.trimIndent()
            )
            return
        }

        val newManifestLocation = target.project.file("src/${kotlinSourceSet.name}/$DEFAULT_FILE_NAME")
        androidSourceSet.manifest.srcFile(newManifestLocation)
        logger.debug("${androidSourceSet.name}: Changed default Manifest location to $newManifestLocation")
    }
}
