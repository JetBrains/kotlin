/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.hasOrUnder
import org.jetbrains.kotlin.fir.extensions.predicate.metaHasOrUnder
import org.jetbrains.kotlin.fir.extensions.predicate.or
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.name.FqName

class AllOpenStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    companion object {
        private val ALL_OPEN = FqName("org.jetbrains.kotlin.fir.plugin.AllOpen")
    }

    override fun transformStatus(declaration: FirDeclaration<*>, owners: List<FirAnnotatedDeclaration<*>>, status: FirDeclarationStatus): FirDeclarationStatus {
        if (status.modality != null) return status
        return status.transform(modality = Modality.OPEN)
    }

    override val predicate: DeclarationPredicate = hasOrUnder(ALL_OPEN) or metaHasOrUnder(ALL_OPEN)

    override val key: FirPluginKey
        get() = AllOpenPluginKey
}
