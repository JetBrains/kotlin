/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import java.io.File

interface FirLegacyUastTypesTestBase : FirUastTypesTestBase {
    override fun getTestMetadataFileFromPath(filePath: String, ext: String): File {
        // We're using test files from .../uast-kotlin/testData/...
        // but want to store metadata under .../uast-kotlin-fir/testData/legacyTypes/...
        val revisedFilePath =
            filePath.replace("uast-kotlin", "uast-kotlin-fir").replace("testData", "testData/legacyTypes")
        return super.getTestMetadataFileFromPath(revisedFilePath, ext)
    }
}
