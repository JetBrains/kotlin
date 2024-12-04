/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.util.MultiplatformExtensionTest
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import kotlin.test.Test

class KT46273JvmTargetAndOrphanTestSourceSetTest : MultiplatformExtensionTest() {

    @Test
    fun `test KT-46273`() {
        kotlin.jvm()

        val commonTest = kotlin.sourceSets.getByName("commonTest")
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        val orphanTestSourceSet = kotlin.sourceSets.create("orphanTest") as DefaultKotlinSourceSet
        orphanTestSourceSet.dependsOn(commonTest)

        project.evaluate()

        /*
        Previously failed with:
        Collection is empty.
        java.util.NoSuchElementException: Collection is empty.
	    at kotlin.collections.CollectionsKt___CollectionsKt.single(_Collections.kt:562)
	    at org.jetbrains.kotlin.gradle.internal.KotlinDependenciesManagementKt.kotlinTestCapabilityForJvmSourceSet(KotlinDependenciesManagement.kt:347)

	    The dependencies' transformation will be called by the IDE during import.
         */
        orphanTestSourceSet.getDependenciesTransformation()
    }
}
