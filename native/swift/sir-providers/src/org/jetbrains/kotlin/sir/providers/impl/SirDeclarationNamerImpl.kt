/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.providers.SirDeclarationNamer

public class SirDeclarationNamerImpl : SirDeclarationNamer {

    override fun KaDeclarationSymbol.sirDeclarationName(): String {
        return getName() ?: error("could not retrieve a name for $this")
    }

    private fun KaDeclarationSymbol.getName(): String? {
        return when (this) {
            is KaNamedClassOrObjectSymbol -> this.classId?.shortClassName
            is KaFunctionLikeSymbol -> this.callableId?.callableName
            is KaVariableSymbol -> this.callableId?.callableName
            else -> error(this)
        }?.asString()
    }
}
