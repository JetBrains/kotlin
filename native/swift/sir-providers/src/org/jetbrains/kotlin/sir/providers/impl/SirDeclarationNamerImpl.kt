/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.providers.SirDeclarationNamer

public class SirDeclarationNamerImpl : SirDeclarationNamer {

    override fun KtDeclarationSymbol.sirDeclarationName(): String {
        return getName() ?: error("could not retrieve a name for $this")
    }

    private fun KtDeclarationSymbol.getName(): String? {
        return when (this) {
            is KtNamedClassOrObjectSymbol -> this.classIdIfNonLocal?.shortClassName
            is KtFunctionLikeSymbol -> this.callableIdIfNonLocal?.callableName
            is KtVariableSymbol -> this.callableIdIfNonLocal?.callableName
            else -> error(this)
        }?.asString()
    }
}
