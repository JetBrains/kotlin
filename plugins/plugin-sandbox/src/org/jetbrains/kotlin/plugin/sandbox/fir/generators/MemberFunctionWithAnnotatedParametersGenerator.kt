/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.impl.toAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.getPrimaryConstructorSymbol
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class MemberFunctionWithAnnotatedParametersGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        val FOO_NAME = Name.identifier("foo")
        val ANNOTATION_CLASS_ID = ClassId.topLevel("AnnotationWithStringValue".fqn())

        private val PREDICATE = LookupPredicate.create {
            annotated("GenerateFunctionWithAnnotatedParameters".fqn())
        }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context == null) return emptyList()
        if (callableId.callableName != FOO_NAME) return emptyList()
        val function = createMemberFunction(context.owner, Key, callableId.callableName, session.builtinTypes.unitType.coneType) {
            valueParameter(
                name = Name.identifier("x"),
                type = session.builtinTypes.intType.coneType,
            )
            valueParameter(
                name = Name.identifier("y"),
                type = session.builtinTypes.intType.coneType,
            )
            withGeneratedDefaultBody()
        }

        val annotationClass = session.symbolProvider.getClassLikeSymbolByClassId(ANNOTATION_CLASS_ID)!!
        val constructorSymbol = annotationClass.getPrimaryConstructorSymbol(session, ScopeSession())!!
        function.valueParameters.forEachIndexed { index, parameter ->
            val annotation = buildAnnotationCall {
                source = context.owner.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)
                annotationTypeRef = annotationClass.defaultType().toFirResolvedTypeRef()
                calleeReference = buildResolvedNamedReference {
                    name = annotationClass.name
                    resolvedSymbol = constructorSymbol
                }
                val mapping = constructorSymbol.valueParameterSymbols.associate {
                    val rawValue = when (index) {
                        0 -> "A"
                        1 -> "B"
                        else -> shouldNotBeCalled()
                    }
                    val value: FirExpression = buildLiteralExpression(
                        source = null,
                        kind = ConstantValueKind.String,
                        value = rawValue,
                        setType = true,
                    )
                    @OptIn(SymbolInternals::class)
                    value to it.fir
                }
                val resolvedArgumentList = buildResolvedArgumentList(
                    original = null,
                    LinkedHashMap(mapping)
                )
                argumentList = resolvedArgumentList
                argumentMapping = resolvedArgumentList.toAnnotationArgumentMapping()
                annotationResolvePhase = FirAnnotationResolvePhase.Types
                containingDeclarationSymbol = parameter.symbol
            }
            parameter.replaceAnnotations(listOf(annotation))
        }

        return listOf(function.symbol)
    }


    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return when {
            classSymbol in matchedClasses -> setOf(FOO_NAME)
            else -> emptySet()
        }
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "MemberFunctionWithAnnotatedParametersGeneratorKey"
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
