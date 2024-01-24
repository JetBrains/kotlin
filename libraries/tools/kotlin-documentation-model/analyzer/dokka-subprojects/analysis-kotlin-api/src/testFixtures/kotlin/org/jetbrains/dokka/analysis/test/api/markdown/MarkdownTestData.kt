/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api.markdown

import org.jetbrains.dokka.analysis.test.api.TestData
import org.jetbrains.dokka.analysis.test.api.TestDataFile

/**
 * A container for populating and holding Markdown test data.
 *
 * This container exists so that common creation, population and verification logic
 * can be reused, instead of having to implement [MdFileCreator] multiple times.
 *
 * @see TestData
 */
class MarkdownTestData : TestData, MdFileCreator {

    private val files = mutableListOf<TestDataFile>()

    override fun mdFile(
        pathFromProjectRoot: String,
        fillFile: MarkdownTestDataFile.() -> Unit
    ) {
        check(pathFromProjectRoot.endsWith(".md")) { "Markdown files are expected to have .md extension" }

        val testDataFile = MarkdownTestDataFile(pathFromProjectRoot)
        fillFile(testDataFile)
        files.add(testDataFile)
    }

    override fun getFiles(): List<TestDataFile> {
        return files
    }

    override fun toString(): String {
        return "MarkdownTestData(files=$files)"
    }
}
