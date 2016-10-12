/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.tasks.kapt

import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.io.File

internal fun generateKotlinAptAnnotation(outputDirectory: File): File {
    val packageName = "__gen"
    val className = "KotlinAptAnnotation"
    val classFqName = "$packageName/$className"

    val bytes = with (ClassWriter(0)) {
        visit(49, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE + ACC_ANNOTATION, classFqName,
                null, null, arrayOf("java/lang/annotation/Annotation"))
        visitSource(null, null)
        visitEnd()
        toByteArray()
    }

    val injectPackage = File(outputDirectory, packageName)
    injectPackage.mkdirs()
    val outputFile = File(injectPackage, "$className.class")
    outputFile.writeBytes(bytes)

    return outputFile
}

internal fun generateAnnotationProcessorWrapper(
        processorFqName: String,
        packageName: String,
        outputDirectory: File,
        className: String,
        taskQualifier: String
): File {
    val classFqName = "$packageName/$className"

    val bytes = with (ClassWriter(0)) {
        val superClass = "org/jetbrains/kotlin/annotation/AnnotationProcessorWrapper"

        visit(49, ACC_PUBLIC + ACC_SUPER, classFqName, null,
                superClass, null)

        visitSource(null, null)

        with (visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)) {
            visitVarInsn(ALOAD, 0)
            visitLdcInsn(processorFqName)
            visitLdcInsn(taskQualifier)
            visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", false)
            visitInsn(RETURN)
            visitMaxs(3 /*max stack*/, 1 /*max locals*/)
            visitEnd()
        }

        visitEnd()
        toByteArray()
    }
    val outputFile = File(outputDirectory, "$className.class")
    outputFile.writeBytes(bytes)

    return outputFile
}