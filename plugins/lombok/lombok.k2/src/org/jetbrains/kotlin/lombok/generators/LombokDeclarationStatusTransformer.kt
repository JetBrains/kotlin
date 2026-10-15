/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

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
import org.jetbrains.kotlin.fir.declarations.getCompilerRequiredAnnotationByClassId
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.lombokService

class LombokDeclarationStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    private val requiredArgsConstructorPart = RequiredArgsConstructorGeneratorPart(session)

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return declaration is FirJavaField || declaration is FirJavaClass || declaration is FirJavaConstructor ||
                declaration is FirConstructor && declaration.isPrimary
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        field: FirField,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus {
        if (containingClass == null) return status
        val lombokService = session.lombokService
        val fieldDefaults = lombokService.getFieldDefaults(containingClass)
        val visibility = when {
            status.visibility != JavaVisibilities.PackageVisibility -> null
            status.isStatic -> null
            field.symbol.getCompilerRequiredAnnotationByClassId(LombokNames.PACKAGE_PRIVATE_ID) != null -> null
            fieldDefaults != null && fieldDefaults.level != AccessLevel.NONE -> fieldDefaults.level.toVisibility(field.symbol)
            session.lombokService.config.fieldDefaultPrivate -> Visibilities.Private
            session.lombokService.getValue(containingClass) == null -> null
            else -> Visibilities.Private
        } ?: return status
        return status.copy(visibility = visibility)
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
        if (containingClass == null) return status

        val isHidden = if (constructor is FirJavaConstructor) {
            // Only hide synthetic constructors (default no-args from Java analysis), not user-declared ones.
            constructor.source?.kind !is KtRealSourceElementKind && containingClass.hasConstructorHiddenByStaticFactory()
        } else {
            // The static factory of a Kotlin class delegates to its primary constructor, which it then stands in for,
            // as the constructor generated for a Java class does. Only once it is actually generated, though, or the
            // class could not be constructed at all.
            constructor.isPrimary && containingClass is FirClassSymbol<*> &&
                    requiredArgsConstructorPart.generatesStaticFactory(containingClass)
        }

        return if (isHidden) {
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
