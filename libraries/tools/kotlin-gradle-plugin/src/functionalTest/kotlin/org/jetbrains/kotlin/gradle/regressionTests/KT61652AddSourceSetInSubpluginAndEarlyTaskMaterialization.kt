/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.util.assertDoesNotThrow
import kotlin.test.Test

class KT61652AddSourceSetInSubpluginAndEarlyTaskMaterialization {
    @Test
    fun `test GranularMetadataTransformation doesn't fail when it is created too early and subplugin applied`() {
        class TestSubplugin : KotlinCompilerPluginSupportPlugin {
            override fun getCompilerPluginId(): String = "test"
            override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact("test", "test")

            override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

            override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
                val project = kotlinCompilation.project
                val emptyResult = project.provider { emptyList<SubpluginOption>() }

                if (kotlinCompilation !is KotlinMetadataCompilation) return emptyResult
                if (kotlinCompilation.name == "main") return emptyResult

                project
                    .kotlinExtension
                    .sourceSets
                    .create("generatedBy" + kotlinCompilation.defaultSourceSet.name)

                return emptyResult
            }
        }

        val root = buildProject {}
        val lib = buildProjectWithMPP(projectBuilder = { withParent(root).withName("lib") }) {
            kotlin { jvm(); linuxX64() }
        }

        val app = buildProjectWithMPP(projectBuilder = { withParent(root).withName("app") }) {
            kotlin {
                jvm()
                linuxX64()
                sourceSets.getByName("commonMain").dependencies { api(project(":lib")) }
            }
        }

        lib.plugins.apply(TestSubplugin::class.java)
        app.evaluate()

        /**
         * Create GranularMetadataTransformation.Params artificially to reproduce real-case scenario when
         * either IDE dependencies resolvers create it or transform*DependenciesMetadata
         */
        val gmtParams = GranularMetadataTransformation.Params(app, app.kotlinExtension.sourceSets.getByName("commonMain"))
        val allProjectsData = gmtParams.projectData
        lib.evaluate()

        assertDoesNotThrow { allProjectsData[":lib"]!!.sourceSetMetadataOutputs.getOrThrow() }
    }
}