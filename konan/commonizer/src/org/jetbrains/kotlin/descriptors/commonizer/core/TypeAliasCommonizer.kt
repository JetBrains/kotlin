/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.*
import org.jetbrains.kotlin.name.Name

class TypeAliasCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirTypeAlias, CirClass>() {
    private lateinit var name: Name
    private val underlyingType = TypeCommonizer.default(cache)
    private val visibility = VisibilityCommonizer.lowering(allowPrivate = true)

    override fun commonizationResult() = CirCommonClass(
        name = name,
        typeParameters = emptyList(),
        kind = ClassKind.CLASS,
        modality = Modality.FINAL,
        visibility = visibility.result,
        isCompanion = false,
        isInline = false,
        isInner = false
    )

    override fun initialize(first: CirTypeAlias) {
        name = first.name
    }

    override fun doCommonizeWith(next: CirTypeAlias) =
        next.typeParameters.isEmpty() // TAs with declared type parameters can't be commonized
                && next.underlyingType.arguments.isEmpty() // TAs with functional types or types with parameters at the right-hand side can't be commonized
                && next.underlyingType.kind == CirSimpleTypeKind.CLASS // right-hand side could have only class
                && underlyingType.commonizeWith(next.underlyingType)
                && visibility.commonizeWith(next)
}
