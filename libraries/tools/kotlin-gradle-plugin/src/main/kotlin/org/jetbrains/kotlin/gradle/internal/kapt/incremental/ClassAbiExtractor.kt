/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.jetbrains.org.objectweb.asm.*

const val metadataDescriptor: String = "Lkotlin/Metadata;"

/**
 * Use this to get the ASM version, as otherwise value gets inlined which may cause runtime issues if
 * another plugin adds ASM to the classpath. See https://youtrack.jetbrains.com/issue/KT-31291 for more details.
 */
internal val lazyAsmApiVersion = lazy {
    try {
        val field = Opcodes::class.java.getField("API_VERSION")
        field.get(null) as Int
    } catch(e: Throwable) {
        Opcodes.API_VERSION
    }
}

class ClassAbiExtractor(private val writer: ClassWriter) : ClassVisitor(lazyAsmApiVersion.value, writer) {

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