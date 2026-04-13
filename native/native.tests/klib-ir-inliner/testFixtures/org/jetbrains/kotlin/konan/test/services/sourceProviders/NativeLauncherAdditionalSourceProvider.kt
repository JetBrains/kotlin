/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.services.sourceProviders

import org.jetbrains.kotlin.konan.test.blackbox.support.util.generateBoxFunctionLauncher
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives.ESCAPE_MODULE_NAME
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.testInfo

private const val LAUNCHER_FILE_NAME = "__launcher__.kt"
private val BOX_FUNCTION_NAME = "box"

class NativeLauncherAdditionalSourceProvider(testServices: TestServices) : MainFunctionForBlackBoxTestsSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        val fileWithBox = module.files.firstOrNull { containsBoxMethod(it.originalContent) } ?: return emptyList()
        var boxFqName = detectPackage(fileWithBox)?.let { "$it.$BOX_FUNCTION_NAME" } ?: BOX_FUNCTION_NAME
        if (ESCAPE_MODULE_NAME in globalDirectives) {
            val additionalPackage = BatchingPackageInserter.computePackage(testServices.testInfo)
            boxFqName = "$additionalPackage.$boxFqName"
        }
        val launcherContent = generateBoxFunctionLauncher(boxFqName)

        val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("launcher")
        val launcherFile = tempDir.resolve(LAUNCHER_FILE_NAME).also {
            it.writeText(launcherContent)
        }
        return listOf(launcherFile.toTestFile())
    }
}
