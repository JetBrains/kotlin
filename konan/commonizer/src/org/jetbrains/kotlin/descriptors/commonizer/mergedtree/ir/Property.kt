/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Getter.Companion.toGetter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Setter.Companion.toSetter
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface Property : FunctionOrProperty {
    val isVar: Boolean
    val isLateInit: Boolean
    val isConst: Boolean
    val isDelegate: Boolean
    val getter: Getter?
    val setter: Setter?
    val backingFieldAnnotations: Annotations? // null assumes no backing field
    val delegateFieldAnnotations: Annotations? // null assumes no backing field
    val compileTimeInitializer: ConstantValue<*>?
}

data class CommonProperty(
    override val name: Name,
    override val modality: Modality,
    override val visibility: Visibility,
    override val isExternal: Boolean,
    override val extensionReceiver: ExtensionReceiver?,
    override val returnType: UnwrappedType,
    override val setter: Setter?,
    override val typeParameters: List<TypeParameter>
) : CommonFunctionOrProperty(), Property {
    override val isVar get() = setter != null
    override val isLateInit get() = false
    override val isConst get() = false
    override val isDelegate get() = false
    override val getter get() = Getter.DEFAULT_NO_ANNOTATIONS
    override val backingFieldAnnotations: Annotations? get() = null
    override val delegateFieldAnnotations: Annotations? get() = null
    override val compileTimeInitializer: ConstantValue<*>? get() = null
}

class TargetProperty(descriptor: PropertyDescriptor) : TargetFunctionOrProperty<PropertyDescriptor>(descriptor), Property {
    override val isVar get() = descriptor.isVar
    override val isLateInit get() = descriptor.isLateInit
    override val isConst get() = descriptor.isConst
    override val isDelegate get() = @Suppress("DEPRECATION") descriptor.isDelegated
    override val getter by lazy(PUBLICATION) { descriptor.getter?.toGetter() }
    override val setter by lazy(PUBLICATION) { descriptor.setter?.toSetter() }
    override val backingFieldAnnotations get() = descriptor.backingField?.annotations
    override val delegateFieldAnnotations get() = descriptor.delegateField?.annotations
    override val compileTimeInitializer get() = descriptor.compileTimeInitializer
}

interface PropertyAccessor {
    val annotations: Annotations
    val isDefault: Boolean
    val isExternal: Boolean
    val isInline: Boolean
}

data class Getter(
    override val annotations: Annotations,
    override val isDefault: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean
) : PropertyAccessor {
    companion object {
        val DEFAULT_NO_ANNOTATIONS = Getter(Annotations.EMPTY, isDefault = true, isExternal = false, isInline = false)

        fun PropertyGetterDescriptor.toGetter() =
            if (isDefault && annotations.isEmpty())
                DEFAULT_NO_ANNOTATIONS
            else
                Getter(annotations, isDefault, isExternal, isInline)
    }
}

data class Setter(
    override val annotations: Annotations,
    val parameterAnnotations: Annotations,
    override val visibility: Visibility,
    override val isDefault: Boolean,
    override val isExternal: Boolean,
    override val isInline: Boolean
) : PropertyAccessor, MaybeVirtualCallableMember {
    override val isVirtual get() = false

    companion object {
        fun createDefaultNoAnnotations(visibility: Visibility) = Setter(
            Annotations.EMPTY,
            Annotations.EMPTY,
            visibility,
            isDefault = visibility == Visibilities.PUBLIC,
            isExternal = false,
            isInline = false
        )

        fun PropertySetterDescriptor.toSetter() = Setter(
            annotations,
            valueParameters.single().annotations,
            visibility,
            isDefault,
            isExternal,
            isInline
        )
    }
}
