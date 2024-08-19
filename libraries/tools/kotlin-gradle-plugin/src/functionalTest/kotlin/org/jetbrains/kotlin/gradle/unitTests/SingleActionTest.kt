/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.SingleActionPerBuild
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class SingleActionTest {

    private val projectWithSubproject = buildProjectWithJvm {}
    private val subProjectA = buildProject(
        projectBuilder = {
            withName("subprojectA")
            withParent(projectWithSubproject)
        },
    )

    @Test
    fun onlySingleActionPerProjectIsTriggered() {
        val counter = AtomicInteger(0)

        repeat(10) {
            SingleActionPerProject.run(subProjectA, "test-1") {
                counter.incrementAndGet()
            }
        }

        projectWithSubproject.evaluate()

        assertEquals(1, counter.get())
    }

    @Test
    fun onlySingleActionPerRootProjectIsTriggered() {
        val counter = AtomicInteger(0)

        repeat(10) {
            SingleActionPerProject.run(subProjectA.rootProject, "test-2") {
                counter.incrementAndGet()
            }
        }

        projectWithSubproject.evaluate()

        assertEquals(1, counter.get())
    }

    @Test
    fun actionsForDifferentProjectsAreTriggered() {
        val counterRoot = AtomicInteger(0)
        val counterSubProject = AtomicInteger(0)

        SingleActionPerProject.run(subProjectA, "test-3") {
            counterSubProject.incrementAndGet()
        }

        SingleActionPerProject.run(projectWithSubproject, "test-3") {
            counterRoot.incrementAndGet()
        }

        projectWithSubproject.evaluate()

        assertEquals(1, counterRoot.get())
        assertEquals(1, counterSubProject.get())
    }

    @Test
    fun onlySingleActionPerBuildIsTriggered() {
        val counter = AtomicInteger(0)

        repeat(10) {
            SingleActionPerBuild.run(subProjectA, "test-4") {
                counter.incrementAndGet()
            }
        }

        repeat(10) {
            SingleActionPerBuild.run(projectWithSubproject, "test-4") {
                counter.incrementAndGet()
            }
        }

        projectWithSubproject.evaluate()

        assertEquals(1, counter.get())
    }
}