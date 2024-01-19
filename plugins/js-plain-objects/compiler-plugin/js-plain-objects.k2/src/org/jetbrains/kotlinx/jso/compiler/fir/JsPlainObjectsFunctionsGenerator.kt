/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
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
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.jspo.compiler.fir.services.jsPlainObjectPropertiesProvider
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsPluginKey
import org.jetbrains.kotlinx.jspo.compiler.resolve.StandardIds

/**
 * The extension generate a synthetic factory and copy-method for an `external interface` annotated with @JsPlainObjects
 * Imagine the next interfaces:
 * ```
 * external interface User {
 *   val name: String
 * }
 * @JsPlainObjects
 * external interface Admin {
 *   val chat: Chat
 *   val email: String?
 * }
 * ```
 *
 * For the interface `Admin` this function should generate the companion inline function:
 * ```
 * external interface Admin {
 *   val chat: Chat
 *
 *  inline fun copy(chat: Chat = this.chat, email: String = this.email): Admin =
 *      Admin.Companion.invoke(chat, name)
 *
 *   companion object {
 *      inline operator fun invoke(chat: Chat, email: String? = VOID): Admin =
 *          js("{ chat: chat, name: name }")
 *   }
 * }
 * ```
 */
class JsPlainObjectsFunctionsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val voidPropertySymbol by lazy {
        session.symbolProvider
            .getTopLevelPropertySymbols(StandardIds.KOTLIN_JS_FQN, StandardIds.VOID_PROPERTY_NAME)
            .single()
    }

   private val matchedInterfaces by lazy {
        session.predicateBasedProvider
            .getSymbolsByPredicate(JsPlainObjectsPredicates.AnnotatedWithJsPlainObject.LOOKUP)
            .filterIsInstance<FirRegularClassSymbol>()
            .toSet()
    }

    private val factoryFqNamesToJsPlainObjectsInterface by lazy {
        matchedInterfaces.associateBy { it.classId.asSingleFqName() }
    }

    private val FirClassLikeSymbol<*>.isJsPlainObject: Boolean
        get() = this is FirRegularClassSymbol && this in matchedInterfaces

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        return if (classSymbol.isJsPlainObject) setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) else emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        return if (
            owner is FirRegularClassSymbol &&
            owner.isJsPlainObject &&
            name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        ) generateCompanionDeclaration(owner)
        else null
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.companionObjectSymbol != null) return null
        val classId = owner.classId.createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
        return buildRegularClass {
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            moduleData = session.moduleData
            origin = JsPlainObjectsPluginKey.origin
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
            classSymbol.isCompanion && outerClass?.isJsPlainObject == true -> setOf(OperatorNameConventions.INVOKE)
            classSymbol.isJsPlainObject -> setOf(StandardNames.DATA_CLASS_COPY)
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
                    ?.let { factoryFqNamesToJsPlainObjectsInterface[it.asSingleFqName()] }
                    ?.let { listOf(createJsPlainObjectCopyFunction(callableId, context.owner, it).symbol) } ?: emptyList()
            }
            OperatorNameConventions.INVOKE -> {
                possibleInterface
                    ?.takeIf { context.owner.isCompanion }
                    ?.let { factoryFqNamesToJsPlainObjectsInterface[it.asSingleFqName()] }
                    ?.let { listOf(createJsPlainObjectFactoryFunction(callableId, context.owner, it).symbol) } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun createJsPlainObjectFactoryFunction(
        callableId: CallableId,
        parent: FirClassSymbol<*>,
        jsPlainObjectInterface: FirRegularClassSymbol,
    ): FirSimpleFunction {
        return createJsPlainObjectsFunction(callableId, parent, jsPlainObjectInterface) {
            runIf(resolvedReturnTypeRef.type.isNullable) {
                buildPropertyAccessExpression {
                    calleeReference = buildResolvedNamedReference {
                        name = StandardIds.VOID_PROPERTY_NAME
                        resolvedSymbol = voidPropertySymbol
                    }
                    coneTypeOrNull = voidPropertySymbol.resolvedReturnType
                }
            }
        }
    }

    private fun createJsPlainObjectCopyFunction(
        callableId: CallableId,
        parent: FirClassSymbol<*>,
        jsPlainObjectInterface: FirRegularClassSymbol,
    ): FirSimpleFunction {
        val interfaceType = jsPlainObjectInterface.defaultType()
        return createJsPlainObjectsFunction(callableId, parent, jsPlainObjectInterface) {
            buildPropertyAccessExpression {
                calleeReference = buildResolvedNamedReference {
                    name = StandardIds.VOID_PROPERTY_NAME
                    resolvedSymbol = voidPropertySymbol
                }
                coneTypeOrNull = voidPropertySymbol.resolvedReturnType
            }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun createJsPlainObjectsFunction(
        callableId: CallableId,
        parent: FirClassSymbol<*>,
        jsPlainObjectInterface: FirRegularClassSymbol,
        getParameterDefaultValueFromProperty: FirPropertySymbol.() -> FirExpression?
    ): FirSimpleFunction {
        val jsPlainObjectProperties = session.jsPlainObjectPropertiesProvider.getJsPlainObjectsPropertiesForClass(jsPlainObjectInterface)
        val functionTarget = FirFunctionTarget(null, isLambda = false)
        val jsPlainObjectInterfaceDefaultType = jsPlainObjectInterface.defaultType()

        return buildSimpleFunction {
            moduleData = jsPlainObjectInterface.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = JsPlainObjectsPluginKey.origin
            symbol = FirNamedFunctionSymbol(callableId)
            name = callableId.callableName
            returnTypeRef = jsPlainObjectInterfaceDefaultType.toFirResolvedTypeRef()

            status = FirResolvedDeclarationStatusImpl(
                jsPlainObjectInterface.visibility,
                Modality.FINAL,
                jsPlainObjectInterface.visibility.toEffectiveVisibility(parent, forClass = true)
            ).apply {
                isInline = true
                isOperator = true
            }

            dispatchReceiverType = parent.defaultType()
            jsPlainObjectInterface.typeParameterSymbols.mapTo(typeParameters) { it.fir }
            jsPlainObjectProperties.mapTo(valueParameters) {
                val typeRef = it.resolvedReturnTypeRef
                buildValueParameter {
                    moduleData = session.moduleData
                    origin = JsPlainObjectsPluginKey.origin
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