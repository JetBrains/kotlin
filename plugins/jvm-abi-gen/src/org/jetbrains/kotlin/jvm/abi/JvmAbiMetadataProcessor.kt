/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import kotlinx.metadata.*
import kotlinx.metadata.jvm.JvmClassExtensionVisitor
import kotlinx.metadata.jvm.JvmPackageExtensionVisitor
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Wrap the visitor for a Kotlin Metadata annotation to strip out private and local
 * functions, properties, and type aliases as well as local delegated properties.
 */
fun abiMetadataProcessor(internalName: String, publicClasses: Set<String>, annotationVisitor: AnnotationVisitor): AnnotationVisitor =
    kotlinClassHeaderVisitor { header ->
        // kotlinx-metadata only supports writing Kotlin metadata of version >= 1.4, so we need to
        // update the metadata version if we encounter older metadata annotations.
        val metadataVersion =
            if (header.metadataVersion.let { it.size >= 2 && it[0] >= 1 && it[1] >= 4 }) header.metadataVersion else intArrayOf(1, 4)
        val newHeader = when (val metadata = KotlinClassMetadata.read(header)) {
            is KotlinClassMetadata.Class -> {
                val writer = KotlinClassMetadata.Class.Writer()
                metadata.accept(AbiKmClassVisitor(internalName, publicClasses, writer))
                writer.write(metadataVersion, header.extraInt).header
            }
            is KotlinClassMetadata.FileFacade -> {
                val writer = KotlinClassMetadata.FileFacade.Writer()
                metadata.accept(AbiKmPackageVisitor(writer))
                writer.write(metadataVersion, header.extraInt).header
            }
            is KotlinClassMetadata.MultiFileClassPart -> {
                val writer = KotlinClassMetadata.MultiFileClassPart.Writer()
                metadata.accept(AbiKmPackageVisitor(writer))
                writer.write(metadata.facadeClassName, metadataVersion, header.extraInt).header
            }
            else -> header
        }

        // Write out the stripped annotation
        annotationVisitor.visitKotlinMetadata(newHeader)
    }

/**
 * Parse a KotlinClassHeader from an existing Kotlin Metadata annotation visitor.
 */
private fun kotlinClassHeaderVisitor(body: (KotlinClassHeader) -> Unit): AnnotationVisitor =
    object : AnnotationVisitor(Opcodes.API_VERSION) {
        var kind: Int = 1
        var metadataVersion: IntArray = intArrayOf()
        var data1: MutableList<String> = mutableListOf()
        var data2: MutableList<String> = mutableListOf()
        var extraString: String = ""
        var packageName: String = ""
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

        override fun visitArray(name: String): AnnotationVisitor {
            val destination = when (name) {
                METADATA_DATA_FIELD_NAME -> data1
                METADATA_STRINGS_FIELD_NAME -> data2
                else -> error("Wrong array field in kotlin metadata: $name")
            }
            return object : AnnotationVisitor(Opcodes.ASM7) {
                override fun visit(name: String?, value: Any?) {
                    destination += value as String
                }
            }
        }


        override fun visitEnd() {
            body(KotlinClassHeader(
                kind,
                metadataVersion,
                data1.toTypedArray(),
                data2.toTypedArray(),
                extraString,
                packageName,
                extraInt
            ))
        }
    }

/**
 * Serialize a KotlinClassHeader to an existing Kotlin Metadata annotation visitor.
 */
private fun AnnotationVisitor.visitKotlinMetadata(header: KotlinClassHeader) {
    visit(KIND_FIELD_NAME, header.kind)
    visit(METADATA_VERSION_FIELD_NAME, header.metadataVersion)
    visitArray(METADATA_DATA_FIELD_NAME).apply {
        header.data1.forEach { visit(null, it) }
        visitEnd()
    }
    visitArray(METADATA_STRINGS_FIELD_NAME).apply {
        header.data2.forEach { visit(null, it) }
        visitEnd()
    }
    visit(METADATA_EXTRA_STRING_FIELD_NAME, header.extraString)
    visit(METADATA_PACKAGE_NAME_FIELD_NAME, header.packageName)
    visit(METADATA_EXTRA_INT_FIELD_NAME, header.extraInt)
    visitEnd()
}

/**
 * Class metadata adapter which removes private functions, properties, type aliases,
 * and local delegated properties.
 */
private class AbiKmClassVisitor(private val internalName: String, private val publicClasses: Set<String>, delegate: KmClassVisitor) : KmClassVisitor(delegate) {
    override fun visitConstructor(flags: Flags): KmConstructorVisitor? {
        if (!isPrivateDeclaration(flags))
            return super.visitConstructor(flags)
        return null
    }

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        if (!isPrivateDeclaration(flags))
            return super.visitFunction(flags, name)
        return null
    }

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? {
        if (!isPrivateDeclaration(flags))
            return super.visitProperty(flags, name, getterFlags, setterFlags)
        return null
    }

    override fun visitNestedClass(name: String) {
        if ("$internalName\$$name" in publicClasses)
            super.visitNestedClass(name)
    }

    override fun visitCompanionObject(name: String) {
        if ("$internalName\$$name" in publicClasses)
            super.visitCompanionObject(name)
    }

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? {
        if (!isPrivateDeclaration(flags))
            return super.visitTypeAlias(flags, name)
        return null
    }

    override fun visitExtensions(type: KmExtensionType): KmClassExtensionVisitor? {
        val delegate = super.visitExtensions(type)
        if (type != JvmClassExtensionVisitor.TYPE) return delegate
        return object : JvmClassExtensionVisitor(delegate as JvmClassExtensionVisitor?) {
            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor? = null
        }
    }
}

/**
 * Class metadata adapter which removes private functions, properties, and type aliases.
 */
private class AbiKmPackageVisitor(delegate: KmPackageVisitor) : KmPackageVisitor(delegate) {
    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        if (!isPrivateDeclaration(flags))
            return super.visitFunction(flags, name)
        return null
    }

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? {
        if (!isPrivateDeclaration(flags))
            return super.visitProperty(flags, name, getterFlags, setterFlags)
        return null
    }

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? {
        if (!isPrivateDeclaration(flags))
            return super.visitTypeAlias(flags, name)
        return null
    }

    override fun visitExtensions(type: KmExtensionType): KmPackageExtensionVisitor? {
        val delegate = super.visitExtensions(type)
        if (type != JvmPackageExtensionVisitor.TYPE) return delegate
        return object : JvmPackageExtensionVisitor(delegate as JvmPackageExtensionVisitor?) {
            override fun visitLocalDelegatedProperty(
                flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags
            ): KmPropertyVisitor? = null
        }
    }
}

private fun isPrivateDeclaration(flags: Flags): Boolean =
    Flag.IS_PRIVATE(flags) || Flag.IS_PRIVATE_TO_THIS(flags) || Flag.IS_LOCAL(flags)
