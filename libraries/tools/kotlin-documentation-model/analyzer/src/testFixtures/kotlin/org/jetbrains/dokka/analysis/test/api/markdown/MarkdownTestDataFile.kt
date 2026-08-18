/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.markdown

import org.jetbrains.dokka.analysis.test.api.TestDataFile

class MarkdownTestDataFile(
    pathFromProjectRoot: String
) : TestDataFile(pathFromProjectRoot = pathFromProjectRoot) {

    private var fileContents = ""

    operator fun String.unaryPlus() {
        fileContents += (this.trimIndent() + System.lineSeparator())
    }

    override fun getContents(): String {
        return fileContents
    }

    override fun toString(): String {
        return "MarkdownTestDataFile(pathFromProjectRoot='$pathFromProjectRoot', fileContents='$fileContents')"
    }
}
