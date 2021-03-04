/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirConstantValue
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirProperty
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPropertyGetter
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPropertyFactory
import org.jetbrains.kotlin.descriptors.commonizer.core.PropertyCommonizer.ConstCommonizationState.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirKnownClassifiers

class PropertyCommonizer(classifiers: CirKnownClassifiers) : AbstractFunctionOrPropertyCommonizer<CirProperty>(classifiers) {
    private val setter = PropertySetterCommonizer()
    private var isExternal = true
    private lateinit var constCommonizationState: ConstCommonizationState

    override fun commonizationResult(): CirProperty {
        val setter = setter.result

        val constCommonizationState = constCommonizationState
        val constCompileTimeInitializer = (constCommonizationState as? ConstSameValue)?.compileTimeInitializer

        if (constCommonizationState is ConstMultipleValues) {
            // fix all commonized properties to make then non-const
            constCommonizationState.properties.forEach { it.isConst = false }
        }

        return CirPropertyFactory.create(
            annotations = emptyList(),
            name = name,
            typeParameters = typeParameters.result,
            visibility = visibility.result,
            modality = modality.result,
            containingClass = null, // does not matter
            isExternal = isExternal,
            extensionReceiver = extensionReceiver.result,
            returnType = returnType.result,
            kind = kind,
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
        super.initialize(first)

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
                    return false
                }
                is Const -> {
                    // previous property was constant
                    constCommonizationState.properties += next

                    if (constCommonizationState is ConstSameValue) {
                        if (constCommonizationState.compileTimeInitializer != next.compileTimeInitializer) {
                            // const properties have different constants
                            this.constCommonizationState = ConstMultipleValues(constCommonizationState)
                        }
                    }
                }
            }
        } else if (constCommonizationState != NonConst) {
            // previous property was constant but this one is not
            return false
        }

        val result = super.doCommonizeWith(next)
                && setter.commonizeWith(next.setter)

        if (result) {
            isExternal = isExternal && next.isExternal
        }

        return result
    }

    private sealed class ConstCommonizationState {
        object NonConst : ConstCommonizationState()

        abstract class Const : ConstCommonizationState() {
            val properties: MutableList<CirProperty> = mutableListOf()
        }

        class ConstSameValue(val compileTimeInitializer: CirConstantValue<*>) : Const() {
            init {
                check(compileTimeInitializer != CirConstantValue.NullValue)
            }
        }

        class ConstMultipleValues(previous: ConstSameValue) : Const() {
            init {
                properties += previous.properties
            }
        }
    }
}
