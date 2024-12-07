/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirConstantValue
import org.jetbrains.kotlin.commonizer.cir.CirProperty
import org.jetbrains.kotlin.commonizer.cir.CirPropertyGetter
import org.jetbrains.kotlin.commonizer.core.PropertyCommonizer.ConstCommonizationState.*
import org.jetbrains.kotlin.descriptors.Modality

class PropertyCommonizer(
    functionOrPropertyBaseCommonizer: FunctionOrPropertyBaseCommonizer
) : AbstractStandardCommonizer<CirProperty, CirProperty?>() {
    private val setter = PropertySetterCommonizer.asNullableCommonizer()
    private lateinit var constCommonizationState: ConstCommonizationState
    private val functionOrPropertyBaseCommonizer = functionOrPropertyBaseCommonizer.asCommonizer()

    override fun commonizationResult(): CirProperty? {
        val functionOrPropertyBase = functionOrPropertyBaseCommonizer.result ?: return null

        val setter = setter.result?.takeIf { setter ->
            setter !== PropertySetterCommonizer.privateFallbackSetter || functionOrPropertyBase.modality == Modality.FINAL
        }

        val constCommonizationState = constCommonizationState
        val constCompileTimeInitializer = (constCommonizationState as? ConstSameValue)?.compileTimeInitializer

        return CirProperty(
            annotations = functionOrPropertyBase.annotations,
            name = functionOrPropertyBase.name,
            typeParameters = functionOrPropertyBase.typeParameters,
            visibility = functionOrPropertyBase.visibility,
            modality = functionOrPropertyBase.modality,
            containingClass = null, // does not matter
            extensionReceiver = functionOrPropertyBase.extensionReceiver,
            returnType = functionOrPropertyBase.returnType,
            kind = functionOrPropertyBase.kind,
            isVar = setter != null,
            isLateInit = false,
            isConst = constCompileTimeInitializer != null,
            isDelegate = false,
            getter = CirPropertyGetter.DEFAULT_NO_ANNOTATIONS,
            setter = setter,
            backingFieldAnnotations = emptyList(),
            delegateFieldAnnotations = emptyList(),
            compileTimeInitializer = constCompileTimeInitializer ?: CirConstantValue.NullValue
        )
    }

    override fun initialize(first: CirProperty) {
        constCommonizationState = if (first.isConst) {
            first.compileTimeInitializer.takeIf { it != CirConstantValue.NullValue }?.let(::ConstSameValue) ?: NonConst
        } else {
            NonConst
        }
    }

    override fun doCommonizeWith(next: CirProperty): Boolean {
        if (next.isLateInit) {
            // expect property can't be lateinit
            return false
        }

        val constCommonizationState = constCommonizationState
        if (next.isConst) {
            // const properties should be lifted up
            // otherwise commonization should fail: expect property can't be const because expect can't have initializer
            when (constCommonizationState) {
                NonConst -> {
                    // previous property was not constant
                    this.constCommonizationState = NonConst
                }
                is Const -> {
                    if (constCommonizationState is ConstSameValue) {
                        if (constCommonizationState.compileTimeInitializer != next.compileTimeInitializer) {
                            // const properties have different constants
                            this.constCommonizationState = ConstMultipleValues()
                        }
                    }
                }
            }
        } else if (constCommonizationState != NonConst) {
            // previous property was constant but this one is not
            this.constCommonizationState = NonConst
        }

        val result = functionOrPropertyBaseCommonizer.commonizeWith(next)
                && setter.commonizeWith(next.setter)

        return result
    }

    private sealed class ConstCommonizationState {
        object NonConst : ConstCommonizationState()

        abstract class Const : ConstCommonizationState()

        class ConstSameValue(val compileTimeInitializer: CirConstantValue) : Const() {
            init {
                check(compileTimeInitializer != CirConstantValue.NullValue)
            }
        }

        class ConstMultipleValues : Const()
    }
}
