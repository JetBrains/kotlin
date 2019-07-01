/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.jetbrains.org.objectweb.asm.*

const val metadataDescriptor: String = "Lkotlin/Metadata;"

class ClassAbiExtractor(private val writer: ClassWriter) : ClassVisitor(Opcodes.API_VERSION, writer) {

    override fun visitMethod(
        access: Int,
        name: String?,
        desc: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        return if (access.isAbi()) {
            super.visitMethod(access, name, desc, signature, exceptions)
        } else {
            null
        }
    }

    override fun visitAnnotation(desc: String?, visible: Boolean): AnnotationVisitor? {
        return if (desc != null && desc != metadataDescriptor) {
            super.visitAnnotation(desc, visible)
        } else {
            null
        }
    }

    override fun visitField(access: Int, name: String?, desc: String?, signature: String?, value: Any?): FieldVisitor? {
        return if (access.isAbi()) {
            super.visitField(access, name, desc, signature, value)
        } else {
            null
        }
    }

    override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
        if (access.isAbi() && outerName != null && innerName != null) {
            super.visitInnerClass(name, outerName, innerName, access)
        }
    }

    fun getBytes(): ByteArray = writer.toByteArray()

    private fun Int.isAbi() = (this and Opcodes.ACC_PRIVATE) == 0
}