/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

class TempFiles(name: String) {
    private val tempRootDir = System.getProperty("kotlin.native.interop.stubgenerator.temp") ?: System.getProperty("java.io.tmpdir") ?: "."

    val directory: File = File(tempRootDir, name).canonicalFile.also {
        it.mkdirs()
    }

    fun file(relativePath: String, contents: String): File = File(directory, relativePath).canonicalFile.apply {
        parentFile.mkdirs()
        writeText(contents)
    }
}

class TestFilesFactory : TestWatcher() {
    private lateinit var description: Description
    private var count = 0 // Just in case there are multiple calls of `tempFiles` in a test.

    fun tempFiles(): TempFiles = TempFiles("${description.className}/${description.methodName}/${count++}")

    override fun starting(description: Description) {
        this.description = description
        this.count = 0
        super.starting(description)
    }
}
