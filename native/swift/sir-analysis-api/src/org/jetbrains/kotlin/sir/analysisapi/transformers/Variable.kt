/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.analysisapi.transformers

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildForeignVariable

internal fun KtVariableDeclaration.toForeignVariable(): SirForeignVariable = buildForeignVariable {
    origin = AAVariable(this@toForeignVariable)
}

private class AAVariable(
    private val original: KtVariableDeclaration
) : SirKotlinOrigin.Property {
    override val fqName: FqName
        get() = original.fqName ?: FqName.fromSegments(emptyList())

    override val type: SirKotlinOrigin.Type
        get() = analyze(original) {
            AAKotlinType(name = original.getReturnKtType().toString())
        }

    override val isWriteable: Boolean
        get() = original.isVar
}