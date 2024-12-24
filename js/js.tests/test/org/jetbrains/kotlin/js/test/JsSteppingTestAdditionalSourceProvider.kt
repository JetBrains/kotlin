/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.ModuleStructureExtractor
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

open class JsSteppingTestAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    protected open val commonTestHelpersFile: String = "$HELPERS_DIR/jsCommonTestHelpers.kt"
    protected open val minimalTestHelpersLocation: String? = "$HELPERS_DIR/jsMinimalTestHelpers.kt"
    protected open val withStdlibTestHelpersFile: String? = "$HELPERS_DIR/jsWithStdlibTestHelpers.kt"

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        // HACK: For some reason if we add the same additional files to each module (not only the main one),
        // we get the 'Symbol already bound' exception during linking.
        // The craziest part is that we run most of the box tests with additional files, where we _do_ add the same additional files
        // to each module (using JsAdditionalSourceProvider), but the linker doesn't complain.
        // For some reason, it doesn't like _specifically_ the symbols defined in compiler/testData/debug/jsTestHelpers.
        return if (module.name == ModuleStructureExtractor.DEFAULT_MODULE_NAME) {
            buildList {
                if (containsDirective(globalDirectives, module, ConfigurationDirectives.WITH_STDLIB))
                    withStdlibTestHelpersFile?.let { add(File(it).toTestFile()) }
                else
                    minimalTestHelpersLocation?.let { add(File(it).toTestFile()) }
                add(File(commonTestHelpersFile).toTestFile())
            }
        } else
            emptyList()
    }

    companion object {
        private const val HELPERS_DIR = "compiler/testData/debug/jsTestHelpers"
    }
}
