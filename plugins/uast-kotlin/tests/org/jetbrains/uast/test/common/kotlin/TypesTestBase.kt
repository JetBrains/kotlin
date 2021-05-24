/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.env.kotlin.assertEqualsToFile
import java.io.File

interface TypesTestBase {
    fun getTypesFile(testName: String): File

    fun check(testName: String, file: UFile) {
        val valuesFile = getTypesFile(testName)

        assertEqualsToFile("Log values", valuesFile, file.asLogTypes())
    }
}
