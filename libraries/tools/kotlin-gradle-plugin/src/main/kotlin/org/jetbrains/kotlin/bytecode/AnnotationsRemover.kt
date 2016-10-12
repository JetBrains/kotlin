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

package org.jetbrains.kotlin.bytecode

import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*

internal class AnnotationsRemover(annotations: Iterable<String>) {
    private val annotations = annotations.mapTo(HashSet()) { "L$it;" }

    fun transformClassFile(inputFile: File, outputFile: File) {
        assert(inputFile.extension.toLowerCase() == "class") { "Expected class file: $inputFile" }
        val bytes = inputFile.readBytes()
        val reader = ClassReader(bytes)
        val classWriter = ClassWriter(0)
        val visitor = ClassAnnotationRemover(classWriter)
        reader.accept(visitor, 0)
        outputFile.writeBytes(classWriter.toByteArray())
    }

    inner class ClassAnnotationRemover(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM5, classVisitor) {
        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? =
                checkAnnotation(desc) { super.visitAnnotation(desc, visible) }

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? =
                checkAnnotation(desc) { super.visitTypeAnnotation(typeRef, typePath, desc, visible) }

        override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            val methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
            return MethodAnnotationRemover(methodVisitor)
        }

        override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor {
            val fieldVisitor = super.visitField(access, name, desc, signature, value)
            return FieldAnnotationRemover(fieldVisitor)
        }
    }

    inner class MethodAnnotationRemover(methodVisitor: MethodVisitor) : MethodVisitor(Opcodes.ASM5, methodVisitor) {
        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? =
                checkAnnotation(desc) { super.visitAnnotation(desc, visible) }

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? =
                checkAnnotation(desc) { super.visitTypeAnnotation(typeRef, typePath, desc, visible) }

        override fun visitParameterAnnotation(parameter: Int, desc: String?, visible: Boolean): AnnotationVisitor? =
                checkAnnotation(desc) { super.visitParameterAnnotation(parameter, desc, visible) }
    }

    inner class FieldAnnotationRemover(fieldVisitor: FieldVisitor) : FieldVisitor(Opcodes.ASM5, fieldVisitor) {
        override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? =
                checkAnnotation(desc) { super.visitAnnotation(desc, visible) }

        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, desc: String?, visible: Boolean): AnnotationVisitor? =
                checkAnnotation(desc) { super.visitTypeAnnotation(typeRef, typePath, desc, visible) }
    }

    private inline fun checkAnnotation(desc: String?, default: () -> AnnotationVisitor?): AnnotationVisitor? {
        if (desc in annotations) return null

        return default()
    }
}
