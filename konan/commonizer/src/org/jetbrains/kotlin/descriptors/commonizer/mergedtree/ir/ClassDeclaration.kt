/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface ClassDeclaration : AnnotatedDeclaration, NamedDeclaration, DeclarationWithTypeParameters, DeclarationWithVisibility {
    val companion: FqName? // null means no companion object
    val kind: ClassKind
    val modality: Modality
    val isCompanion: Boolean
    val isData: Boolean
    val isInline: Boolean
    val isInner: Boolean
    val isExternal: Boolean
    val sealedSubclasses: Collection<FqName>
    val supertypes: Collection<KotlinType>
}

interface ClassConstructor : AnnotatedDeclaration, DeclarationWithTypeParameters, DeclarationWithVisibility, CallableMemberWithParameters {
    val isPrimary: Boolean
    val kind: CallableMemberDescriptor.Kind
}

data class CommonClassDeclaration(
    override val name: Name,
    override val typeParameters: List<TypeParameter>,
    override val kind: ClassKind,
    override val modality: Modality,
    override val visibility: Visibility,
    override val isCompanion: Boolean,
    override val isInline: Boolean,
    override val isInner: Boolean
) : ClassDeclaration {
    override val annotations get() = Annotations.EMPTY
    override val isData get() = false
    override val isExternal get() = false
    override var companion: FqName? = null
    override val sealedSubclasses: Collection<FqName> get() = emptyList()
    override val supertypes: MutableCollection<KotlinType> = ArrayList()
}

data class CommonClassConstructor(
    override val isPrimary: Boolean,
    override val kind: CallableMemberDescriptor.Kind,
    override val visibility: Visibility,
    override val typeParameters: List<TypeParameter>,
    override val valueParameters: List<ValueParameter>,
    override val hasStableParameterNames: Boolean,
    override val hasSynthesizedParameterNames: Boolean
) : ClassConstructor {
    override val annotations: Annotations get() = Annotations.EMPTY
}

class TargetClassDeclaration(private val descriptor: ClassDescriptor) : ClassDeclaration {
    override val annotations get() = descriptor.annotations
    override val name get() = descriptor.name
    override val typeParameters by lazy(PUBLICATION) { descriptor.declaredTypeParameters.map(::TargetTypeParameter) }
    override val companion by lazy(PUBLICATION) { descriptor.companionObjectDescriptor?.fqNameSafe }
    override val kind get() = descriptor.kind
    override val modality get() = descriptor.modality
    override val visibility get() = descriptor.visibility
    override val isCompanion get() = descriptor.isCompanionObject
    override val isData get() = descriptor.isData
    override val isInline get() = descriptor.isInline
    override val isInner get() = descriptor.isInner
    override val isExternal get() = descriptor.isExternal
    override val sealedSubclasses by lazy(PUBLICATION) { descriptor.sealedSubclasses.map { it.fqNameSafe } }
    override val supertypes: Collection<KotlinType> get() = descriptor.typeConstructor.supertypes
}

class TargetClassConstructor(private val descriptor: ClassConstructorDescriptor) : ClassConstructor {
    override val isPrimary: Boolean get() = descriptor.isPrimary
    override val kind: CallableMemberDescriptor.Kind get() = descriptor.kind
    override val annotations: Annotations get() = descriptor.annotations
    override val visibility: Visibility get() = descriptor.visibility
    override val typeParameters: List<TypeParameter> by lazy(PUBLICATION) { descriptor.typeParameters.map(::TargetTypeParameter) }
    override val valueParameters: List<ValueParameter> by lazy(PUBLICATION) { descriptor.valueParameters.map(::PlatformValueParameter) }
    override val hasStableParameterNames: Boolean get() = descriptor.hasStableParameterNames()
    override val hasSynthesizedParameterNames: Boolean get() = descriptor.hasSynthesizedParameterNames()
}

object ClassDeclarationRecursionMarker : ClassDeclaration, RecursionMarker {
    override val companion: FqName? get() = unsupported()
    override val kind: ClassKind get() = unsupported()
    override val modality: Modality get() = unsupported()
    override val isCompanion: Boolean get() = unsupported()
    override val isData: Boolean get() = unsupported()
    override val isInline: Boolean get() = unsupported()
    override val isInner: Boolean get() = unsupported()
    override val isExternal: Boolean get() = unsupported()
    override val sealedSubclasses: Collection<FqName> get() = unsupported()
    override val supertypes: Collection<KotlinType> get() = unsupported()
    override val annotations: Annotations get() = unsupported()
    override val name: Name get() = unsupported()
    override val visibility: Visibility get() = unsupported()
    override val typeParameters: List<TypeParameter> get() = unsupported()
}
