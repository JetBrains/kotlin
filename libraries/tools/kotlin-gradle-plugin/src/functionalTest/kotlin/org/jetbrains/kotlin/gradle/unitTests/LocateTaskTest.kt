/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.DefaultTask
import org.gradle.api.UnknownTaskException
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LocateTaskTest {
    open class MyTask : DefaultTask()
    open class MyOtherTask : DefaultTask()

    private val project = buildProject()

    @Test
    fun `test - no such task with name - returns null`() {
        assertNull(project.locateTask<MyTask>("missing"))
    }

    @Test
    fun `test - task previously registered task`() {
        val registered = project.tasks.register("myTask", MyTask::class.java)
        assertEquals(registered, project.locateTask<MyTask>("myTask"))
        assertNull(project.locateTask<MyTask>("missing"))
    }

    @Test
    fun `test - task with different type registered`() {
        project.tasks.register("myTask", MyTask::class.java)
        assertFailsWith<UnknownTaskException> { project.locateTask<MyOtherTask>("myTask") }
    }
}