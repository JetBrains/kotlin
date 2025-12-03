/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import java.io.File

internal class IntegrationTempFiles(name: String) {
    private val tempRootDir = System.getProperty("java.io.tmpdir")

    val directory: File = File(tempRootDir, name).canonicalFile.also {
        it.mkdirs()
    }

    fun file(relativePath: String, contents: String): File = File(directory, relativePath).canonicalFile.apply {
        parentFile.mkdirs()
        writeText(contents)
    }
}