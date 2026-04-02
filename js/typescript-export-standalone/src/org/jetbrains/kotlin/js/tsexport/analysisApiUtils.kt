/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaContextParameterApi::class)
package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.components.containingDeclaration
import org.jetbrains.kotlin.analysis.api.components.memberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

context(_: KaSession)
internal val KaDeclarationSymbol.parentDeclarationsWithSelf: Sequence<KaDeclarationSymbol>
    get() = generateSequence(this) { it.containingDeclaration }

context(_: KaSession)
internal val KaClassSymbol.singleFieldValueClassUnderlyingType: KaType?
    get() {
        val memberScope = combinedDeclaredMemberScope
        val primaryConstructor = memberScope.constructors.firstOrNull { it.isPrimary }
        val valueField = primaryConstructor?.valueParameters?.singleOrNull()

        return valueField?.returnType
    }
