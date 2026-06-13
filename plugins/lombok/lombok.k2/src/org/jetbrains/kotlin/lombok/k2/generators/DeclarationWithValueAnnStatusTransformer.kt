/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.lombok.k2.config.lombokService

class DeclarationWithValueAnnStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return declaration is FirJavaField || declaration is FirJavaClass || declaration is FirJavaConstructor
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        field: FirField,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
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
        isLocal: Boolean,
    ): FirDeclarationStatus {
        return if (session.lombokService.getValue(regularClass.symbol) != null &&
            status.modality != Modality.FINAL
        ) {
            status.copy(modality = Modality.FINAL)
        } else {
            status
        }
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        constructor: FirConstructor,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus {
        if (containingClass == null ||
            // Only hide synthetic constructors (default no-args from Java analysis), not user-declared ones.
            constructor.source?.kind is KtRealSourceElementKind
        ) {
            return status
        }

        return if (containingClass.hasConstructorHiddenByStaticFactory()) {
            status.copy(visibility = Visibilities.Private)
        } else {
            status
        }
    }

    private fun FirClassLikeSymbol<*>.hasConstructorHiddenByStaticFactory(): Boolean {
        val lombokService = session.lombokService
        return lombokService.getRequiredArgsConstructor(this)?.staticName != null ||
                lombokService.getAllArgsConstructor(this)?.staticName != null ||
                lombokService.getNoArgsConstructor(this)?.staticName != null ||
                lombokService.getData(this)?.staticConstructor != null ||
                lombokService.getValue(this)?.staticConstructor != null
    }
}
