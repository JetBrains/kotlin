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
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toConeTypeProjection
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

private fun FirSimpleFunctionBuilder.buildGetKindFunctionCommon(
    session: FirSession,
    owner: FirClassSymbol<*>,
    annotationCall: FirAnnotation,
    key: GeneratedDeclarationKey) {
    val enumType = annotationCall.typeArguments[0].toConeTypeProjection().type!!

    resolvePhase = FirResolvePhase.BODY_RESOLVE
    moduleData = session.moduleData
    origin = key.origin
    returnTypeRef = buildResolvedTypeRef {
        type = enumType
    }
    name = FqnUtils.Kind.GET_KIND_FUNCTION_NAME
    symbol = FirNamedFunctionSymbol(CallableId(owner.classId, FqnUtils.Kind.GET_KIND_FUNCTION_NAME))
    dispatchReceiverType = owner.defaultType()
}

class AddAbstractGetKindFunctionExtension(session: FirSession): AbstractMemberGenerationFirExtension(session) {
    override val classAnnotationFqn: AnnotationFqn = FqnUtils.Kind.WITH_ABSTRACT_KIND_ANNOTATION_FQN

    override val generatedFunctionNames = listOf(FqnUtils.Kind.GET_KIND_FUNCTION_NAME)
    override val generatedPropertyNames = emptyList<Name>()

    override fun generateFunctions(owner: FirClassSymbol<*>, annotationCall: FirAnnotation): List<FirNamedFunctionSymbol> {
        val getKindFun = buildSimpleFunction {
            buildGetKindFunctionCommon(session, owner, annotationCall, Key)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.ABSTRACT,
                EffectiveVisibility.Public
            )
        }
        return listOf(getKindFun.symbol)
    }

    object Key: GeneratedDeclarationKey() {
        override fun toString() = "AddAbstractGetKindFunctionExtensionKey"
    }
}

class AddGetKindFunctionExtension(session: FirSession): AbstractMemberGenerationFirExtension(session) {
    override val classAnnotationFqn: AnnotationFqn = FqnUtils.Kind.WITH_KIND_ANNOTATION_FQN
    override val generatedFunctionNames: List<Name> = listOf(FqnUtils.Kind.GET_KIND_FUNCTION_NAME)
    override val generatedPropertyNames: List<Name> = listOf()

    override fun generateFunctions(owner: FirClassSymbol<*>, annotationCall: FirAnnotation): List<FirNamedFunctionSymbol> {
        val getKindFun = buildSimpleFunction {
            buildGetKindFunctionCommon(session, owner, annotationCall, Key)
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
        }

        return listOf(getKindFun.symbol)
    }

    object Key: GeneratedDeclarationKey() {
        override fun toString() = "AddGetKindFunctionExtensionKey"
    }
}