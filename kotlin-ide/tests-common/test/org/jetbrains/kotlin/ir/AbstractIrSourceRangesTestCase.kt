/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.util.RenderIrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.Printer
import java.io.File

abstract class AbstractIrSourceRangesTestCase : AbstractIrGeneratorTestCase() {
    override fun doTest(wholeFile: File, testFiles: List<TestFile>) {
        val dir = wholeFile.parentFile
        val testFileToIrFile = generateIrFilesAsSingleModule(testFiles)
        for ((testFile, irFile) in testFileToIrFile) {
            val irFileDump = irFile.dumpWithSourceLocations(irFile.fileEntry)
            val expectedSourceLocations = File(dir, testFile.name.replace(".kt", ".txt"))
            KotlinTestUtils.assertEqualsToFile(expectedSourceLocations, irFileDump)
        }
    }

    private fun IrElement.dumpWithSourceLocations(fileEntry: SourceManager.FileEntry): String =
        StringBuilder().also {
            acceptVoid(DumpSourceLocations(it, fileEntry))
        }.toString()

    private class DumpSourceLocations(
        out: Appendable,
        val fileEntry: SourceManager.FileEntry
    ) : IrElementVisitorVoid {
        val printer = Printer(out, "  ")
        val elementRenderer = RenderIrElementVisitor()

        override fun visitElement(element: IrElement) {
            val sourceRangeInfo = fileEntry.getSourceRangeInfo(element.startOffset, element.endOffset)
            printer.println("@${sourceRangeInfo.render()} ${element.accept(elementRenderer, null)}")
            printer.pushIndent()
            element.acceptChildrenVoid(this)
            printer.popIndent()
        }

        private fun SourceRangeInfo.render() =
            if (startLineNumber == endLineNumber)
                "$startLineNumber:$startColumnNumber..$endColumnNumber"
            else
                "$startLineNumber:$startColumnNumber..$endLineNumber:$endColumnNumber"
    }
}
