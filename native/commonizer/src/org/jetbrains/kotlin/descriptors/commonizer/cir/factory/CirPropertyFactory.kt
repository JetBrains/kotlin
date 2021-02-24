/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirPropertyImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirPropertyFactory {
    fun create(source: PropertyDescriptor, containingClass: CirContainingClass?): CirProperty {
        val compileTimeInitializer = source.compileTimeInitializer?.let { constantValue ->
            CirConstantValueFactory.createSafely(
                constantValue = constantValue,
                owner = source,
            )
        } ?: CirConstantValue.NullValue

        return create(
            annotations = source.annotations.compactMap(CirAnnotationFactory::create),
            name = CirName.create(source.name),
            typeParameters = source.typeParameters.compactMap(CirTypeParameterFactory::create),
            visibility = source.visibility,
            modality = source.modality,
            containingClass = containingClass,
            isExternal = source.isExternal,
            extensionReceiver = source.extensionReceiverParameter?.let(CirExtensionReceiverFactory::create),
            returnType = CirTypeFactory.create(source.returnType!!),
            kind = source.kind,
            isVar = source.isVar,
            isLateInit = source.isLateInit,
            isConst = source.isConst,
            isDelegate = source.isDelegated,
            getter = source.getter?.let(CirPropertyGetterFactory::create),
            setter = source.setter?.let(CirPropertySetterFactory::create),
            backingFieldAnnotations = source.backingField?.annotations?.compactMap(CirAnnotationFactory::create).orEmpty(),
            delegateFieldAnnotations = source.delegateField?.annotations?.compactMap(CirAnnotationFactory::create).orEmpty(),
            compileTimeInitializer = compileTimeInitializer
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: CirName,
        typeParameters: List<CirTypeParameter>,
        visibility: DescriptorVisibility,
        modality: Modality,
        containingClass: CirContainingClass?,
        isExternal: Boolean,
        extensionReceiver: CirExtensionReceiver?,
        returnType: CirType,
        kind: CallableMemberDescriptor.Kind,
        isVar: Boolean,
        isLateInit: Boolean,
        isConst: Boolean,
        isDelegate: Boolean,
        getter: CirPropertyGetter?,
        setter: CirPropertySetter?,
        backingFieldAnnotations: List<CirAnnotation>,
        delegateFieldAnnotations: List<CirAnnotation>,
        compileTimeInitializer: CirConstantValue<*>
    ): CirProperty {
        return CirPropertyImpl(
            annotations = annotations,
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            modality = modality,
            containingClass = containingClass,
            isExternal = isExternal,
            extensionReceiver = extensionReceiver,
            returnType = returnType,
            kind = kind,
            isVar = isVar,
            isLateInit = isLateInit,
            isConst = isConst,
            isDelegate = isDelegate,
            getter = getter,
            setter = setter,
            backingFieldAnnotations = backingFieldAnnotations,
            delegateFieldAnnotations = delegateFieldAnnotations,
            compileTimeInitializer = compileTimeInitializer
        )
    }
}
