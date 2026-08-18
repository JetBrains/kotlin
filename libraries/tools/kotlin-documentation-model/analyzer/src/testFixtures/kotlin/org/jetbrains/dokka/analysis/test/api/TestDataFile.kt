/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api

import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * Represents a single file of a project's [TestData].
 *
 * This file will be materialized and created physically before running Dokka,
 * and then passed as one of the input files into it.
 *
 * @property pathFromProjectRoot this file's path from the root of the project. Must begin
 *                               with `/` to not confuse it with relative paths.
 */
@AnalysisTestDslMarker
abstract class TestDataFile(val pathFromProjectRoot: String) {

    init {
        require(pathFromProjectRoot.startsWith("/")) {
            "File path going from the project's root must begin with \"/\" to not confuse it with relative paths."
        }
    }

    /**
     * Returns the string contents of this file.
     *
     * The contents must be complete, as if the user themselves wrote it. For Kotlin files,
     * it should return Kotlin source code (including the package and all import statements).
     * For `.md` files, it should return valid Markdown documentation.
     *
     * These contents will be used to populate the real input file to be used by Dokka.
     */
    abstract fun getContents(): String
}
