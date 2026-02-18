/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.lombok.k2.config.lombokService

class DeclarationWithValueAnnStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return declaration is FirJavaField || declaration is FirJavaClass
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        field: FirField,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return if (containingClass == null ||
            status.visibility != JavaVisibilities.PackageVisibility ||
            session.lombokService.getValue(containingClass) == null
        ) {
            status
        } else {
            status.copy(visibility = Visibilities.Private)
        }
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        regularClass: FirRegularClass,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        return if (session.lombokService.getValue(regularClass.symbol) != null &&
            status.modality != Modality.FINAL
        ) {
            status.copy(modality = Modality.FINAL)
        } else {
            status
        }
    }
}
