/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.services

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class ParcelizeUtilSourcesProvider(testServices: TestServices, baseDir: String = ".") : AdditionalSourceProvider(testServices) {
    private val libraryPath = "$baseDir/plugins/parcelize/parcelize-compiler/testData/boxLib.kt"

    @OptIn(ExperimentalStdlibApi::class)
    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        return listOf(File(libraryPath).toTestFile())
    }
}
