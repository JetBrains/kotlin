/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File
import kotlin.math.absoluteValue

private const val TEST_ANNOTATION_DESC = "Lorg/junit/Test;"

class TestClassScanningContext(
    private val classpath: List<File>,
) {
    private val classNodeCache = hashMapOf<String, ClassNode?>()

    fun findClassNode(className: String): ClassNode? {
        return classNodeCache.getOrPut(className) {
            val classFile = classpath.firstNotNullOfOrNull { root ->
                val targetFile = root.resolve("$className.class")
                if (root.isDirectory && targetFile.isFile) return@firstNotNullOfOrNull targetFile
                null
            } ?: return@getOrPut null

            val classNode = ClassNode()
            val classReader = ClassReader(classFile.readBytes())
            classReader.accept(classNode, ClassReader.SKIP_DEBUG and ClassReader.SKIP_CODE)
            classNode
        }
    }
}

internal fun TestClassScanningContext.isTestClassExcluded(file: File, currentBatch: Int, totalBatches: Int): Boolean {
    if (file.extension != "class") return false
    val className = file.invariantSeparatorsPath.removeSuffix(file.extension)
    val classNode = findClassNode(className) ?: return false

    /* No need to add something to the exclude list if it does not contain any tests */
    if (containsTests(classNode)) {
        return false
    }

    classNode.innerClasses.forEach { innerClass ->
        val innerClass = findClassNode(innerClass.name) ?: error("Cannot resolve inner class ${innerClass.name} for class $className")

        /* We cannot exclude a class which contains inner class with tests */
        if (containsTests(innerClass)) {
            return false
        }
    }

    val testClassBatch = (classNode.signature.hashCode().absoluteValue % totalBatches) + 1
    return testClassBatch != currentBatch
}

internal fun TestClassScanningContext.containsTests(classNode: ClassNode): Boolean {
    if (classNode.methods.any { methodNode ->
            methodNode.visibleAnnotations.any { annotation -> annotation.desc == TEST_ANNOTATION_DESC }
        }) return true

    val superClass = classNode.superName
    val superClassNode = findClassNode(superClass) ?: error("Cannot resolve super class $superClass for class ${classNode.name}")
    return containsTests(superClassNode)
}
