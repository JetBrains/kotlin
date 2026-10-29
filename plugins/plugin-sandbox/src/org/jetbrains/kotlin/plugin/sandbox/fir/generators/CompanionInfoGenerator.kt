/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getCompilerRequiredAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.resolved
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.unwrapAndFlattenArgument
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension.CompanionObjectInfo
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class CompanionInfoGenerator(
    session: FirSession,
    private val annotationClassId: ClassId,
) : FirDeclarationGenerationExtension(session) {
    private val annotationPredicate = LookupPredicate.create { annotated(annotationClassId.asSingleFqName()) }

    override fun needsCompanionObject(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Boolean =
        session.predicateBasedProvider.matches(annotationPredicate, classSymbol)

    override fun getCompanionObjectInfo(
        owner: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): CompanionObjectInfo = parseCompanionSpecification(owner, annotationClassId)

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(annotationPredicate)
    }
}

private val SPECIFICATION_ARGUMENT: Name = Name.identifier("spec")

/**
 * Builds the [CompanionObjectInfo] for [owner] from the `spec` argument of the [annotationClassId] annotation.
 */
internal fun parseCompanionSpecification(
    owner: FirClassSymbol<*>,
    annotationClassId: ClassId,
): CompanionObjectInfo {
    val annotation = owner.getCompilerRequiredAnnotationByClassId(annotationClassId)
    // Arguments of unresolved annotations are not mapped yet, and `findArgumentByName` returns only the first vararg element.
    val arguments = when {
        annotation is FirAnnotationCall && !annotation.resolved -> annotation.argumentList.arguments
        else -> listOfNotNull(annotation?.findArgumentByName(SPECIFICATION_ARGUMENT))
    }
    val specifications = arguments
        .flatMap { it.unwrapAndFlattenArgument(flattenArrays = true) }
        .mapNotNull { (it as? FirLiteralExpression)?.value as? String }

    val [annotationSpecifications, supertypeSpecifications] = specifications.partition { it.startsWith("@") }
    return CompanionObjectInfo(
        superTypes = supertypeSpecifications.map { SupertypeSpecificationParser(it).parse() },
        annotations = annotationSpecifications.map { createAnnotation(it.removePrefix("@")) },
    )
}

private fun createAnnotation(classIdText: String): FirAnnotation {
    val classId = ClassId.fromString(classIdText)
    return buildAnnotation {
        argumentMapping = FirEmptyAnnotationArgumentMapping
        annotationTypeRef = classId.constructClassLikeType().toFirResolvedTypeRef()
    }
}

/**
 * Parses one supertype specification, for example `com/example/Provider<kotlin/String>`.
 * The specification holds a [ClassId] with optional type arguments.
 */
private class SupertypeSpecificationParser(
    private val specification: String,
) {
    private var index = 0

    fun parse(): ConeClassLikeType {
        val type = parseType()
        require(index == specification.length) { "Unexpected trailing characters in type specification '$specification'" }
        return type
    }

    private fun parseType(): ConeClassLikeType {
        val nameStart = index
        while (index < specification.length && specification[index] !in "<>,?") index++
        val name = specification.substring(nameStart, index)
        require(name.isNotEmpty()) { "Expected a type name in type specification '$specification'" }

        val arguments = if (consume('<')) parseTypeArguments() else emptyArray()
        val nullable = consume('?')
        return ClassId.fromString(name).constructClassLikeType(arguments, nullable)
    }

    private fun parseTypeArguments(): Array<ConeTypeProjection> = buildList {
        do {
            add(if (consume('*')) ConeStarProjection else parseType())
        } while (consume(','))
        require(consume('>')) { "Expected '>' in type specification '$specification'" }
    }.toTypedArray()

    private fun consume(char: Char): Boolean {
        if (index >= specification.length || specification[index] != char) return false
        index++
        return true
    }
}

class AddToCompanionGenerator(session: FirSession) : CompanionInfoGenerator(
    session,
    ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/AddToCompanion"),
)

class AlsoAddToCompanionGenerator(session: FirSession) : CompanionInfoGenerator(
    session,
    ClassId.fromString("org/jetbrains/kotlin/plugin/sandbox/AlsoAddToCompanion"),
)
