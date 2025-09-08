/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices

class TestUtilsSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        const val COMMON_SOURCE_PATH = "testUtils.kt"
    }

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile> {
        return buildList {
            add(this::class.java.classLoader.getResource(COMMON_SOURCE_PATH)!!.toTestFile())

            if (DataFrameDirectives.WITH_SCHEMA_READER in module.directives) {
                add(this::class.java.classLoader.getResource("schemaReaderDeclaration.kt")!!.toTestFile())
                add(this::class.java.classLoader.getResource("dataSchemaSourceDeclaration.kt")!!.toTestFile())
            }
        }
    }
}