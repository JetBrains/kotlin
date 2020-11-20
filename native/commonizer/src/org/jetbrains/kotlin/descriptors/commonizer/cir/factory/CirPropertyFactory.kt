/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirPropertyImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.checkConstantSupportedInCommonization
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue

object CirPropertyFactory {
    fun create(source: PropertyDescriptor): CirProperty {
        val compileTimeInitializer: ConstantValue<*>? = source.compileTimeInitializer
        if (compileTimeInitializer != null) {
            checkConstantSupportedInCommonization(
                constantValue = compileTimeInitializer,
                owner = source,
                allowAnnotationValues = false
            )
        }

        return create(
            annotations = source.annotations.compactMap(CirAnnotationFactory::create),
            name = source.name.intern(),
            typeParameters = source.typeParameters.compactMap(CirTypeParameterFactory::create),
            visibility = source.visibility,
            modality = source.modality,
            containingClassDetails = CirContainingClassDetailsFactory.create(source),
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
            backingFieldAnnotations = source.backingField?.annotations?.compactMap(CirAnnotationFactory::create),
            delegateFieldAnnotations = source.delegateField?.annotations?.compactMap(CirAnnotationFactory::create),
            compileTimeInitializer = source.compileTimeInitializer
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: Name,
        typeParameters: List<CirTypeParameter>,
        visibility: DescriptorVisibility,
        modality: Modality,
        containingClassDetails: CirContainingClassDetails?,
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
        backingFieldAnnotations: List<CirAnnotation>?,
        delegateFieldAnnotations: List<CirAnnotation>?,
        compileTimeInitializer: ConstantValue<*>?
    ): CirProperty {
        return CirPropertyImpl(
            annotations = annotations,
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            modality = modality,
            containingClassDetails = containingClassDetails,
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
