/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.checkers.AbstractKotlinHighlightingPassTest
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractCodeFragmentHighlightingTest : AbstractKotlinHighlightingPassTest() {
    override fun doTest(filePath: String) {
        myFixture.configureByCodeFragment(filePath)
        checkHighlighting(filePath)
    }

    fun doTestWithImport(filePath: String) {
        myFixture.configureByCodeFragment(filePath)

        project.executeWriteCommand("Imports insertion") {
            val fileText = FileUtil.loadFile(File(filePath), true)
            val file = myFixture.file as KtFile
            InTextDirectivesUtils.findListWithPrefixes(fileText, "// IMPORT: ").forEach {
                val descriptor = file.resolveImportReference(FqName(it)).singleOrNull()
                    ?: error("Could not resolve descriptor to import: $it")
                ImportInsertHelper.getInstance(project).importDescriptor(file, descriptor)
            }
        }

        checkHighlighting(filePath)
    }

    private fun checkHighlighting(filePath: String) {
        val inspectionName = InTextDirectivesUtils.findStringWithPrefixes(File(filePath).readText(), "// INSPECTION_CLASS: ")
        if (inspectionName != null) {
            val inspection = Class.forName(inspectionName).newInstance() as InspectionProfileEntry
            myFixture.enableInspections(inspection)
            try {
                myFixture.checkHighlighting(true, false, false)
            } finally {
                myFixture.disableInspections(inspection)
            }
            return
        }

        myFixture.checkHighlighting(true, false, false)
    }
}