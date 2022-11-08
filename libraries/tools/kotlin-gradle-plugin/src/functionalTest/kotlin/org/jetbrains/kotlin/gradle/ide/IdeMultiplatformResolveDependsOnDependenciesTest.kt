/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.ide

import org.jetbrains.kotlin.gradle.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.ide.IdeSourceDependency
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import kotlin.test.Test

class IdeMultiplatformResolveDependsOnDependenciesTest {

    @Test
    fun `test - sample 0 - default dependsOn to commonMain and commonTest`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")


        project.kotlinIdeMultiplatformImport.resolveDependencies(jvmMain)
            .filterIsInstance<IdeSourceDependency>()
            .assertMatches(dependsOnDependency(commonMain))


        project.kotlinIdeMultiplatformImport.resolveDependencies(jvmTest)
            .filterIsInstance<IdeSourceDependency>()
            .assertMatches(dependsOnDependency(commonTest))
    }

    @Test
    fun `test - sample 1 - custom dependsOn edge`() {
        val project = buildProjectWithMPP()
        val kotlin = project.multiplatformExtension
        kotlin.jvm()
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val customMain = kotlin.sourceSets.create("customMain")
        val jvmMain = kotlin.sourceSets.getByName("jvmMain")

        jvmMain.dependsOn(customMain)
        customMain.dependsOn(commonMain)

        project.kotlinIdeMultiplatformImport.resolveDependencies(jvmMain)
            .filterIsInstance<IdeSourceDependency>()
            .assertMatches(
                dependsOnDependency(commonMain),
                dependsOnDependency(customMain)
            )

        project.kotlinIdeMultiplatformImport.resolveDependencies(customMain)
            .filterIsInstance<IdeSourceDependency>()
            .assertMatches(dependsOnDependency(commonMain))
    }
}