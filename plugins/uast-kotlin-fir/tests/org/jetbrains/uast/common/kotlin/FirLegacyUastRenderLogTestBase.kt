/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.common.kotlin

import java.io.File

interface FirLegacyUastRenderLogTestBase : FirUastRenderLogTestBase {
    override fun getTestMetadataFileFromPath(filePath: String, ext: String): File {
        // We're using test files from .../uast-kotlin/testData/...
        // but want to store metadata under .../uast-kotlin-fir/testData/legacy/...
        val revisedFilePath =
            filePath.replace("uast-kotlin", "uast-kotlin-fir").replace("testData", "testData/legacy")
        return super.getTestMetadataFileFromPath(revisedFilePath, ext)
    }
}
