/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.jspo.compiler.fir.services.ClassProperty
import org.jetbrains.kotlinx.jspo.compiler.fir.services.jsPlainObjectPropertiesProvider
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsPluginKey
import org.jetbrains.kotlinx.jspo.compiler.resolve.StandardIds
import kotlin.collections.plusAssign

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
 *   val email: String?
 *
 *   @JsExport.Ignore
 *   companion object {
 *      @JsNoDispatchReceiver
 *      inline operator fun invoke(chat: Chat, email: String? = VOID): Admin =
 *          js("{ chat: chat, name: name }")
 *
 *      @JsNoDispatchReceiver
 *      inline fun copy(source: Admin, chat: Chat = VOID, email: String = VOID): Admin =
 *          js("Object.assign({}, source, { chat: chat, email: email })")
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
            annotateWith(JsStandardClassIds.Annotations.JsExportIgnore)
        }.symbol
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        val outerClass = classSymbol.getContainingClassSymbol()
        if (!classSymbol.isCompanion || outerClass?.isJsPlainObject != true) return emptySet()
        return setOf(OperatorNameConventions.INVOKE, StandardNames.DATA_CLASS_COPY)
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context == null) return emptyList()

        val containingClass = callableId.classId
        val possibleInterface = containingClass?.outerClassId

        return when (callableId.callableName) {
            StandardNames.DATA_CLASS_COPY -> {
                possibleInterface
                    ?.takeIf { context.owner.isCompanion }
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
        return createJsPlainObjectsFunction(callableId, parent, jsPlainObjectInterface, isOperator = true) {
            runIf(resolvedTypeRef.coneType.isMarkedOrFlexiblyNullable) {
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
        return createJsPlainObjectsFunction(callableId, parent, jsPlainObjectInterface, includeJsPlainObjectInterfaceAsParameter = true) {
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
        isOperator: Boolean = false,
        includeJsPlainObjectInterfaceAsParameter: Boolean = false,
        getParameterDefaultValueFromProperty: ClassProperty.() -> FirExpression?
    ): FirSimpleFunction {
        var typeParameterSubstitutor: ConeSubstitutor? = null
        val jsPlainObjectProperties = session.jsPlainObjectPropertiesProvider.getJsPlainObjectsPropertiesForClass(jsPlainObjectInterface)

        val functionTarget = FirFunctionTarget(null, isLambda = false)
        val jsPlainObjectInterfaceDefaultType = jsPlainObjectInterface.defaultType()
        val typeParameterSubstitutionMap = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()

        return buildSimpleFunction {
            val functionalSymbol = FirNamedFunctionSymbol(callableId)

            moduleData = jsPlainObjectInterface.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = JsPlainObjectsPluginKey.origin
            symbol = functionalSymbol
            name = callableId.callableName

            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                Visibilities.Public.toEffectiveVisibility(parent, forClass = true)
            ).apply {
                isInline = true
                isOverride = false
                this.isOperator = isOperator
            }

            annotateWith(JsStandardClassIds.Annotations.JsExportIgnore)

            if (jsPlainObjectInterface.typeParameterSymbols.isNotEmpty()) {
                jsPlainObjectInterface.typeParameterSymbols.mapTo(typeParameters) {
                    val typeParameter = buildTypeParameterCopy(it.fir) {
                        origin = JsPlainObjectsPluginKey.origin
                        symbol = FirTypeParameterSymbol()
                        containingDeclarationSymbol = functionalSymbol
                    }
                    typeParameterSubstitutionMap[it] = ConeTypeParameterTypeImpl(
                        typeParameter.symbol.toLookupTag(), isMarkedNullable = false
                    )
                    typeParameter
                }

                val localTypeParameterSubstitutor = substitutorByMap(typeParameterSubstitutionMap, session).also {
                    typeParameterSubstitutor = it
                }

                typeParameters.forEach { typeParameter ->
                    typeParameter.replaceBounds(
                        typeParameter.bounds.map { boundTypeRef ->
                            boundTypeRef.withReplacedConeType(localTypeParameterSubstitutor.substituteOrNull(boundTypeRef.coneType))
                        }
                    )
                }
            }

            val replacedJsPlainObjectType = jsPlainObjectInterfaceDefaultType.toFirResolvedTypeRef().run {
                typeParameterSubstitutor?.let {
                    withReplacedConeType(it.substituteOrNull(jsPlainObjectInterfaceDefaultType))
                } ?: this
            }

            returnTypeRef = replacedJsPlainObjectType
            dispatchReceiverType = parent.defaultType()

            annotateWith(JsStandardClassIds.Annotations.JsNoDispatchReceiver)

            if (includeJsPlainObjectInterfaceAsParameter) {
                val sourceVariableName = Name.identifier("source")

                valueParameters += buildValueParameter {
                    moduleData = session.moduleData
                    origin = JsPlainObjectsPluginKey.origin
                    returnTypeRef = replacedJsPlainObjectType
                    name = sourceVariableName
                    symbol = FirValueParameterSymbol(sourceVariableName)
                    isCrossinline = false
                    isNoinline = true
                    isVararg = false
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    containingDeclarationSymbol = this@buildSimpleFunction.symbol
                }
            }

            jsPlainObjectProperties.mapTo(valueParameters) {
                val typeRef = it.resolvedTypeRef
                buildValueParameter {
                    moduleData = session.moduleData
                    origin = JsPlainObjectsPluginKey.origin
                    returnTypeRef = typeParameterSubstitutor?.let { subst ->
                        typeRef.withReplacedConeType(subst.substituteOrNull(typeRef.coneType))
                    } ?: typeRef
                    name = it.name
                    symbol = FirValueParameterSymbol(it.name)
                    isCrossinline = false
                    isNoinline = true
                    isVararg = false
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    containingDeclarationSymbol = this@buildSimpleFunction.symbol
                    defaultValue = it.getParameterDefaultValueFromProperty()
                }
            }
        }.also(functionTarget::bind)
    }

    private fun FirAnnotationContainerBuilder.annotateWith(classId: ClassId) {
        annotations += buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = classId.toLookupTag()
                    .constructClassType(typeArguments = ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = false)
            }
            argumentMapping = FirEmptyAnnotationArgumentMapping
        }
    }
}
