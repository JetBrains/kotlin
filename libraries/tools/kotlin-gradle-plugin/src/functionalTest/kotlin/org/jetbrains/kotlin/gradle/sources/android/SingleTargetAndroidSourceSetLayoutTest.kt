/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.sources.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.addBuildEventsListenerRegistryMock
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.androidExtension
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SingleTargetAndroidSourceSetLayoutTest {
    private val project = ProjectBuilder.builder().build() as ProjectInternal

    private val android: BaseExtension = run {
        addBuildEventsListenerRegistryMock(project)
        project.plugins.apply(LibraryPlugin::class.java)
        project.androidExtension
    }

    @BeforeTest
    fun setup() {
        project.plugins.apply(KotlinAndroidPluginWrapper::class.java)
        android.compileSdkVersion(31)
    }

    @Test
    fun `test - default configuration - AndroidSourceSet has associated KotlinSourceSet`() {
        android.sourceSets.all { androidSourceSet -> project.getKotlinSourceSetOrFail(androidSourceSet) }
        project.evaluate()
    }

    @Test
    @Suppress("deprecation")
    fun `test - default configuration - AndroidSourceSet has KotlinSourceSet as convention`() {
        android.sourceSets.all { androidSourceSet ->
            assertSame(
                project.getKotlinSourceSetOrFail(androidSourceSet),
                (androidSourceSet as org.gradle.api.internal.HasConvention).convention.plugins["kotlin"] as? KotlinSourceSet,
                "Expected Convention 'kotlin' on AndroidSourceSet: ${androidSourceSet.name}"
            )
        }
        project.evaluate()
    }


    @Test
    fun `test - with flavors - AndroidSourceSet has associated KotlinSourceSet`() {
        android.flavorDimensions("price", "market")
        android.productFlavors {
            it.create("free").dimension = "price"
            it.create("paid").dimension = "price"
            it.create("german").dimension = "market"
            it.create("usa").dimension = "market"
        }

        android.sourceSets.all { androidSourceSet ->
            val kotlinSourceSet = project.getKotlinSourceSetOrFail(androidSourceSet)
            assertEquals(androidSourceSet.name, kotlinSourceSet.name)
        }
    }
}