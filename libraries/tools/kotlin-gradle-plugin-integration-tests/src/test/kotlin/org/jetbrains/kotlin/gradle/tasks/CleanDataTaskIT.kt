/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.transformProjectWithPluginsDsl
import org.junit.Test


class CleanDataTaskIT : BaseGradleIT() {

    @Test
    fun testDownloadedFolderDeletion() {
        val project = transformProjectWithPluginsDsl("cleanTask")

        project.build("testCleanTask") {
            assertSuccessful()
        }

    }
}
