/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.PhasedFirFileResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver

internal class IdePhasedFirFileResolver(
    private val lazyDeclarationResolver: FirLazyDeclarationResolver,
    private val cache: ModuleFileCache
) : PhasedFirFileResolver() {
    override fun resolveDeclaration(declaration: FirDeclaration, fromPhase: FirResolvePhase, toPhase: FirResolvePhase) {
        lazyDeclarationResolver.lazyResolveDeclaration(declaration, cache, toPhase, checkPCE = false)
    }
}