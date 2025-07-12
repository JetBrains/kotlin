/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.findPackage
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable


fun ClassDescriptor.isInlined(): Boolean = KotlinTypeInlineClassesSupport.isInlined(this)

fun KotlinType.binaryRepresentationIsNullable() = KotlinTypeInlineClassesSupport.representationIsNullable(this)

@InternalKotlinNativeApi
inline fun <R> KotlinType.unwrapToPrimitiveOrReference(
    eachInlinedClass: (inlinedClass: ClassDescriptor, nullable: Boolean) -> Unit,
    ifPrimitive: (primitiveType: KonanPrimitiveType, nullable: Boolean) -> R,
    ifReference: (type: KotlinType) -> R,
): R = KotlinTypeInlineClassesSupport.unwrapToPrimitiveOrReference(this, eachInlinedClass, ifPrimitive, ifReference)


// TODO: consider renaming to `isReference`.
fun KotlinType.binaryTypeIsReference(): Boolean = this.computePrimitiveBinaryTypeOrNull() == null

fun KotlinType.computePrimitiveBinaryTypeOrNull(): PrimitiveBinaryType? =
    this.computeBinaryType().primitiveBinaryTypeOrNull()

fun KotlinType.computeBinaryType(): BinaryType<ClassDescriptor> = KotlinTypeInlineClassesSupport.computeBinaryType(this)

@InternalKotlinNativeApi
object KotlinTypeInlineClassesSupport : InlineClassesSupport<ClassDescriptor, KotlinType>() {

    override fun isNullable(type: KotlinType): Boolean = type.isNullable()
    override fun makeNullable(type: KotlinType): KotlinType = type.makeNullable()
    override tailrec fun erase(type: KotlinType): ClassDescriptor {
        val descriptor = type.constructor.declarationDescriptor
        return if (descriptor is ClassDescriptor) {
            descriptor
        } else {
            erase(type.constructor.supertypes.first())
        }
    }

    override fun computeFullErasure(type: KotlinType): Sequence<ClassDescriptor> {
        val classifier = type.constructor.declarationDescriptor
        return if (classifier is ClassDescriptor) sequenceOf(classifier)
        else type.constructor.supertypes.asSequence().flatMap { computeFullErasure(it) }
    }

    override fun hasInlineModifier(clazz: ClassDescriptor): Boolean = clazz.isInlineClass()

    override fun getNativePointedSuperclass(clazz: ClassDescriptor): ClassDescriptor? = clazz.getAllSuperClassifiers()
        .firstOrNull { it.fqNameUnsafe == InteropFqNames.nativePointed } as ClassDescriptor?

    override fun getInlinedClassUnderlyingType(clazz: ClassDescriptor): KotlinType =
        clazz.unsubstitutedPrimaryConstructor!!.valueParameters.single().type

    override fun getPackageFqName(clazz: ClassDescriptor) =
        clazz.findPackage().fqName

    override fun getName(clazz: ClassDescriptor) =
        clazz.name

    override fun isTopLevelClass(clazz: ClassDescriptor): Boolean = clazz.containingDeclaration is PackageFragmentDescriptor
}

