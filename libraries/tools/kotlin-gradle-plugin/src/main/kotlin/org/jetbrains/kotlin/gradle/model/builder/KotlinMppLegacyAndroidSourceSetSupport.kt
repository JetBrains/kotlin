/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.logging.kotlinWarn
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

internal object KotlinMppLegacyAndroidSourceSetSupport {
    private val logger = Logging.getLogger(this.javaClass)

    fun configureLegacySourceSetSupport(
        project: Project,
        kotlinMppSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        val legacyAndroidOnlySourceDirs = androidOnlySourceDirs(kotlinMppSourceSet, androidSourceSet)
        warnUsagesOfDeprecatedSourceDirs(kotlinMppSourceSet, legacyAndroidOnlySourceDirs)
        registerLegacySourceSetsInKotlinMppSourceSet(project, kotlinMppSourceSet, legacyAndroidOnlySourceDirs)
    }

    fun androidOnlySourceDirs(
        kotlinMppSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ): List<File> = androidSourceSet.java.srcDirs.filter { srcDir -> srcDir !in kotlinMppSourceSet.kotlin.srcDirs }

    private fun warnUsagesOfDeprecatedSourceDirs(kotlinMppSourceSet: KotlinSourceSet, deprecatedSourceDir: List<File>) {
        deprecatedSourceDir.filter { sourceDir -> sourceDir.exists() }
            .forEach { sourceDir -> warnUsageOfDeprecatedSourceDir(kotlinMppSourceSet, sourceDir) }
    }

    private fun warnUsageOfDeprecatedSourceDir(kotlinMppSourceSet: KotlinSourceSet, deprecatedSourceDir: File) {
        logger.kotlinWarn(
            "Android source directory ${deprecatedSourceDir.absolutePath} is deprecated in Kotlin Multiplatform projects. " +
                    "Use one of ${kotlinMppSourceSet.kotlin.srcDirs.joinToString(",") { it.absolutePath }} instead"
        )
    }

    fun registerLegacySourceSetsInKotlinMppSourceSet(
        project: Project,
        kotlinMppSourceSet: KotlinSourceSet,
        legacySourceDirs: List<File>
    ) = legacySourceDirs.forEach { legacySourceDir ->
        kotlinMppSourceSet.kotlin.srcDir(legacySourceDir)
        logger.kotlinDebug(
            "${project.path}: ${kotlinMppSourceSet.name} support added for legacy source directory: ${legacySourceDir.path}"
        )
    }


    fun configureAndroidManifest(project: Project, kotlinMppSourceSets: KotlinSourceSet, androidSourceSet: AndroidSourceSet) {
        if (!androidSourceSet.manifest.srcFile.exists()) {
            val kotlinMppSourceSetManifestFile = project.file("src/${kotlinMppSourceSets.name}/${androidSourceSet.manifest.srcFile.name}")
            androidSourceSet.manifest.srcFile(kotlinMppSourceSetManifestFile)
            logger.kotlinDebug(
                "${project.path}: ${kotlinMppSourceSets.name} using android manifest file at ${kotlinMppSourceSetManifestFile.path}"
            )
        }
    }
}

