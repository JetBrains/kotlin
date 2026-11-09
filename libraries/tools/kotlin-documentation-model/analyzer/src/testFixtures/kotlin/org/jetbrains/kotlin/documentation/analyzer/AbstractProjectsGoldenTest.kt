/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

abstract class AbstractProjectsGoldenTest : AbstractKdmGoldenTest() {
    @Suppress("unused") // Called by the generated Java tests.
    fun runTest(testDataFilePath: String) {
        val file = testDataFile(testDataFilePath)
        val bundle = generateDocumentationBundleFromTestData(file)
        assertMatchesGoldenFile(bundle, file)
        assertMatchesGoldenTextFile(bundle.shortTreeView(), file, ".tree.txt")
        assertMatchesGoldenTextFile(bundle.declarationSignaturesByFragment(), file, ".signatures.txt")
    }
}
