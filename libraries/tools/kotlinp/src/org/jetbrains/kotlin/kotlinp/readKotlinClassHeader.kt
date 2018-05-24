/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlinx.metadata.jvm.KotlinClassHeader
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream

internal fun File.readKotlinClassHeader(): KotlinClassHeader? {
    var header: KotlinClassHeader? = null

    try {
        ClassReader(FileInputStream(this)).accept(object : ClassVisitor(Opcodes.ASM4) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
                if (desc == "Lkotlin/Metadata;") readMetadataVisitor { header = it }
                else null
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    } catch (e: Exception) {
        return null
    }

    return header
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
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
                Metadata::k.name -> kind = value as? Int
                Metadata::mv.name -> metadataVersion = value as? IntArray
                Metadata::bv.name -> bytecodeVersion = value as? IntArray
                Metadata::xs.name -> extraString = value as? String
                Metadata::xi.name -> extraInt = value as? Int
                Metadata::pn.name -> packageName = value as? String
            }
        }

        override fun visitArray(name: String?): AnnotationVisitor? =
            when (name) {
                Metadata::d1.name -> stringArrayVisitor { data1 = it }
                Metadata::d2.name -> stringArrayVisitor { data2 = it }
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
