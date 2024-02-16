/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm

import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.metadata.jvm.UnstableMetadataApi
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream

fun ClassReader.readKotlinClassHeader(): Metadata? {
    var header: Metadata? = null

    try {
        val metadataDesc = Type.getDescriptor(Metadata::class.java)
        accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
                if (desc == metadataDesc) readMetadataVisitor { header = it }
                else null
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
    } catch (e: Exception) {
        return null
    }

    return header
}

private fun readMetadataVisitor(output: (Metadata) -> Unit): AnnotationVisitor =
    object : AnnotationVisitor(Opcodes.API_VERSION) {
        var kind: Int? = null
        var metadataVersion: IntArray? = null
        var data1: Array<String>? = null
        var data2: Array<String>? = null
        var extraString: String? = null
        var packageName: String? = null
        var extraInt: Int? = null

        override fun visit(name: String?, value: Any?) {
            when (name) {
                "k" -> kind = value as? Int
                "mv" -> metadataVersion = value as? IntArray
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
            return object : AnnotationVisitor(Opcodes.API_VERSION) {
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
            output(Metadata(kind, metadataVersion, data1, data2, extraString, packageName, extraInt))
        }
    }

internal fun readClassFile(file: File): Metadata {
    return ClassReader(FileInputStream(file)).readKotlinClassHeader() ?: throw KotlinpException("file is not a Kotlin class file: $file")
}

internal fun readMetadata(metadata: Metadata): KotlinClassMetadata {
    return try {
        KotlinClassMetadata.readLenient(metadata)
    } catch (e: IllegalArgumentException) {
        throw KotlinpException("inconsistent Kotlin metadata: ${e.message}")
    }
}

@OptIn(UnstableMetadataApi::class)
internal fun readModuleFile(file: File): KotlinModuleMetadata? =
    runCatching { KotlinModuleMetadata.read(file.readBytes()) }.getOrNull()
