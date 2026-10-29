/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.generators.kotlin.isCompanionNeeded

/**
 * A generator that puts members into the companion object of the class it generates for - `@Log` its logger,
 * `@NoArgsConstructor` its static factory, `@Builder` its `builder()` - and so needs one where the class declares
 * none.
 */
abstract class LombokCompanionObjectGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    /** Whether [owner] needs a generated companion object for what this generator would put into it. */
    abstract fun needsCompanionObject(owner: FirClassSymbol<*>): Boolean

    final override fun needsCompanionObject(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Boolean =
        isCompanionNeeded(classSymbol, context) && needsCompanionObject(classSymbol)
}
