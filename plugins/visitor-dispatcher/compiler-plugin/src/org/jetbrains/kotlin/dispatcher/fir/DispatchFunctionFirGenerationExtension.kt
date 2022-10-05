/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.common.AbstractMemberGenerationFirExtension
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.dispatcher.common.FqnUtils
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildBlock
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.annotated
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

abstract class AddDispatchFunctionExtensionBase(
    session: FirSession,
    override val classAnnotationFqn: AnnotationFqn,
    private val dispatchFunctionModality: Modality,
    private val key: GeneratedDeclarationKey
): AbstractMemberGenerationFirExtension(session) {
    override val generatedFunctionNames = listOf(FqnUtils.DispatchedVisitor.DISPATCH_FUNCTION_NAME)
    override val generatedPropertyNames = listOf<Name>()

    private val withAbstractKindPredicate = annotated(FqnUtils.Kind.WITH_ABSTRACT_KIND_ANNOTATION_FQN)

    override fun generateFunctions(owner: FirClassSymbol<*>, annotationCall: FirAnnotation): List<FirNamedFunctionSymbol> {
        // val nodeType = annotationCall.typeArguments[0].toConeTypeProjection().type!!
        // TODO: filter by "is subclass of nodeType"
        val dispatchedPoints = session.predicateBasedProvider.getSymbolsByPredicate(withAbstractKindPredicate).map {
            val type = ConeClassLikeTypeImpl((it as FirClassLikeSymbol).toLookupTag(), emptyArray(), false)
            buildDispatchFunction(owner, type).symbol
        }
        return dispatchedPoints
    }

    private fun buildDispatchFunction(
        owner: FirClassSymbol<*>,
        nodeType: ConeKotlinType
    ): FirSimpleFunction {
        return buildSimpleFunction {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = key.origin
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                dispatchFunctionModality,
                EffectiveVisibility.Public
            )
            returnTypeRef = session.builtinTypes.unitType
            name = FqnUtils.DispatchedVisitor.DISPATCH_FUNCTION_NAME
            symbol = FirNamedFunctionSymbol(CallableId(owner.classId, FqnUtils.DispatchedVisitor.DISPATCH_FUNCTION_NAME))
            dispatchReceiverType = owner.defaultType()
            valueParameters.add(
                buildValueParameter {
                    moduleData = session.moduleData
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    origin = key.origin
                    returnTypeRef = buildResolvedTypeRef {
                        type = nodeType
                    }
                    name = FqnUtils.DispatchedVisitor.DISPATCH_FUNCTION_NODE_ARGUMENT_NAME
                    symbol = FirValueParameterSymbol(name)
                    isCrossinline = false
                    isNoinline = false
                    isVararg = false
                }
            )
            body = buildBlock {  }
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerAdditionalPredicated() {
        register(withAbstractKindPredicate)
    }
}

class AddAbstractDispatchFunctionExtension(session: FirSession): AddDispatchFunctionExtensionBase(
    session,
    FqnUtils.DispatchedVisitor.DISPATCHED_VISITOR_ANNOTATION_FQN,
    Modality.ABSTRACT,
    Key
) {
    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "DispatchAbstractFunctionKey"
        }
    }
}

class AddDispatchFunctionExtension(session: FirSession): AddDispatchFunctionExtensionBase(
    session,
    FqnUtils.DispatchedVisitor.GENERATE_DISPATCH_FUNCTION_FQN,
    Modality.FINAL,
    Key
) {
    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "DispatchFunctionKey"
        }
    }
}