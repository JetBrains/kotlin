/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.IdeDependsOnDependencyResolver
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test

class IdeDependsOnDependencyResolverTest {

    @Test
    fun `test - sample 0 - default dependsOn to commonMain and commonTest`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.targetHierarchy.default()
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")

        IdeDependsOnDependencyResolver.resolve(jvmMain)
            .filterIsInstance<IdeaKotlinSourceDependency>()
            .assertMatches(dependsOnDependency(commonMain))


        IdeDependsOnDependencyResolver.resolve(jvmTest)
            .filterIsInstance<IdeaKotlinSourceDependency>()
            .assertMatches(dependsOnDependency(commonTest))
    }

    @Test
    fun `test - sample 1 - custom dependsOn edge`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.targetHierarchy.default()
        kotlin.jvm()
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val customMain = kotlin.sourceSets.create("customMain")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")

        jvmMain.dependsOn(customMain)
        customMain.dependsOn(commonMain)

        IdeDependsOnDependencyResolver.resolve(jvmMain)
            .filterIsInstance<IdeaKotlinSourceDependency>()
            .assertMatches(
                dependsOnDependency(commonMain),
                dependsOnDependency(customMain)
            )

        IdeDependsOnDependencyResolver.resolve(customMain)
            .filterIsInstance<IdeaKotlinSourceDependency>()
            .assertMatches(dependsOnDependency(commonMain))
    }
}