/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.junit.Test
import java.io.File


class BuildSessionLoggerTest {
    @Test
    fun testListProfileFilesForFile() {
        val file = File.createTempFile("files", "txt")
        BuildSessionLogger.listProfileFiles(file)
    }

    @Test
    fun testListProfileFilesForNotExistedFile() {
        BuildSessionLogger.listProfileFiles(File("does_not_exist"))
    }
}