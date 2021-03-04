/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.Flag
import kotlinx.metadata.KmProperty
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.compileTimeValue
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirPropertyImpl
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeCallableKind
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeModality
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeVisibility
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirPropertyFactory {
    fun create(name: CirName, source: KmProperty, containingClass: CirContainingClass?, typeResolver: CirTypeResolver): CirProperty {
        val compileTimeInitializer = if (Flag.Property.HAS_CONSTANT(source.flags)) {
            CirConstantValueFactory.createSafely(
                constantValue = source.compileTimeValue,
                owner = source,
            )
        } else CirConstantValue.NullValue

        return create(
            annotations = CirAnnotationFactory.createAnnotations(source.flags, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { CirTypeParameterFactory.create(it, typeResolver) },
            visibility = decodeVisibility(source.flags),
            modality = decodeModality(source.flags),
            containingClass = containingClass,
            isExternal = Flag.Property.IS_EXTERNAL(source.flags),
            extensionReceiver = source.receiverParameterType?.let { CirExtensionReceiverFactory.create(it, typeResolver) },
            returnType = CirTypeFactory.create(source.returnType, typeResolver),
            kind = decodeCallableKind(source.flags),
            isVar = Flag.Property.IS_VAR(source.flags),
            isLateInit = Flag.Property.IS_LATEINIT(source.flags),
            isConst = Flag.Property.IS_CONST(source.flags),
            isDelegate = Flag.Property.IS_DELEGATED(source.flags),
            getter = CirPropertyGetterFactory.create(source, typeResolver),
            setter = CirPropertySetterFactory.create(source, typeResolver),
            backingFieldAnnotations = emptyList(), // TODO unclear where to read backing/delegate field annotations from, see KT-44625
            delegateFieldAnnotations = emptyList(), // TODO unclear where to read backing/delegate field annotations from, see KT-44625
            compileTimeInitializer = compileTimeInitializer
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: CirName,
        typeParameters: List<CirTypeParameter>,
        visibility: Visibility,
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
