/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import kotlin.metadata.*
import kotlin.metadata.jvm.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Wrap the visitor for a Kotlin Metadata annotation to strip out private and local
 * functions, properties, and type aliases as well as local delegated properties.
 */
fun abiMetadataProcessor(
    annotationVisitor: AnnotationVisitor,
    removeDataClassCopyIfConstructorIsPrivate: Boolean,
    preserveDeclarationOrder: Boolean,
    classesToBeDeleted: Set<String>,
    pruneClass: Boolean,
    treatInternalAsPrivate: Boolean,
): AnnotationVisitor =
    kotlinClassHeaderVisitor { header ->
        // kotlinx-metadata only supports writing Kotlin metadata of version >= 1.4, so we need to
        // update the metadata version if we encounter older metadata annotations.
        val metadataVersion = header.metadataVersion.takeIf { v ->
            val major = v.getOrNull(0) ?: 0
            val minor = v.getOrNull(1) ?: 0
            major > 1 || major == 1 && minor >= 4
        } ?: intArrayOf(1, 4)

        val newHeader = runCatching {
            KotlinClassMetadata.transform(header) { metadata ->
                when (metadata) {
                    is KotlinClassMetadata.Class -> {
                        metadata.kmClass.removePrivateDeclarations(
                            removeDataClassCopyIfConstructorIsPrivate,
                            preserveDeclarationOrder,
                            classesToBeDeleted,
                            pruneClass,
                            treatInternalAsPrivate,
                        )
                    }
                    is KotlinClassMetadata.FileFacade -> {
                        metadata.kmPackage.removePrivateDeclarations(preserveDeclarationOrder, pruneClass, treatInternalAsPrivate)
                    }
                    is KotlinClassMetadata.MultiFileClassPart -> {
                        metadata.kmPackage.removePrivateDeclarations(preserveDeclarationOrder, pruneClass, treatInternalAsPrivate)
                    }
                    else -> Unit
                }
            }
        }.getOrElse { cause ->
            // TODO: maybe jvm-abi-gen should throw this exception by default, and not only in tests.
            if (System.getProperty("idea.is.unit.test").toBoolean()) {
                val actual = "${metadataVersion[0]}.${metadataVersion[1]}"
                val expected = JvmMetadataVersion.LATEST_STABLE_SUPPORTED.toString()
                throw AssertionError(
                    "jvm-abi-gen can't process class file with the new metadata version because the version of kotlinx-metadata-jvm " +
                            "it depends on is too old.\n" +
                            "Class file has metadata version $actual, but default metadata version of kotlinx-metadata-jvm is " +
                            "$expected, so it can process class files with metadata version up to +1 from that (because of " +
                            "Kotlin/JVM's one-version forward compatibility policy).\n" +
                            "To fix this error, ensure that jvm-abi-gen depends on the latest version of kotlinx-metadata-jvm.\n" +
                            "If this happens during the update of the default language version in the project, make sure that " +
                            "a version of kotlinx-metadata-jvm has been published that supports this version, and update " +
                            "\"versions.kotlinx-metadata-jvm\" in `gradle/versions.properties`.",
                    cause
                )
            }
            header
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

private fun KmClass.removePrivateDeclarations(
    removeCopyAlongWithConstructor: Boolean,
    preserveDeclarationOrder: Boolean,
    classesToBeDeleted: Set<String>,
    pruneClass: Boolean,
    treatInternalAsPrivate: Boolean,
) {
    constructors.removeIf { pruneClass || it.visibility.shouldRemove(treatInternalAsPrivate) }
    (this as KmDeclarationContainer).removePrivateDeclarations(
        copyFunShouldBeDeleted(removeCopyAlongWithConstructor),
        preserveDeclarationOrder,
        pruneClass,
        treatInternalAsPrivate,
    )
    nestedClasses.removeIf { "$name\$$it" in classesToBeDeleted }
    companionObject = companionObject.takeUnless { "$name\$$it" in classesToBeDeleted }
    localDelegatedProperties.clear()
    // TODO: do not serialize private type aliases once KT-17229 is fixed.
}

private fun KmPackage.removePrivateDeclarations(
    preserveDeclarationOrder: Boolean,
    pruneClass: Boolean,
    treatInternalAsPrivate: Boolean,
) {
    (this as KmDeclarationContainer).removePrivateDeclarations(false, preserveDeclarationOrder, pruneClass, treatInternalAsPrivate)
    localDelegatedProperties.clear()
    // TODO: do not serialize private type aliases once KT-17229 is fixed.
}

private fun KmDeclarationContainer.removePrivateDeclarations(
    copyFunShouldBeDeleted: Boolean,
    preserveDeclarationOrder: Boolean,
    pruneClass: Boolean,
    treatInternalAsPrivate: Boolean,
) {
    functions.removeIf { pruneClass || it.visibility.shouldRemove(treatInternalAsPrivate) || (copyFunShouldBeDeleted && it.name == "copy") }
    properties.removeIf { pruneClass || it.visibility.shouldRemove(treatInternalAsPrivate) }

    if (!preserveDeclarationOrder) {
        functions.sortWith(compareBy(KmFunction::name, { it.signature.toString() }))
        properties.sortWith(compareBy(KmProperty::name, { it.getterSignature.toString() }))
    }

    for (property in properties) {
        // Whether or not the *non-const* property is initialized by a compile-time constant is not a part of the ABI.
        if (!property.isConst) {
            property.hasConstant = false
        }
    }
}

private fun KmClass.copyFunShouldBeDeleted(removeDataClassCopy: Boolean): Boolean =
    removeDataClassCopy && isData && constructors.none { !it.isSecondary }

private fun Visibility.shouldRemove(treatInternalAsPrivate: Boolean): Boolean {
    return this == Visibility.PRIVATE ||
            this == Visibility.PRIVATE_TO_THIS ||
            this == Visibility.LOCAL ||
            (treatInternalAsPrivate && this == Visibility.INTERNAL)
}
