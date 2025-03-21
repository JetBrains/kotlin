/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider

public class ObservingSirDeclarationProvider(
    private val declarationsProvider: SirDeclarationProvider,
    private val kaClassReferenceHandler: SirKaClassReferenceHandler? = null,
) : SirDeclarationProvider {

    override fun KaDeclarationSymbol.toSir(): SirTranslationResult {
        if (this is KaClassLikeSymbol) {
            kaClassReferenceHandler?.onClassReference(this)
        }
        return with(declarationsProvider) { this@toSir.toSir() }
    }
}
