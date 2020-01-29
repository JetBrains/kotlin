/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirGetter.Companion.toGetter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirSetter.Companion.toSetter
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface CirProperty : CirFunctionOrProperty {
    val isVar: Boolean
    val isLateInit: Boolean
    val isConst: Boolean
    val isDelegate: Boolean
    val getter: CirGetter?
    val setter: CirSetter?
    val backingFieldAnnotations: Annotations? // null assumes no backing field
    val delegateFieldAnnotations: Annotations? // null assumes no backing field
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
    override val isVar get() = setter != null
    override val isLateInit get() = false
    override val isConst get() = false
    override val isDelegate get() = false
    override val getter get() = CirGetter.DEFAULT_NO_ANNOTATIONS
    override val backingFieldAnnotations: Annotations? get() = null
    override val delegateFieldAnnotations: Annotations? get() = null
    override val compileTimeInitializer: ConstantValue<*>? get() = null
}

class CirWrappedProperty(wrapped: PropertyDescriptor) : CirWrappedFunctionOrProperty<PropertyDescriptor>(wrapped), CirProperty {
    override val isVar get() = wrapped.isVar
    override val isLateInit get() = wrapped.isLateInit
    override val isConst get() = wrapped.isConst
    override val isDelegate get() = @Suppress("DEPRECATION") wrapped.isDelegated
    override val getter by lazy(PUBLICATION) { wrapped.getter?.toGetter() }
    override val setter by lazy(PUBLICATION) { wrapped.setter?.toSetter() }
    override val backingFieldAnnotations get() = wrapped.backingField?.annotations
    override val delegateFieldAnnotations get() = wrapped.delegateField?.annotations
    override val compileTimeInitializer get() = wrapped.compileTimeInitializer
}

interface CirPropertyAccessor {
    val annotations: Annotations
    val isDefault: Boolean
    val isExternal: Boolean
    val isInline: Boolean
}

data class CirGetter(
    override val annotations: Annotations,
    override val isDefault: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean
) : CirPropertyAccessor {
    companion object {
        val DEFAULT_NO_ANNOTATIONS = CirGetter(Annotations.EMPTY, isDefault = true, isExternal = false, isInline = false)

        fun PropertyGetterDescriptor.toGetter() =
            if (isDefault && annotations.isEmpty())
                DEFAULT_NO_ANNOTATIONS
            else
                CirGetter(annotations, isDefault, isExternal, isInline)
    }
}

data class CirSetter(
    override val annotations: Annotations,
    val parameterAnnotations: Annotations,
    override val visibility: Visibility,
    override val isDefault: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean
) : CirPropertyAccessor, CirDeclarationWithVisibility {
    companion object {
        fun createDefaultNoAnnotations(visibility: Visibility) = CirSetter(
            Annotations.EMPTY,
            Annotations.EMPTY,
            visibility,
            isDefault = visibility == Visibilities.PUBLIC,
            isExternal = false,
            isInline = false
        )

        fun PropertySetterDescriptor.toSetter() = CirSetter(
            annotations,
            valueParameters.single().annotations,
            visibility,
            isDefault,
            isExternal,
            isInline
        )
    }
}
