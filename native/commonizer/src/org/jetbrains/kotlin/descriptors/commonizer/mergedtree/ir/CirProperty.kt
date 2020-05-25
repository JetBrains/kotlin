/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirGetter.Companion.toGetter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirSetter.Companion.toSetter
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.checkConstantSupportedInCommonization
import org.jetbrains.kotlin.resolve.constants.ConstantValue

interface CirProperty : CirFunctionOrProperty {
    val isVar: Boolean
    val isLateInit: Boolean
    val isConst: Boolean
    val isDelegate: Boolean
    val getter: CirGetter?
    val setter: CirSetter?
    val backingFieldAnnotations: List<CirAnnotation>? // null assumes no backing field
    val delegateFieldAnnotations: List<CirAnnotation>? // null assumes no backing field
    val compileTimeInitializer: ConstantValue<*>?
}

data class CirCommonProperty(
    override val name: Name,
    override val modality: Modality,
    override val visibility: Visibility,
    override val isExternal: Boolean,
    override val extensionReceiver: CirExtensionReceiver?,
    override val returnType: CirType,
    override val kind: CallableMemberDescriptor.Kind,
    override val setter: CirSetter?,
    override val typeParameters: List<CirTypeParameter>
) : CirCommonFunctionOrProperty(), CirProperty {
    override val annotations: List<CirAnnotation> get() = emptyList()
    override val isVar get() = setter != null
    override val isLateInit get() = false
    override val isConst get() = false
    override val isDelegate get() = false
    override val getter get() = CirGetter.DEFAULT_NO_ANNOTATIONS
    override val backingFieldAnnotations: List<CirAnnotation>? get() = null
    override val delegateFieldAnnotations: List<CirAnnotation>? get() = null
    override val compileTimeInitializer: ConstantValue<*>? get() = null
}

class CirPropertyImpl(original: PropertyDescriptor) : CirFunctionOrPropertyImpl<PropertyDescriptor>(original), CirProperty {
    override val isVar = original.isVar
    override val isLateInit = original.isLateInit
    override val isConst = original.isConst
    override val isDelegate = @Suppress("DEPRECATION") original.isDelegated
    override val getter = original.getter?.toGetter()
    override val setter = original.setter?.toSetter()
    override val backingFieldAnnotations = original.backingField?.annotations?.map(CirAnnotation.Companion::create)
    override val delegateFieldAnnotations = original.delegateField?.annotations?.map(CirAnnotation.Companion::create)
    override val compileTimeInitializer = original.compileTimeInitializer

    init {
        compileTimeInitializer?.let { compileTimeInitializer ->
            checkConstantSupportedInCommonization(
                constantValue = compileTimeInitializer,
                owner = original,
                allowAnnotationValues = false
            )
        }
    }
}

interface CirPropertyAccessor {
    val annotations: List<CirAnnotation>
    val isDefault: Boolean
    val isExternal: Boolean
    val isInline: Boolean
}

@Suppress("DataClassPrivateConstructor")
data class CirGetter private constructor(
    override val annotations: List<CirAnnotation>,
    override val isDefault: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean
) : CirPropertyAccessor {
    companion object {
        private val interner = Interner<CirGetter>()

        val DEFAULT_NO_ANNOTATIONS = interner.intern(
            CirGetter(
                annotations = emptyList(),
                isDefault = true,
                isExternal = false,
                isInline = false
            )
        )

        fun PropertyGetterDescriptor.toGetter() =
            if (isDefault && annotations.isEmpty())
                DEFAULT_NO_ANNOTATIONS
            else
                interner.intern(
                    CirGetter(
                        annotations = annotations.map(CirAnnotation.Companion::create),
                        isDefault = isDefault,
                        isExternal = isExternal,
                        isInline = isInline
                    )
                )
    }
}

@Suppress("DataClassPrivateConstructor")
data class CirSetter private constructor(
    override val annotations: List<CirAnnotation>,
    val parameterAnnotations: List<CirAnnotation>,
    override val visibility: Visibility,
    override val isDefault: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean
) : CirPropertyAccessor, CirDeclarationWithVisibility {
    companion object {
        private val interner = Interner<CirSetter>()

        fun createDefaultNoAnnotations(visibility: Visibility) = interner.intern(
            CirSetter(
                annotations = emptyList(),
                parameterAnnotations = emptyList(),
                visibility = visibility,
                isDefault = visibility == Visibilities.PUBLIC,
                isExternal = false,
                isInline = false
            )
        )

        fun PropertySetterDescriptor.toSetter() = interner.intern(
            CirSetter(
                annotations = annotations.map(CirAnnotation.Companion::create),
                parameterAnnotations = valueParameters.single().annotations.map(CirAnnotation.Companion::create),
                visibility = visibility,
                isDefault = isDefault,
                isExternal = isExternal,
                isInline = isInline
            )
        )
    }
}
