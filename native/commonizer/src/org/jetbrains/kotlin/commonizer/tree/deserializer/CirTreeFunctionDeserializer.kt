/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import kotlinx.metadata.KmFunction
import org.jetbrains.kotlin.commonizer.cir.CirClass
import org.jetbrains.kotlin.commonizer.cir.CirContainingClass
import org.jetbrains.kotlin.commonizer.cir.CirFunction
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.utils.isFakeOverride
import org.jetbrains.kotlin.commonizer.utils.isKniBridgeFunction
import org.jetbrains.kotlin.commonizer.utils.isTopLevelDeprecatedFunction

object CirTreeFunctionDeserializer {
    operator fun invoke(function: KmFunction, containingClass: CirContainingClass?, typeResolver: CirTypeResolver): CirFunction? {
        val functionTypeResolver = typeResolver.create(function.typeParameters)
        if (function.isFakeOverride()
            || function.isKniBridgeFunction()
            || function.isTopLevelDeprecatedFunction(isTopLevel = containingClass !is CirClass)
        ) {
            return null
        }

        return CirDeserializers.function(
            name = CirName.create(function.name),
            source = function,
            containingClass = containingClass,
            typeResolver = functionTypeResolver
        )
    }
}
