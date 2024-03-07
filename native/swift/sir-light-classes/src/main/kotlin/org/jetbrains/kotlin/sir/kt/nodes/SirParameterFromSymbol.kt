/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.sir.SirParameter
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.kt.SirSession

public class SirParameterFromSymbol(
    private val ktSymbol: KtValueParameterSymbol,
    public override val ktAnalysisSession: KtAnalysisSession,
    public override val sirSession: SirSession,
) : SirParameter, SirDeclarationFromSymbol {
    override val argumentName: String by lazy {
        ktSymbol.name.asString()
    }
    override val type: SirType by lazy {
        withTranslationContext {
            ktSymbol.returnType.translateType()
        }
    }
}