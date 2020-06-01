/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirProperty
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPropertyFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPropertyGetterFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache
import org.jetbrains.kotlin.resolve.constants.ConstantValue

class PropertyCommonizer(cache: CirClassifiersCache) : AbstractFunctionOrPropertyCommonizer<CirProperty>(cache) {
    private val setter = PropertySetterCommonizer()
    private var isExternal = true
    private var constCompileTimeInitializer: ConstantValue<*>? = null

    override fun commonizationResult(): CirProperty {
        val setter = setter.result
        val constCompileTimeInitializer = constCompileTimeInitializer

        return CirPropertyFactory.create(
            annotations = emptyList(),
            name = name,
            typeParameters = typeParameters.result,
            visibility = visibility.result,
            modality = modality.result,
            containingClassDetails = null,
            isExternal = isExternal,
            extensionReceiver = extensionReceiver.result,
            returnType = returnType.result,
            kind = kind,
            isVar = setter != null,
            isLateInit = false,
            isConst = constCompileTimeInitializer != null,
            isDelegate = false,
            getter = CirPropertyGetterFactory.DEFAULT_NO_ANNOTATIONS,
            setter = setter,
            backingFieldAnnotations = null,
            delegateFieldAnnotations = null,
            compileTimeInitializer = constCompileTimeInitializer
        )
    }

    override fun initialize(first: CirProperty) {
        super.initialize(first)

        if (first.isConst) {
            constCompileTimeInitializer = first.compileTimeInitializer
        }
    }

    override fun doCommonizeWith(next: CirProperty): Boolean {
        if (next.isLateInit) {
            // expect property can't be lateinit
            return false
        }

        val constCompileTimeInitializer = constCompileTimeInitializer
        if (next.isConst) {
            // const properties should be lifted up
            // otherwise commonization should fail: expect property can't be const because expect can't have initializer

            if (constCompileTimeInitializer == null || constCompileTimeInitializer != next.compileTimeInitializer) {
                // previous property was not constant or const properties have different constants
                return false
            }
        } else if (constCompileTimeInitializer != null) {
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
}
