/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import org.jetbrains.kotlin.codegen.AbstractLineNumberTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File

abstract class AbstractIrLineNumberTest : AbstractLineNumberTest() {
    override fun compareCustom(psiFile: KtFile, wholeFile: File) {
        val fileText = psiFile.text
        val expectedLineNumbers = normalize(
            fileText.substring(Regex("// \\d+").find(fileText)!!.range.start + 2)
                .trim().split(" ").map { it.trim() }.toMutableList()
        )
        val actualLineNumbers = normalize(extractActualLineNumbersFromBytecode(classFileFactory, false))
        KtUsefulTestCase.assertSameElements(actualLineNumbers, expectedLineNumbers)
    }

    override fun readAllLineNumbers(reader: ClassReader) =
        normalize(super.readAllLineNumbers(reader))

    override fun extractSelectedLineNumbersFromSource(file: KtFile) =
        normalize(super.extractSelectedLineNumbersFromSource(file))

    override fun getTestFunLineNumbersMethodVisitor(
        labels: ArrayList<Label>,
        labels2LineNumbers: java.util.HashMap<Label, String>
    ): MethodVisitor {
        return object : MethodVisitor(Opcodes.API_VERSION) {
            private var lastLabel: Label? = null
            private var lastLine = -1

            override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                if (LINE_NUMBER_FUN == name) {
                    labels.add(lastLabel ?: error("A function call with no preceding label"))
                }
            }

            override fun visitLabel(label: Label) {
                if (lastLabel != null && !labels2LineNumbers.containsKey(lastLabel!!) && lastLine >= 0) {
                    labels2LineNumbers[lastLabel!!] = lastLine.toString() // Inherited line number
                }
                lastLabel = label
            }

            override fun visitLineNumber(line: Int, start: Label) {
                labels2LineNumbers[start] = line.toString()
                lastLine = line
            }
        }
    }

    override fun readTestFunLineNumbers(cr: ClassReader) =
        normalize(super.readTestFunLineNumbers(cr))

    private fun normalize(numbers: List<String>) =
        numbers
            .map { if (it.startsWith('+')) it.substring(1) else it }
            .toSet()
            .toMutableList()
            .sortedBy { it.toInt() }
            .toList()

    override fun getBackend(): TargetBackend {
        return TargetBackend.JVM_IR
    }
}
