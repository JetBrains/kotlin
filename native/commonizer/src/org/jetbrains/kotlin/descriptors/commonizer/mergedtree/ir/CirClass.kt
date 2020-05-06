/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

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
    override val annotations: List<CirAnnotation> get() = emptyList()
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
    override val annotations: List<CirAnnotation> get() = emptyList()
    override val containingClassKind get() = unsupported()
    override val containingClassModality get() = unsupported()
    override val containingClassIsData get() = unsupported()
}

class CirClassImpl(original: ClassDescriptor) : CirClass {
    override val annotations = original.annotations.map(CirAnnotation.Companion::create)
    override val name = original.name.intern()
    override val typeParameters = original.declaredTypeParameters.map(::CirTypeParameterImpl)
    override val companion = original.companionObjectDescriptor?.fqNameSafe?.intern()
    override val kind = original.kind
    override val modality = original.modality
    override val visibility = original.visibility
    override val isCompanion = original.isCompanionObject
    override val isData = original.isData
    override val isInline = original.isInline
    override val isInner = original.isInner
    override val isExternal = original.isExternal
    override val supertypes = original.typeConstructor.supertypes.map(CirType.Companion::create)
}

class CirClassConstructorImpl(original: ClassConstructorDescriptor) : CirClassConstructor {
    override val isPrimary = original.isPrimary
    override val kind = original.kind
    override val containingClassKind = original.containingDeclaration.kind
    override val containingClassModality = original.containingDeclaration.modality
    override val containingClassIsData = original.containingDeclaration.isData
    override val annotations = original.annotations.map(CirAnnotation.Companion::create)
    override val visibility = original.visibility
    override val typeParameters = original.typeParameters.mapNotNull { typeParameter ->
        // save only type parameters that are contributed by the constructor itself
        typeParameter.takeIf { it.containingDeclaration == original }?.let(::CirTypeParameterImpl)
    }
    override val valueParameters = original.valueParameters.map(CirValueParameterImpl.Companion::create)
    override val hasStableParameterNames = original.hasStableParameterNames()
    override val hasSynthesizedParameterNames = original.hasSynthesizedParameterNames()
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
