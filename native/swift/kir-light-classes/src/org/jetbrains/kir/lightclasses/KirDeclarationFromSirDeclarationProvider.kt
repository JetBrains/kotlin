/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kir.lightclasses

import org.jetbrains.kir.lightclasses.nodes.KirFunctionFromSirFunction
import org.jetbrains.kotlin.kir.providers.KirDeclarationProvider
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.KirTranslationResult
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirFunction

public class KirDeclarationFromSirDeclarationProvider(
    private val kirSession: KirSession,
) : KirDeclarationProvider {
    public override fun SirDeclaration.toKir(): KirTranslationResult =
        when (val sirDeclaration = this@toKir) {
            is SirFunction -> KirFunctionFromSirFunction(
                sirDeclaration = sirDeclaration,
                kirSession = kirSession,
            ).let(KirTranslationResult::RegularFunction)
            else -> KirTranslationResult.Untranslatable(sirDeclaration)
        }
}
