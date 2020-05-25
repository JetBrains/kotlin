/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirProperty
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPropertyFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirPropertyGetterFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache

class PropertyCommonizer(cache: CirClassifiersCache) : AbstractFunctionOrPropertyCommonizer<CirProperty>(cache) {
    private val setter = PropertySetterCommonizer()
    private var isExternal = true

    override fun commonizationResult(): CirProperty {
        val setter = setter.result

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
            isConst = false,
            isDelegate = false,
            getter = CirPropertyGetterFactory.DEFAULT_NO_ANNOTATIONS,
            setter = setter,
            backingFieldAnnotations = null,
            delegateFieldAnnotations = null,
            compileTimeInitializer = null
        )
    }

    override fun doCommonizeWith(next: CirProperty): Boolean {
        when {
            next.isConst -> return false // expect property can't be const because expect can't have initializer
            next.isLateInit -> return false // expect property can't be lateinit
        }

        val result = super.doCommonizeWith(next)
                && setter.commonizeWith(next.setter)

        if (result) {
            isExternal = isExternal && next.isExternal
        }

        return result
    }
}
