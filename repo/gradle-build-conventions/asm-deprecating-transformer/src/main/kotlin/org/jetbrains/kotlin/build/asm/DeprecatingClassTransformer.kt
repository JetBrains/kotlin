/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.asm

import kotlinx.metadata.hasAnnotations
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * ASM JVM class transformer that marks each visited class as @kotlin.Deprecated with the specified [deprecationMessage]
 * Context: https://youtrack.jetbrains.com/issue/KT-70251
 */
class DeprecatingClassTransformer(
    cv: ClassWriter,
    private val deprecationMessage: String,
    private val processedClassCallback: (className: String) -> Unit = {},
) : ClassVisitor(Opcodes.API_VERSION, cv) {
    private var isAlreadyDeprecated = false

    override fun visit(
        version: Int, access: Int, name: String, signature: String?, superName: String?,
        interfaces: Array<out String>?,
    ) {
        processedClassCallback(name.replace('/', '.'))
        val deprecatedAccess = access or Opcodes.ACC_DEPRECATED
        super.visit(version, deprecatedAccess, name, signature, superName, interfaces)
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        when (desc) {
            DEPRECATED_ANNOTATION_DESC -> isAlreadyDeprecated = true
            METADATA_ANNOTATION_DESC -> return MetadataAnnotationVisitor()
        }
        return super.visitAnnotation(desc, visible)
    }

    override fun visitEnd() {
        super.visitEnd()
        if (!isAlreadyDeprecated) {
            // don't deprecate and don't override the message if the class is already deprecated
            val deprecatedAnnotation = super.visitAnnotation(DEPRECATED_ANNOTATION_DESC, true)
            deprecatedAnnotation.visit("message", deprecationMessage)
            deprecatedAnnotation.visitEnd()
        }
    }

    /**
     * Reads the existing [kotlin.Metadata] and overrides it.
     * Does not define the super annotation visitor and does not contain calls to `super`,
     * because we don't need to write the existing annotation as is.
     *
     * The kotlin.Metadata of the supported versions won't change and has only primitive and array values,
     * thus we override only [visit] and [visitArray].
     */
    private inner class MetadataAnnotationVisitor : AnnotationVisitor(Opcodes.API_VERSION) {
        private val values = mutableMapOf<String, Any>()

        override fun visit(name: String, value: Any?) {
            if (value != null) {
                values[name] = value
            }
        }

        override fun visitArray(name: String): AnnotationVisitor {
            return object : AnnotationVisitor(Opcodes.API_VERSION) {
                private val list = mutableListOf<String>()

                override fun visit(name: String?, value: Any?) {
                    if (value != null) {
                        list.add(value as String)
                    }
                }

                override fun visitEnd() {
                    values[name] = list.toTypedArray()
                }
            }
        }

        private fun writeAnnotation(metadata: Metadata) {
            cv.visitAnnotation(METADATA_ANNOTATION_DESC, true).apply {
                visit("k", metadata.kind)
                visitArray("mv").apply {
                    metadata.metadataVersion.forEach { visit(null, it) }
                    visitEnd()
                }
                visitArray("d1").apply {
                    metadata.data1.forEach { visit(null, it) }
                    visitEnd()
                }
                visitArray("d2").apply {
                    metadata.data2.forEach { visit(null, it) }
                    visitEnd()
                }
                if (metadata.extraString.isNotEmpty()) {
                    visit("xs", metadata.extraString)
                }
                if (metadata.packageName.isNotEmpty()) {
                    visit("pn", metadata.packageName)
                }
                visit("xi", metadata.extraInt)
                visitEnd()
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun visitEnd() {
            val header = Metadata(
                kind = values["k"] as Int? ?: 1,
                metadataVersion = values["mv"] as IntArray? ?: intArrayOf(),
                data1 = values["d1"] as Array<String>? ?: emptyArray(),
                data2 = values["d2"] as Array<String>? ?: emptyArray(),
                extraString = values["xs"] as String? ?: "",
                packageName = values["pn"] as String? ?: "",
                extraInt = values["xi"] as Int? ?: 0
            )

            val metadata = KotlinClassMetadata.readStrict(header)
            if (metadata is KotlinClassMetadata.Class) {
                // we cover only regular classes/interfaces currently and not file facades or other types of classes
                // top-level symbols are hidden by removing .kotlin_module files in `KotlinModuleMetadataVersionBasedSkippingTransformer`
                val kClass = metadata.kmClass
                kClass.hasAnnotations = true
            }

            writeAnnotation(metadata.write())
        }
    }

    companion object {
        private const val DEPRECATED_ANNOTATION_DESC = "Lkotlin/Deprecated;"
        private const val METADATA_ANNOTATION_DESC = "Lkotlin/Metadata;"
    }
}