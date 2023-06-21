/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.commonMain
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformSourceSetConventionsImpl.dependencies
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.resolveMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test

class KT59446TransformationOnTransitiveJavaDependencyTest {
    @Test
    fun `test - transform transitive java dependency`() {
        val rootProject = buildProject()
        val projectA = buildProjectWithMPP(projectBuilder = { withParent(rootProject).withName("a") })
        val projectB = buildProjectWithMPP(projectBuilder = { withParent(rootProject).withName("b") })
        val projectJava = buildProject(projectBuilder = { withParent(rootProject).withName("java") })

        /**
         * a -> b -> java
         */
        projectJava.plugins.apply("java-library")

        projectA.multiplatformExtension.jvm()
        projectB.multiplatformExtension.jvm()

        projectB.multiplatformExtension.sourceSets.commonMain.dependencies {
            api(project(":java"))
        }

        projectA.multiplatformExtension.sourceSets.commonMain.dependencies {
            api(project(":b"))
        }

        /*
        Call transformation
        Exception was:
        org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle$IllegalLifecycleException: Future was not completed yet 'Kotlin Plugin Lifecycle: (project ':java') *not started*'
	        at org.jetbrains.kotlin.gradle.utils.FutureImpl.getOrThrow(Future.kt:113)
	        at org.jetbrains.kotlin.gradle.utils.LenientFutureImpl.getOrThrow(Future.kt:138)
	        at org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation.toModuleDependencyIdentifier(GranularMetadataTransformation.kt:303)
	        at org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation.processDependency(GranularMetadataTransformation.kt:260)
        */
        projectA.multiplatformExtension.sourceSets.commonMain.get().resolveMetadata<MetadataDependencyResolution>()
    }
}