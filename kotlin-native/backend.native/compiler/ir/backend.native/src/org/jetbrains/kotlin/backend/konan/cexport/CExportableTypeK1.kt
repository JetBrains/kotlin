/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.BinaryType
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.backend.konan.computeBinaryType
import org.jetbrains.kotlin.backend.konan.unwrapToPrimitiveOrReference
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

/** Views a K1 [KotlinType] as a [CExportableType], classifying it via the descriptor-based inline-classes support. */
internal fun KotlinType.asCExportableType(): CExportableType = K1CExportableType(this)

private class K1CExportableType(private val type: KotlinType) : CExportableType {
    override fun isUnit(): Boolean = type.isUnit()

    override fun isNothing(): Boolean = type.isNothing()

    override fun binaryTypeIsReference(): Boolean = type.binaryTypeIsReference()

    override fun referenceClassOrNull(): CExportableClass? =
            (type.computeBinaryType() as? BinaryType.Reference)?.types?.first()?.let(::K1CExportableClass)

    override fun <R : Any> unwrapToPrimitiveOrReference(
            eachInlinedClass: (CExportableClass) -> R?,
            ifPrimitive: (KonanPrimitiveType) -> R,
            ifReference: (CExportableClass) -> R,
    ): R = type.unwrapToPrimitiveOrReference(
            eachInlinedClass = { inlinedClass, _ -> eachInlinedClass(K1CExportableClass(inlinedClass))?.let { return it } },
            ifPrimitive = { primitiveType, _ -> ifPrimitive(primitiveType) },
            ifReference = { referenceType ->
                ifReference(K1CExportableClass((referenceType.computeBinaryType() as BinaryType.Reference).types.first()))
            },
    )
}

private class K1CExportableClass(private val descriptor: ClassDescriptor) : CExportableClass {
    override val classId: ClassId? get() = descriptor.classId
    override fun isString(): Boolean = descriptor == descriptor.builtIns.string
    override val fqName: String get() = descriptor.fqNameSafe.asString()
}
