/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirType

class SirNominalType(
    var declRef: SirDeclarationReference,
) : SirType {
    constructor(decl: SirNamedDeclaration) : this(DiscoveredDeclaration(decl))

    val type: SirNamedDeclaration
        get() = (declRef as DiscoveredDeclaration).decl
}

// TODO: Protocols. For now, only `any Any` is supported
data object SirExistentialType : SirType

sealed interface SirDeclarationReference

@JvmInline
value class DiscoveredDeclaration(val decl: SirNamedDeclaration) : SirDeclarationReference

class UnknownDeclaration(val origin: SirOrigin) : SirDeclarationReference
