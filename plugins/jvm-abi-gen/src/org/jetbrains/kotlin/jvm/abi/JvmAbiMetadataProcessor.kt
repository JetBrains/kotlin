/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmPackage
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import kotlinx.metadata.jvm.localDelegatedProperties
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Wrap the visitor for a Kotlin Metadata annotation to strip out private and local
 * functions, properties, and type aliases as well as local delegated properties.
 */
fun abiMetadataProcessor(annotationVisitor: AnnotationVisitor): AnnotationVisitor =
    kotlinClassHeaderVisitor { header ->
        // kotlinx-metadata only supports writing Kotlin metadata of version >= 1.4, so we need to
        // update the metadata version if we encounter older metadata annotations.
        val metadataVersion = header.metadataVersion.takeIf { v ->
            val major = v.getOrNull(0) ?: 0
            val minor = v.getOrNull(1) ?: 0
            major > 1 || major == 1 && minor >= 4
        } ?: intArrayOf(1, 4)

        val newHeader = when (val metadata = KotlinClassMetadata.read(header)) {
            is KotlinClassMetadata.Class -> {
                val klass = metadata.toKmClass()
                klass.removePrivateDeclarations()
                KotlinClassMetadata.writeClass(klass, metadataVersion, header.extraInt).annotationData
            }
            is KotlinClassMetadata.FileFacade -> {
                val pkg = metadata.toKmPackage()
                pkg.removePrivateDeclarations()
                KotlinClassMetadata.writeFileFacade(pkg, metadataVersion, header.extraInt).annotationData
            }
            is KotlinClassMetadata.MultiFileClassPart -> {
                val pkg = metadata.toKmPackage()
                pkg.removePrivateDeclarations()
                KotlinClassMetadata.writeMultiFileClassPart(pkg, metadata.facadeClassName, metadataVersion, header.extraInt).annotationData
            }
            else -> header
        }

        // Write out the stripped annotation
        annotationVisitor.visitKotlinMetadata(newHeader)
    }

/**
 * Parse a KotlinClassHeader from an existing Kotlin Metadata annotation visitor.
 */
private fun kotlinClassHeaderVisitor(body: (Metadata) -> Unit): AnnotationVisitor =
    object : AnnotationVisitor(Opcodes.API_VERSION) {
        var kind: Int = 1
        var metadataVersion: IntArray = intArrayOf()
        var data1: MutableList<String> = mutableListOf()
        var data2: MutableList<String> = mutableListOf()
        var extraString: String? = null
        var packageName: String? = null
        var extraInt: Int = 0

        override fun visit(name: String, value: Any?) {
            when (name) {
                KIND_FIELD_NAME -> kind = value as Int
                METADATA_EXTRA_INT_FIELD_NAME -> extraInt = value as Int
                METADATA_VERSION_FIELD_NAME -> metadataVersion = value as IntArray
                METADATA_EXTRA_STRING_FIELD_NAME -> extraString = value as String
                METADATA_PACKAGE_NAME_FIELD_NAME -> packageName = value as String
            }
        }

        override fun visitArray(name: String): AnnotationVisitor? {
            val destination = when (name) {
                METADATA_DATA_FIELD_NAME -> data1
                METADATA_STRINGS_FIELD_NAME -> data2
                else -> return null
            }
            return object : AnnotationVisitor(Opcodes.API_VERSION) {
                override fun visit(name: String?, value: Any?) {
                    destination += value as String
                }
            }
        }

        override fun visitEnd() {
            body(
                Metadata(
                    kind,
                    metadataVersion,
                    data1.toTypedArray(),
                    data2.toTypedArray(),
                    extraString,
                    packageName,
                    extraInt
                )
            )
        }
    }

/**
 * Serialize a KotlinClassHeader to an existing Kotlin Metadata annotation visitor.
 */
private fun AnnotationVisitor.visitKotlinMetadata(header: Metadata) {
    visit(KIND_FIELD_NAME, header.kind)
    visit(METADATA_VERSION_FIELD_NAME, header.metadataVersion)
    if (header.data1.isNotEmpty()) {
        visitArray(METADATA_DATA_FIELD_NAME).apply {
            header.data1.forEach { visit(null, it) }
            visitEnd()
        }
    }
    if (header.data2.isNotEmpty()) {
        visitArray(METADATA_STRINGS_FIELD_NAME).apply {
            header.data2.forEach { visit(null, it) }
            visitEnd()
        }
    }
    if (header.extraString.isNotEmpty()) {
        visit(METADATA_EXTRA_STRING_FIELD_NAME, header.extraString)
    }
    if (header.packageName.isNotEmpty()) {
        visit(METADATA_PACKAGE_NAME_FIELD_NAME, header.packageName)
    }
    if (header.extraInt != 0) {
        visit(METADATA_EXTRA_INT_FIELD_NAME, header.extraInt)
    }
    visitEnd()
}

private fun KmClass.removePrivateDeclarations() {
    constructors.removeIf { isPrivateDeclaration(it.flags) }
    functions.removeIf { isPrivateDeclaration(it.flags) }
    properties.removeIf { isPrivateDeclaration(it.flags) }
    localDelegatedProperties.clear()
    // TODO: do not serialize private type aliases once KT-17229 is fixed.
}

private fun KmPackage.removePrivateDeclarations() {
    functions.removeIf { isPrivateDeclaration(it.flags) }
    properties.removeIf { isPrivateDeclaration(it.flags) }
    localDelegatedProperties.clear()
    // TODO: do not serialize private type aliases once KT-17229 is fixed.
}

private fun isPrivateDeclaration(flags: Flags): Boolean =
    Flag.IS_PRIVATE(flags) || Flag.IS_PRIVATE_TO_THIS(flags) || Flag.IS_LOCAL(flags)
