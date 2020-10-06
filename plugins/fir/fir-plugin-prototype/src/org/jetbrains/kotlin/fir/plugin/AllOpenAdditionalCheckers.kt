/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.plugin.checkers.DummyNameChecker

class AllOpenAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val key: FirPluginKey
        get() = AllOpenPluginKey

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val memberDeclarationCheckers: Set<FirMemberDeclarationChecker> = setOf(DummyNameChecker)
    }
}
