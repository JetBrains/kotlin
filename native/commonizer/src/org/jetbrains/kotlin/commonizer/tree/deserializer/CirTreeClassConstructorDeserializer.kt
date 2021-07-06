/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import kotlinx.metadata.KmConstructor
import org.jetbrains.kotlin.commonizer.cir.CirClassConstructor
import org.jetbrains.kotlin.commonizer.cir.CirContainingClass
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver

internal object CirTreeClassConstructorDeserializer {
    operator fun invoke(
        constructor: KmConstructor, containingClass: CirContainingClass, typeResolver: CirTypeResolver
    ): CirClassConstructor {
        return CirDeserializers.constructor(
            source = constructor,
            containingClass = containingClass,
            typeResolver = typeResolver
        )
    }
}
