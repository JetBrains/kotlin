/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlinx.metadata.jvm.KotlinClassHeader
import org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream

internal fun File.readKotlinClassHeader(): KotlinClassHeader? {
    var header: KotlinClassHeader? = null

    try {
        val metadataDesc = Type.getDescriptor(Metadata::class.java)
        ClassReader(FileInputStream(this)).accept(object : ClassVisitor(Opcodes.ASM4) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
                if (desc == metadataDesc) readMetadataVisitor { header = it }
                else null
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    } catch (e: Exception) {
        return null
    }

    return header
}

private fun readMetadataVisitor(output: (KotlinClassHeader) -> Unit): AnnotationVisitor =
    object : AnnotationVisitor(Opcodes.ASM4) {
        var kind: Int? = null
        var metadataVersion: IntArray? = null
        var bytecodeVersion: IntArray? = null
        var data1: Array<String>? = null
        var data2: Array<String>? = null
        var extraString: String? = null
        var packageName: String? = null
        var extraInt: Int? = null

        override fun visit(name: String?, value: Any?) {
            when (name) {
                "k" -> kind = value as? Int
                "mv" -> metadataVersion = value as? IntArray
                "bv" -> bytecodeVersion = value as? IntArray
                "xs" -> extraString = value as? String
                "xi" -> extraInt = value as? Int
                "pn" -> packageName = value as? String
            }
        }

        override fun visitArray(name: String?): AnnotationVisitor? =
            when (name) {
                "d1" -> stringArrayVisitor { data1 = it }
                "d2" -> stringArrayVisitor { data2 = it }
                else -> null
            }

        private fun stringArrayVisitor(output: (Array<String>) -> Unit): AnnotationVisitor {
            return object : AnnotationVisitor(Opcodes.ASM4) {
                val strings = mutableListOf<String>()

                override fun visit(name: String?, value: Any?) {
                    (value as? String)?.let(strings::add)
                }

                override fun visitEnd() {
                    output(strings.toTypedArray())
                }
            }
        }

        override fun visitEnd() {
            output(KotlinClassHeader(kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt))
        }
    }
