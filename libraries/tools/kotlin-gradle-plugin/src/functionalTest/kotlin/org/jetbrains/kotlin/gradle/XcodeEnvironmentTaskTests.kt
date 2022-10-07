/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.konan.target.HostManager
import kotlin.test.Test

class XcodeEnvironmentTaskTests {
    private val project = ProjectBuilder.builder().build() as ProjectInternal

    init {
        project.applyMultiplatformPlugin().apply {
            iosX64().binaries.framework()
            linuxX64().binaries.sharedLib()
        }
        project.evaluate()
    }

    @Test
    fun `test XcodeVersion task registered`() {
        if (HostManager.hostIsMac) {
            val xcodeVersionTask = project.tasks.getByName("getXcodeVersion")
            project.tasks.getByName("linkDebugFrameworkIosX64").assertDependsOn(xcodeVersionTask)
            project.tasks.getByName("linkDebugSharedLinuxX64").assertNotDependsOn(xcodeVersionTask)
        } else {
            project.assertContainsNoTaskWithName("getXcodeVersion")
        }
    }

    @Test
    fun `test XcodeSimulators task registered`() {
        if (HostManager.hostIsMac) {
            val xcodeSimulatorsTask = project.tasks.getByName("getXcodeSimulators")
            project.tasks.getByName("iosX64Test").assertDependsOn(xcodeSimulatorsTask)
            project.tasks.getByName("linuxX64Test").assertNotDependsOn(xcodeSimulatorsTask)
        } else {
            project.assertContainsNoTaskWithName("getXcodeSimulators")
        }
    }
}