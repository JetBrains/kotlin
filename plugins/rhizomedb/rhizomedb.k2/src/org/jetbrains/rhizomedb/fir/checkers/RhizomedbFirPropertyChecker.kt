/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.rhizomedb.fir.KotlinStdlib
import org.jetbrains.rhizomedb.fir.RhizomedbAnnotations
import org.jetbrains.rhizomedb.fir.checkers.RhizomedbFirErrors.WRONG_ATTRIBUTE_TARGET
import org.jetbrains.rhizomedb.fir.extensions.rhizomedbPredicateMatcher
import org.jetbrains.rhizomedb.fir.getManyAnnotation

object RhizomedbFirPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        checkManyAnnotation(declaration, context, reporter)
        checkAttributeAnnotation(declaration, context, reporter)
    }

    private fun checkManyAnnotation(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val manyAnnotation = declaration.getManyAnnotation(session) ?: return
        val predicateMatcher = session.rhizomedbPredicateMatcher
        if (!predicateMatcher.isAttributeAnnotated(declaration.symbol)) {
            reporter.reportOn(manyAnnotation.source, RhizomedbFirErrors.NON_ATTRIBUTE, context)
        }
        if (!declaration.returnTypeRef.isSet()) {
            reporter.reportOn(manyAnnotation.source, RhizomedbFirErrors.MANY_NON_SET, context)
        }
    }

    private fun checkAttributeAnnotation(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val annotations = declaration.collectAttributeAnnotations(session)
        if (annotations.isEmpty()) {
            return
        }
        if (annotations.size > 1) {
            for ((annotation, _) in annotations) {
                reporter.reportOn(annotation.source, WRONG_ATTRIBUTE_TARGET, WrongAtttributeTarget.DUPLICATED_ATTRIBUTE, context)
            }
        } else {
            val predicateMatcher = session.rhizomedbPredicateMatcher
            val (annotation, kind) = annotations.single()
            if (!isAssociatedEntityTypeExists(declaration, session)) {
                reporter.reportOn(annotation.source, WRONG_ATTRIBUTE_TARGET, WrongAtttributeTarget.NO_ENTITY_TYPE, context)
            }

            val type = declaration.getTargetAttributeType(session)?.toRegularClassSymbol(session) ?: return
            val isEntity = predicateMatcher.isEntity(type)
            when (kind) {
                AttributeAnnotationKind.VALUE -> {
                    if (isEntity) {
                        reporter.reportOn(annotation.source, WRONG_ATTRIBUTE_TARGET, WrongAtttributeTarget.NOT_REF, context)
                    }

                    // TODO: find serializer
//                    if (type.findSerializer() == null) {
//                        reporter.reportOn(annotation.source, WRONG_ATTRIBUTE_TARGET, WrongAtttributeTarget.NO_SERIALIZER, context)
//                    }
                }
                AttributeAnnotationKind.TRANSIENT -> {
                    if (isEntity) {
                        reporter.reportOn(annotation.source, WRONG_ATTRIBUTE_TARGET, WrongAtttributeTarget.NOT_REF, context)
                    }
                }
                AttributeAnnotationKind.REF -> {
                    if (!isEntity) {
                        reporter.reportOn(annotation.source, WRONG_ATTRIBUTE_TARGET, WrongAtttributeTarget.NOT_ENTITY, context)
                    }
                }
            }
        }
    }

    private fun FirProperty.getTargetAttributeType(session: FirSession): ConeKotlinType? {
        val returnType = returnTypeRef
        return if (returnType is FirResolvedTypeRef && returnType.isSet() && session.rhizomedbPredicateMatcher.isManyAnnotated(symbol)) {
            returnType.coneType.typeArguments.singleOrNull()?.type ?: returnType.coneType
        } else {
            returnType.coneTypeOrNull
        }
    }

    private fun isAssociatedEntityTypeExists(declaration: FirProperty, session: FirSession): Boolean {
        val entity = declaration.getContainingClass() ?: return false
        val companion = entity.companionObjectSymbol ?: return false
        return session.rhizomedbPredicateMatcher.isEntityType(companion)
    }

    private fun FirProperty.collectAttributeAnnotations(session: FirSession): List<AttributeAnnotation> = buildList {
        getAnnotationByClassId(RhizomedbAnnotations.valueAttributeClassId, session)?.let {
            add(AttributeAnnotation(it, AttributeAnnotationKind.VALUE))
        }
        getAnnotationByClassId(RhizomedbAnnotations.transientAttributeClassId, session)?.let {
            add(AttributeAnnotation(it, AttributeAnnotationKind.TRANSIENT))
        }
        getAnnotationByClassId(RhizomedbAnnotations.referenceAttributeClassId, session)?.let {
            add(AttributeAnnotation(it, AttributeAnnotationKind.REF))
        }
    }
}

enum class AttributeAnnotationKind {
    VALUE,
    TRANSIENT,
    REF,
}

private data class AttributeAnnotation(val element: FirAnnotation, val kind: AttributeAnnotationKind)

private fun FirTypeRef.isSet(): Boolean {
    val type = (this as? FirResolvedTypeRef)?.coneType ?: return false
    return (type as? ConeClassLikeType)?.lookupTag?.classId == KotlinStdlib.setClassId && !type.isNullable
}