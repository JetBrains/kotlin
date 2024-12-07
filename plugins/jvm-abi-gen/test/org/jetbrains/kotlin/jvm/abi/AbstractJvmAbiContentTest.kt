/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.codegen.BytecodeListingTextCollectingVisitor
import org.jetbrains.kotlin.incremental.isClassFile
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.org.objectweb.asm.ClassReader
import java.io.File

abstract class AbstractJvmAbiContentTest : BaseJvmAbiTest() {
    fun doTest(path: String) {
        val testDir = File(path)
        val compilation = Compilation(testDir, name = null).also { make(it) }

        val classToBytecode = hashMapOf<File, String>()
        val baseDir = compilation.abiDir
        val classFiles = baseDir.walk().filter { it.isFile && it.isClassFile() }
        for (classFile in classFiles) {
            val bytes = classFile.readBytes()
            val reader = ClassReader(bytes)
            val visitor = BytecodeListingTextCollectingVisitor(
                filter = BytecodeListingTextCollectingVisitor.Filter.EMPTY,
                allClasses = emptyMap(),
                withSignatures = false,
                sortDeclarations = false, // Declaration order matters for the ABI
            )
            reader.accept(visitor, 0)
            classToBytecode[classFile] = visitor.text
        }

        val actual = classToBytecode.entries
            .sortedBy { it.key.relativeTo(baseDir).invariantSeparatorsPath }
            .joinToString("\n") { it.value }
        val signaturesFile = testDir.resolve("signatures.txt")
        if (!signaturesFile.exists()) {
            signaturesFile.writeText("")
        }
        KtUsefulTestCase.assertSameLinesWithFile(signaturesFile.canonicalPath, actual)
    }
}
