/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests.integration.utils

import org.jetbrains.kotlin.backend.konan.tests.integration.GenerateObjCExportIntegrationTestData
import java.io.File

internal object IntegrationTestFiles {

    val integrationDir = File(
        System.getProperty(outputsSystemProperty)?.let(::File) ?: error("Missing `$outputsSystemProperty` system property"),
        "integrationTestFiles"
    ).apply { mkdirs() }

    fun getHeaders(headersHandler: (name: String, k1Header: String, k2Header: String) -> Unit) {
        val generatorTestName = GenerateObjCExportIntegrationTestData::class.simpleName
        if (integrationDir.listFiles().isEmpty()) {
            error("No integration test files found in ${integrationDir.absolutePath}. Run `$generatorTestName`")
        }
        integrationDir.listFiles()?.forEach { file ->
            val libName = file.name
            headersHandler(
                libName,
                File(integrationDir, libName).listFiles()?.firstOrNull { h -> h.name == "k1.h" }?.readText()
                    ?: error("No K1 header file for $libName. Run $generatorTestName"),
                File(integrationDir, libName).listFiles()?.firstOrNull { it.name == "k2.h" }?.readText()
                    ?: error("No K2 header file for $libName. Run $generatorTestName"),
            )
        }
    }

    fun storeHeader(name: String, content: String) {
        val testTag = System.getProperty("testDisplayName.tag")
        val isK1 = testTag == "K1"
        val isK2 = testTag == "AA"
        val fileName = if (isK1) "k1.h" else if (isK2) "k2.h" else error("Unknown test tag: `$testTag`")

        File(File(integrationDir, name).apply { mkdirs() }, fileName)
            .writeText(content)
    }
}

internal class IntegrationTempFiles(name: String) {
    private val tempRootDir = System.getProperty(outputsSystemProperty) ?: System.getProperty("java.io.tmpdir") ?: "."

    val directory: File = File(tempRootDir, name).canonicalFile.also {
        it.mkdirs()
    }

    fun file(relativePath: String, contents: String): File = File(directory, relativePath).canonicalFile.apply {
        parentFile.mkdirs()
        writeText(contents)
    }
}

/**
 * Reference to layout.buildDirectory
 */
private const val outputsSystemProperty = "integration-test-outputs"