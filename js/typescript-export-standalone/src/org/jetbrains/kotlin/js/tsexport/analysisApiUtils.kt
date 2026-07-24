/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingDeclaration
import org.jetbrains.kotlin.analysis.api.types.KaType

context(_: KaSession)
internal val KaDeclarationSymbol.parentDeclarationsWithSelf: Sequence<KaDeclarationSymbol>
    get() = generateSequence(this) { it.containingDeclaration }

context(_: KaSession)
internal val KaClassSymbol.inlineClassUnderlyingType: KaType?
    get() {
        val memberScope = combinedDeclaredMemberScope
        val primaryConstructor = memberScope.constructors.firstOrNull { it.isPrimary }
        val valueField = primaryConstructor?.valueParameters?.singleOrNull()

        return valueField?.returnType
    }
