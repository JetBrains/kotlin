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

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.Printer
import java.io.File

private val LINE_SEPARATOR = System.getProperty("line.separator")

abstract class AbstractAsmLikeInstructionListingTest : CodegenTestCase() {
    private companion object {
        val CURIOUS_ABOUT_DIRECTIVE = "// CURIOUS_ABOUT "
        val LOCAL_VARIABLES_TABLE_DIRECTIVE = "// LOCAL_VARIABLES_TABLE"
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        compile(files, javaFilesDir)

        val classes = classFileFactory
                .getClassFiles()
                .sortedBy { it.relativePath }
                .map { file -> ClassNode().also { ClassReader(file.asByteArray()).accept(it, ClassReader.EXPAND_FRAMES) } }

        val testFileLines = wholeFile.readLines()

        val printBytecodeForTheseMethods = testFileLines
                .filter { it.startsWith(CURIOUS_ABOUT_DIRECTIVE) }
                .map { it.substring(CURIOUS_ABOUT_DIRECTIVE.length) }
                .flatMap { it.split(',').map { it.trim() } }

        val showLocalVariables = testFileLines.any { it.trim() == LOCAL_VARIABLES_TABLE_DIRECTIVE }

        KotlinTestUtils.assertEqualsToFile(txtFile, classes.joinToString(LINE_SEPARATOR.repeat(2)) {
            renderClassNode(it, printBytecodeForTheseMethods, showLocalVariables)
        })
    }

    private fun renderClassNode(clazz: ClassNode, printBytecodeForTheseMethods: List<String>, showLocalVariables: Boolean): String {
        val fields = (clazz.fields ?: emptyList()).sortedBy { it.name }
        val methods = (clazz.methods ?: emptyList()).sortedBy { it.name }

        val superTypes = (listOf(clazz.superName) + clazz.interfaces).filterNotNull()

        return buildString {
            renderVisibilityModifiers(clazz.access)
            renderModalityModifiers(clazz.access)
            append(if ((clazz.access and ACC_INTERFACE) != 0) "interface " else "class ")
            append(clazz.name)

            if (superTypes.isNotEmpty()) {
                append(" : " + superTypes.joinToString())
            }

            appendln(" {")

            fields.joinTo(this, LINE_SEPARATOR.repeat(2)) { renderField(it).withMargin() }

            if (fields.isNotEmpty()) {
                appendln().appendln()
            }

            methods.joinTo(this, LINE_SEPARATOR.repeat(2)) {
                val printBytecode = printBytecodeForTheseMethods.contains(it.name)
                renderMethod(it, printBytecode, showLocalVariables).withMargin()
            }

            appendln().append("}")
        }
    }

    private fun renderField(field: FieldNode) = buildString {
        renderVisibilityModifiers(field.access)
        renderModalityModifiers(field.access)
        append(Type.getType(field.desc).className).append(' ')
        append(field.name)
    }

    private fun renderMethod(method: MethodNode, printBytecode: Boolean, showLocalVariables: Boolean) = buildString {
        renderVisibilityModifiers(method.access)
        renderModalityModifiers(method.access)
        val (returnType, parameterTypes) = with(Type.getMethodType(method.desc)) { returnType to argumentTypes }
        append(returnType.className).append(' ')
        append(method.name)
        parameterTypes.mapIndexed { index, type -> "${type.className} p$index" }.joinTo(this, prefix = "(", postfix = ")")

        if (printBytecode && (method.access and ACC_ABSTRACT) == 0) {
            appendln(" {")
            append(renderBytecodeInstructions(method.instructions).trimEnd().withMargin())

            if (showLocalVariables) {
                val localVariableTable = buildLocalVariableTable(method)
                if (localVariableTable.isNotEmpty()) {
                    appendln().appendln()
                    append(localVariableTable.withMargin())
                }
            }

            appendln().append("}")
        }
    }

    private fun buildLocalVariableTable(method: MethodNode): String {
        val localVariables = method.localVariables?.takeIf { it.isNotEmpty() } ?: return ""
        return buildString {
            append("Local variables:")
            for (variable in localVariables) {
                appendln().append((variable.name + ": " + variable.desc).withMargin())
            }
        }
    }

    private fun renderBytecodeInstructions(instructions: InsnList) = buildString {
        val labelMappings = LabelMappings()

        var currentInsn = instructions.first
        while (currentInsn != null) {
            renderInstruction(currentInsn, labelMappings)
            currentInsn = currentInsn.next
        }
    }

    private fun StringBuilder.renderInstruction(node: AbstractInsnNode, labelMappings: LabelMappings) {
        if (node is LabelNode) {
            appendln("LABEL (L" + labelMappings[node.label] + ")")
            return
        }

        if (node is LineNumberNode) {
            appendln("LINENUMBER (" + node.line + ")")
            return
        }

        if (node is FrameNode) return

        append("  ").append(Printer.OPCODES[node.opcode] ?: error("Invalid opcode ${node.opcode}"))

        when (node) {
            is FieldInsnNode -> append(" (" + node.name + ", " + node.desc + ")")
            is JumpInsnNode -> append(" (L" + labelMappings[node.label.label] + ")")
            is IntInsnNode -> append(" (" + node.operand + ")")
            is MethodInsnNode -> append(" (" + node.owner + ", "+ node.name + ", " + node.desc + ")")
            is VarInsnNode -> append(" (" + node.`var` + ")")
            is LdcInsnNode -> append(" (" + node.cst + ")")
        }

        appendln()
    }

    private fun String.withMargin(margin: String = "    "): String {
        return lineSequence().map { margin + it }.joinToString(LINE_SEPARATOR)
    }

    private fun StringBuilder.renderVisibilityModifiers(access: Int) {
        if ((access and ACC_PUBLIC) != 0) append("public ")
        if ((access and ACC_PRIVATE) != 0) append("private ")
        if ((access and ACC_PROTECTED) != 0) append("protected ")
    }

    private fun StringBuilder.renderModalityModifiers(access: Int) {
        if ((access and ACC_FINAL) != 0) append("final ")
        if ((access and ACC_ABSTRACT) != 0) append("abstract ")
        if ((access and ACC_STATIC) != 0) append("static ")
    }

    private class LabelMappings {
        private var mappings = hashMapOf<Int, Int>()
        private var currentIndex = 0

        operator fun get(label: Label): Int {
            val hashCode = System.identityHashCode(label)
            return mappings.getOrPut(hashCode) { currentIndex++ }
        }
    }
}