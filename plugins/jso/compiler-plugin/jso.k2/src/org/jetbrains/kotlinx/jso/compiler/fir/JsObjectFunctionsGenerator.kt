/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jso.compiler.fir

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.builder.createDataClassCopyFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.jso.compiler.fir.services.jsObjectPropertiesProvider
import org.jetbrains.kotlinx.jso.compiler.resolve.JsSimpleObjectPluginKey

/**
 * The extension generate a synthetic factory and copy-method for an `external interface` annotated with @JsSimpleObject
 * Imagine the next interfaces:
 * ```
 * external interface User {
 *   val name: String
 * }
 * @JsSimpleObject
 * external interface Admin {
 *   val chat: Chat
 * }
 * ```
 *
 * For the interface `Admin` this function should generate the companion inline function:
 * ```
 * external interface Admin {
 *   val chat: Chat
 *
 *  inline fun copy(chat: Chat = this.chat, name: String = this.name): Admin =
 *      Admin.Companion.invoke(chat, name)
 *
 *   companion object {
 *      inline operator fun invoke(chat: Chat, name: String): Admin =
 *          js("{ chat: chat, name: name }")
 *   }
 * }
 * ```
 */
class JsObjectFunctionsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val predicateBasedProvider = session.predicateBasedProvider

    private val matchedInterfaces by lazy {
        predicateBasedProvider.getSymbolsByPredicate(JsObjectPredicates.AnnotatedWithJsSimpleObject.LOOKUP)
            .filterIsInstance<FirRegularClassSymbol>()
            .toSet()
    }

    private val factoryFqNamesToJsObjectInterface by lazy {
        matchedInterfaces.associateBy { it.classId.asSingleFqName() }
    }

    private val FirClassLikeSymbol<*>.isJsObject: Boolean
        get() = this is FirRegularClassSymbol && this in matchedInterfaces

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return if (classSymbol.isJsObject) setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) else emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        return if (
            owner is FirRegularClassSymbol &&
            owner.isJsObject &&
            name == org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        ) generateCompanionDeclaration(owner)
        else null
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val classId = owner.classId.createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
        return buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = JsSimpleObjectPluginKey.origin
            classKind = ClassKind.OBJECT
            scopeProvider = session.kotlinScopeProvider
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                Visibilities.Public.toEffectiveVisibility(owner, forClass = true)
            ).apply {
                isExternal = true
                isCompanion = true
            }
            name = classId.shortClassName
            symbol = FirRegularClassSymbol(classId)
        }.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val outerClass = classSymbol.getContainingClassSymbol(session)
        return when {
            classSymbol.isCompanion && outerClass?.isJsObject == true -> setOf(OperatorNameConventions.INVOKE)
            classSymbol.isJsObject -> setOf(StandardNames.DATA_CLASS_COPY)
            else -> emptySet()
        }
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context == null) return emptyList()

        val containingClass = callableId.classId
        val possibleInterface = containingClass?.outerClassId

        return when (callableId.callableName) {
            StandardNames.DATA_CLASS_COPY -> {
                containingClass
                    ?.let { factoryFqNamesToJsObjectInterface[it.asSingleFqName()] }
                    ?.let { listOf(createJsObjectCopyFunction(callableId, context.owner, it).symbol) } ?: emptyList()
            }
            OperatorNameConventions.INVOKE -> {
                possibleInterface
                    ?.takeIf { context.owner.isCompanion }
                    ?.let { factoryFqNamesToJsObjectInterface[it.asSingleFqName()] }
                    ?.let { listOf(createJsObjectFactoryFunction(callableId, context.owner, it).symbol) } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun createJsObjectFactoryFunction(
        callableId: CallableId,
        parent: FirClassSymbol<*>,
        jsSimpleObjectInterface: FirRegularClassSymbol,
    ): FirSimpleFunction {
        return createJsObjectFunction(callableId, parent, jsSimpleObjectInterface) {
            runIf(resolvedReturnTypeRef.type.isNullable) {
                buildConstExpression(
                    source = null,
                    value = null,
                    kind = ConstantValueKind.Null,
                    setType = true
                )
            }
        }
    }

    private fun createJsObjectCopyFunction(
        callableId: CallableId,
        parent: FirClassSymbol<*>,
        jsSimpleObjectInterface: FirRegularClassSymbol,
    ): FirSimpleFunction {
        val interfaceType = jsSimpleObjectInterface.defaultType()
        return createJsObjectFunction(callableId, parent, jsSimpleObjectInterface) {
            buildPropertyAccessExpression {
                dispatchReceiver = buildThisReceiverExpression {
                    calleeReference = buildImplicitThisReference { boundSymbol = jsSimpleObjectInterface }
                    coneTypeOrNull = interfaceType
                }
                calleeReference = buildResolvedNamedReference {
                    name = this@createJsObjectFunction.name
                    resolvedSymbol = this@createJsObjectFunction
                }
                coneTypeOrNull = resolvedReturnType
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun createJsObjectFunction(
        callableId: CallableId,
        parent: FirClassSymbol<*>,
        jsSimpleObjectInterface: FirRegularClassSymbol,
        getParameterDefaultValueFromProperty: FirPropertySymbol.() -> FirExpression?
    ): FirSimpleFunction {
        val jsSimpleObjectProperties = session.jsObjectPropertiesProvider.getJsObjectPropertiesForClass(jsSimpleObjectInterface)
        val functionTarget = FirFunctionTarget(null, isLambda = false)
        val jsSimpleObjectInterfaceDefaultType = jsSimpleObjectInterface.defaultType()

        return buildSimpleFunction {
            moduleData = jsSimpleObjectInterface.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = JsSimpleObjectPluginKey.origin
            symbol = FirNamedFunctionSymbol(callableId)
            name = callableId.callableName
            returnTypeRef = jsSimpleObjectInterfaceDefaultType.toFirResolvedTypeRef()

            status = FirResolvedDeclarationStatusImpl(
                jsSimpleObjectInterface.visibility,
                Modality.FINAL,
                jsSimpleObjectInterface.visibility.toEffectiveVisibility(parent, forClass = true)
            ).apply {
                isInline = true
                isOperator = true
            }

            dispatchReceiverType = parent.defaultType()
            jsSimpleObjectInterface.typeParameterSymbols.mapTo(typeParameters) { it.fir }
            jsSimpleObjectProperties.mapTo(valueParameters) {
                val typeRef = it.resolvedReturnTypeRef
                buildValueParameter {
                    moduleData = session.moduleData
                    origin = JsSimpleObjectPluginKey.origin
                    returnTypeRef = typeRef
                    name = it.name
                    symbol = FirValueParameterSymbol(it.name)
                    isCrossinline = false
                    isNoinline = true
                    isVararg = false
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    containingFunctionSymbol = this@buildSimpleFunction.symbol
                    defaultValue = it.getParameterDefaultValueFromProperty()
                }
            }
        }.also(functionTarget::bind)
    }
}