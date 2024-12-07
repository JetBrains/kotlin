/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.utils.targets
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourcesTasksTest {
    @Test
    fun `test - default resources task names are consistent`() {
        val project = buildProjectWithJvm()
        val jvmTarget = project.kotlinExtension.targets.single()
        val expectedProcessResourceTaskNames = setOf("processResources", "processTestResources")
        val actualProcessResourceTaskNames = project.tasks.withType<ProcessResources>().names
        val kotlinReportedProcessResourceTaskNames = hashSetOf<String>()
        for (compilation in jvmTarget.compilations) {
            val processResourcesTaskName = (compilation as? InternalKotlinCompilation<*>)?.processResourcesTaskName
            if (processResourcesTaskName != null) {
                kotlinReportedProcessResourceTaskNames.add(processResourcesTaskName)
            }
        }
        assertEquals(expectedProcessResourceTaskNames, actualProcessResourceTaskNames)
        assertEquals(expectedProcessResourceTaskNames, kotlinReportedProcessResourceTaskNames)
    }
}