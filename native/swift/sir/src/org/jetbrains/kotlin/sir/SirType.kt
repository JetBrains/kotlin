/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

data class SirType(var reference: SirTypeReference) {
    val declaration: SirNamedDeclaration
        get() =
            (reference as? SirTypeReference.Resolved.SirNominalType)?.declaration
                ?: throw IllegalArgumentException("trying to get declaration of type ${this} that is not resolved or that is not nominal")

    companion object {
        fun Unresolved(origin: SirOrigin): SirType = SirType(SirTypeReference.Unresolved(origin))
    }
}

fun SirNominalType(declaration: SirNamedDeclaration): SirType = SirType(SirTypeReference.Resolved.SirNominalType(declaration))

sealed interface SirTypeReference {
    sealed interface Resolved : SirTypeReference {
        class SirNominalType(
            var declaration: SirNamedDeclaration,
        ) : Resolved

        // TODO: Protocols. For now, only `any Any` is supported
        data object SirExistentialType : Resolved

    }

    class Unresolved(val origin: SirOrigin) : SirTypeReference
}
