/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.mpp.metadataDependencyResolutionsOrEmpty
import org.jetbrains.kotlin.gradle.plugin.mpp.transformMetadataLibrariesForIde
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.metadataTransformation
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

class KT58319ProjectMetadataProvider {
    @Test
    fun `test - ProjectMetadataProviderImpl - supports single target projects`() {
        val rootProject = buildProject()
        val producerProject = buildProjectWithMPP(projectBuilder = { withParent(rootProject) })
        val consumerProject = buildProjectWithMPP(projectBuilder = { withParent(rootProject) })

        producerProject.multiplatformExtension.jvm()
        consumerProject.multiplatformExtension.jvm()

        val consumerCommonMain = consumerProject.multiplatformExtension.sourceSets.getByName("commonMain")
        consumerCommonMain.dependencies {
            implementation(producerProject)
        }

        rootProject.evaluate()
        producerProject.evaluate()
        consumerProject.evaluate()

        /*
       Regression failure reported:
       Caused by: java.lang.IllegalStateException: Unexpected source set 'commonMain'
         at org.jetbrains.kotlin.gradle.plugin.mpp.ProjectMetadataProviderImpl.getSourceSetCompiledMetadata(ProjectMetadataProviderImpl.kt:42)
       > at org.jetbrains.kotlin.gradle.plugin.mpp.TransformMetadataLibrariesKt.transformMetadataLibrariesForIde(transformMetadataLibraries.kt:26)
         at org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet.getDependenciesTransformation$kotlin_gradle_plugin_common(DefaultKotlinSourceSet.kt:178)
         at org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet.getDependenciesTransformation(DefaultKotlinSourceSet.kt:151)
        */
        consumerCommonMain.internal.metadataTransformation.metadataDependencyResolutionsOrEmpty
            .filterIsInstance<MetadataDependencyResolution.ChooseVisibleSourceSets>()
            .ifEmpty { fail("Expected at least one 'ChooseVisibleSourceSets") }
            .forEach { resolution -> assertNotNull(consumerProject.transformMetadataLibrariesForIde(resolution)) }


        /*
        Regression failure reported:
        Caused by: java.lang.IllegalStateException: Unexpected source set 'commonMain'
          at org.jetbrains.kotlin.gradle.plugin.mpp.ProjectMetadataProviderImpl.getSourceSetCompiledMetadata(ProjectMetadataProviderImpl.kt:42)
          at org.jetbrains.kotlin.gradle.plugin.mpp.TransformMetadataLibrariesKt.transformMetadataLibrariesForIde(transformMetadataLibraries.kt:26)
        > at org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet.getDependenciesTransformation$kotlin_gradle_plugin_common(DefaultKotlinSourceSet.kt:178)
          at org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet.getDependenciesTransformation(DefaultKotlinSourceSet.kt:151)
        */
        (consumerCommonMain as DefaultKotlinSourceSet).getDependenciesTransformation().toList()
            .ifEmpty { fail("getDependenciesTransformation() returned nothing") }
    }
}