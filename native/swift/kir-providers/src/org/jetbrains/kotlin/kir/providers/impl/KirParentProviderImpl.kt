/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.impl

import org.jetbrains.kotlin.kir.KirDeclarationParent
import org.jetbrains.kotlin.kir.providers.KirParentProvider
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.kirModule
import org.jetbrains.kotlin.kir.providers.withSession
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModule

public class KirParentProviderImpl(private val kirSession: KirSession) : KirParentProvider {
    override fun SirDeclaration.getKirParent(): KirDeclarationParent = kirSession.withSession {
        when (val sirParent = this@getKirParent.parent) {
            is SirModule -> sirParent.kirModule()
            else -> error("Tried to calculate a KIR parent for a declaration with an unsupported parent: $sirParent")
        }
    }
}
