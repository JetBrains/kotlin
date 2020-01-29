/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface CirClass : CirAnnotatedDeclaration, CirNamedDeclaration, CirDeclarationWithTypeParameters, CirDeclarationWithVisibility, CirDeclarationWithModality {
    val companion: FqName? // null means no companion object
    val kind: ClassKind
    val isCompanion: Boolean
    val isData: Boolean
    val isInline: Boolean
    val isInner: Boolean
    val isExternal: Boolean
    val supertypes: Collection<CirType>
}

interface CirClassConstructor : CirAnnotatedDeclaration, CirDeclarationWithTypeParameters, CirDeclarationWithVisibility, CirMaybeCallableMemberOfClass, CirCallableMemberWithParameters {
    val isPrimary: Boolean
    val kind: CallableMemberDescriptor.Kind
    override val containingClassKind: ClassKind
    override val containingClassModality: Modality
    override val containingClassIsData: Boolean
}

data class CirCommonClass(
    override val name: Name,
    override val typeParameters: List<CirTypeParameter>,
    override val kind: ClassKind,
    override val modality: Modality,
    override val visibility: Visibility,
    override val isCompanion: Boolean,
    override val isInline: Boolean,
    override val isInner: Boolean
) : CirClass {
    override val annotations: List<CirAnnotation> get() = emptyList() // TODO: commonize annotations, KT-34234
    override val isData get() = false
    override val isExternal get() = false
    override var companion: FqName? = null
    override val supertypes: MutableCollection<CirType> = ArrayList()
}

data class CirCommonClassConstructor(
    override val isPrimary: Boolean,
    override val kind: CallableMemberDescriptor.Kind,
    override val visibility: Visibility,
    override val typeParameters: List<CirTypeParameter>,
    override val valueParameters: List<CirValueParameter>,
    override val hasStableParameterNames: Boolean,
    override val hasSynthesizedParameterNames: Boolean
) : CirClassConstructor {
    override val annotations: List<CirAnnotation> get() = emptyList() // TODO: commonize annotations, KT-34234
    override val containingClassKind get() = unsupported()
    override val containingClassModality get() = unsupported()
    override val containingClassIsData get() = unsupported()
}

class CirWrappedClass(private val wrapped: ClassDescriptor) : CirClass {
    override val annotations by lazy(PUBLICATION) { wrapped.annotations.map(::CirAnnotation) }
    override val name get() = wrapped.name
    override val typeParameters by lazy(PUBLICATION) { wrapped.declaredTypeParameters.map(::CirWrappedTypeParameter) }
    override val companion by lazy(PUBLICATION) { wrapped.companionObjectDescriptor?.fqNameSafe }
    override val kind get() = wrapped.kind
    override val modality get() = wrapped.modality
    override val visibility get() = wrapped.visibility
    override val isCompanion get() = wrapped.isCompanionObject
    override val isData get() = wrapped.isData
    override val isInline get() = wrapped.isInline
    override val isInner get() = wrapped.isInner
    override val isExternal get() = wrapped.isExternal
    override val supertypes by lazy(PUBLICATION) { wrapped.typeConstructor.supertypes.map(CirType.Companion::create) }
}

class CirWrappedClassConstructor(private val wrapped: ClassConstructorDescriptor) : CirClassConstructor {
    override val isPrimary get() = wrapped.isPrimary
    override val kind get() = wrapped.kind
    override val containingClassKind get() = wrapped.containingDeclaration.kind
    override val containingClassModality get() = wrapped.containingDeclaration.modality
    override val containingClassIsData get() = wrapped.containingDeclaration.isData
    override val annotations by lazy(PUBLICATION) { wrapped.annotations.map(::CirAnnotation) }
    override val visibility get() = wrapped.visibility
    override val typeParameters by lazy(PUBLICATION) {
        wrapped.typeParameters.mapNotNull { typeParameter ->
            // save only type parameters that are contributed by the constructor itself
            typeParameter.takeIf { it.containingDeclaration == wrapped }?.let(::CirWrappedTypeParameter)
        }
    }
    override val valueParameters by lazy(PUBLICATION) { wrapped.valueParameters.map(::CirWrappedValueParameter) }
    override val hasStableParameterNames get() = wrapped.hasStableParameterNames()
    override val hasSynthesizedParameterNames get() = wrapped.hasSynthesizedParameterNames()
}

object CirClassRecursionMarker : CirClass, CirRecursionMarker {
    override val companion get() = unsupported()
    override val kind get() = unsupported()
    override val modality get() = unsupported()
    override val isCompanion get() = unsupported()
    override val isData get() = unsupported()
    override val isInline get() = unsupported()
    override val isInner get() = unsupported()
    override val isExternal get() = unsupported()
    override val supertypes get() = unsupported()
    override val annotations get() = unsupported()
    override val name get() = unsupported()
    override val visibility get() = unsupported()
    override val typeParameters get() = unsupported()
}
