/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import kotlin.io.path.div
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KotlinImportModelsDumpTaskTest {
    @Test
    fun `does not register the dump task without the internal property`() {
        val project = buildProjectWithJvm { }
        project.evaluate()

        assertNull(project.tasks.findByName("dumpKotlinImportModels"))
    }

    @Test
    fun `registers the dump task when the internal property is set`() {
        val project = buildProjectWithJvm(
            preApplyCode = {
                propertiesExtension.set(
                    PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_DUMP_KOTLIN_IMPORT_MODELS,
                    true,
                )
            },
        )
        project.evaluate()

        val task = project.tasks.findByName("dumpKotlinImportModels")
        assertNotNull(task)
        assertEquals("ide", task.group)
    }

    @Test
    fun `wraps dump write failures with the file name`() {
        val project = buildProjectWithJvm(
            preApplyCode = {
                propertiesExtension.set(
                    PropertiesProvider.PropertyNames.KOTLIN_INTERNAL_DUMP_KOTLIN_IMPORT_MODELS,
                    true,
                )
            },
        )
        project.evaluate()
        val task = project.tasks.getByName("dumpKotlinImportModels") as KotlinImportModelsDumpTask

        // A regular file where the dump needs a directory
        val blocker = project.projectDir.toPath() / "blocker"
        blocker.writeText("not a directory")

        val failure = assertFailsWith<GradleException> {
            task.write(blocker / "base.bin") { }
        }
        assertEquals("Failed to write the Kotlin import model dump 'base.bin'", failure.message)
    }
}
