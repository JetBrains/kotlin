/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.ide.IdeMultiplatformImportAction
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class IdeMultiplatformImportActionTest {

    @Test
    fun `test - action is not launched when not in idea sync`() {
        val project = buildProjectWithMPP()
        /* Register arbitrary target */
        project.multiplatformExtension.jvm()

        IdeMultiplatformImportAction.extensionPoint.register(project) {
            fail("This action is not expected to be executed")
        }

        project.evaluate()
    }

    @Test
    fun `test - action is launched in idea sync`() = runInIdeSyncMode {
        System.setProperty("idea.sync.active", "true")

        val project = buildProject {
            applyMultiplatformPlugin()
        }

        /* Register arbitrary target */
        project.multiplatformExtension.jvm()

        val executedAction1 = AtomicBoolean(false)
        val executedAction2 = AtomicBoolean(false)

        IdeMultiplatformImportAction.extensionPoint.register(project) {
            assertFalse(executedAction1.getAndSet(true))
        }

        IdeMultiplatformImportAction.extensionPoint.register(project) {
            assertFalse(executedAction2.getAndSet(true))
        }

        project.evaluate()
        assertTrue(executedAction1.get(), "Expected action 1 to be executed")
        assertTrue(executedAction2.get(), "Expected action 2 to be executed")
    }

    /**
     * Will swap out the System property 'idea.sync.active' to emulate IDE sync.
     * This method will enter the 'System properties monitor' to block any other thread from reading
     * System properties while this [block] is executing
     */
    private fun runInIdeSyncMode(block: () -> Unit) = synchronized(System.getProperties()) {
        val isIdeaSyncActiveKey = "idea.sync.active"
        val previousValue = System.getProperty(isIdeaSyncActiveKey)
        try {
            System.setProperty(isIdeaSyncActiveKey, "true")
            block()
        } finally {
            if (previousValue != null) {
                System.setProperty(isIdeaSyncActiveKey, previousValue)
            } else {
                System.clearProperty(isIdeaSyncActiveKey)
            }
        }
    }
}