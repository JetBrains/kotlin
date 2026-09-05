/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api

import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * Represents some sort of data of a [TestProject], which normally consists of a number of [TestDataFile].
 *
 * This can be anything that can usually be found in a user-defined project:
 * programming language source code, markdown files with documentation, samples, etc.
 *
 * This virtual test data will be materialized and created physically before running Dokka,
 * and then passed as input files into it.
 */
@AnalysisTestDslMarker
interface TestData {
    fun getFiles(): List<TestDataFile>
}
