/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.tree.deserializer

import kotlinx.metadata.KmProperty
import org.jetbrains.kotlin.commonizer.cir.CirContainingClass
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirProperty
import org.jetbrains.kotlin.commonizer.metadata.CirDeserializers
import org.jetbrains.kotlin.commonizer.metadata.CirTypeResolver
import org.jetbrains.kotlin.commonizer.utils.isFakeOverride

object CirTreePropertyDeserializer {
    operator fun invoke(property: KmProperty, containingClass: CirContainingClass?, typeResolver: CirTypeResolver): CirProperty? {
        if (property.isFakeOverride()) return null
        val propertyTypeResolver = typeResolver.create(property.typeParameters)
        return CirDeserializers.property(
            name = CirName.create(property.name),
            source = property,
            containingClass = containingClass,
            typeResolver = propertyTypeResolver
        )
    }
}
