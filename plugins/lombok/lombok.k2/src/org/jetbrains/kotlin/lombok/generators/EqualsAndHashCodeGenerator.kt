/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.builtins.StandardNames.EQUALS_NAME
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.CallSuperMode
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.lombok.generators.kotlin.isRelevantForConflictsCheck
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * Per-property data passed from the FIR generator to the IR body filler.
 *
 * @param propertyName the simple name of the backing property
 * @param ignoreWithoutBackingField when `true`, the IR filler must skip a property without a backing field
 *   (i.e. a computed property that the user did not explicitly opt in via `@EqualsAndHashCode.Include`)
 */
data class EqualsAndHashCodePropertyInfo(
    val propertyName: Name,
    val ignoreWithoutBackingField: Boolean,
)

/**
 * Declaration key shared by the generated `equals` and `hashCode` for the same class so that the IR body
 * filler builds both bodies from the same property snapshot.
 */
class EqualsAndHashCodeGeneratorKey(
    val propertyInfos: List<EqualsAndHashCodePropertyInfo>,
    val callSuper: Boolean,
) : LombokDeclarationKey()

val FirDeclarationOrigin.isEqualsAndHashCode
    get() = this is FirDeclarationOrigin.Plugin && key is EqualsAndHashCodeGeneratorKey

/**
 * Holder for the (optional) `equals`/`hashCode` symbols generated for a single class.
 * Both share the same [EqualsAndHashCodeGeneratorKey] instance so that the IR body filler
 * sees a consistent property selection across the two functions.
 */
private class EqualsAndHashCodeMembers(
    val equals: FirNamedFunctionSymbol,
    val hashCode: FirNamedFunctionSymbol,
)

/**
 * See https://projectlombok.org/features/EqualsAndHashCode
 */
class EqualsAndHashCodeGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        val OTHER = Name.identifier("other")
        private val callableNames = setOf(EQUALS_NAME, HASHCODE_NAME)

        private val PREDICATE = DeclarationPredicate.create {
            annotated(
                listOf(
                    LombokNames.EQUALS_AND_HASH_CODE,
                    LombokNames.EQUALS_AND_HASH_CODE_INCLUDE_ID.asSingleFqName(),
                    LombokNames.EQUALS_AND_HASH_CODE_EXCLUDE_ID.asSingleFqName(),
                )
            )
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    private val cache: FirCache<FirClassSymbol<*>, EqualsAndHashCodeMembers?, MemberGenerationContext> =
        session.firCachesFactory.createCache(::initializeMembersIfNeeded)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return if (cache.getValue(classSymbol, context) != null) callableNames else emptySet()
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        val members = cache.getValue(classSymbol, context) ?: return emptyList()
        return listOf(
            when (callableId.callableName) {
                EQUALS_NAME -> members.equals
                HASHCODE_NAME -> members.hashCode
                else -> shouldNotBeCalled()
            }
        )
    }

    private fun initializeMembersIfNeeded(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): EqualsAndHashCodeMembers? {
        if (classSymbol !is FirRegularClassSymbol || classSymbol.isInterface) return null

        val annotation = session.lombokService.getEqualsAndHashCode(classSymbol) ?: return null
        val declaredScope = context.declaredScope

        // Skip-both rule: if the user has explicitly declared either `equals` or `hashCode`, generate neither.
        // The checker reports the partial-override error or the "both already exist" warning.
        if (hasUserDeclaredEqualsOrHashCode(declaredScope)) return null

        val equalsSymbol: FirNamedFunctionSymbol
        val hashCodeSymbol: FirNamedFunctionSymbol

        if (classSymbol.hasJavaOrigin) {
            equalsSymbol = classSymbol.createJavaMethod(
                name = EQUALS_NAME,
                valueParameters = listOf(ConeLombokValueParameter(OTHER, session.builtinTypes.nullableAnyType)),
                returnTypeRef = session.builtinTypes.booleanType,
                visibility = Visibilities.Public,
                modality = Modality.OPEN,
            ).symbol

            hashCodeSymbol = classSymbol.createJavaMethod(
                name = HASHCODE_NAME,
                returnTypeRef = session.builtinTypes.intType,
                valueParameters = emptyList(),
                visibility = Visibilities.Public,
                modality = Modality.OPEN,
            ).symbol
        } else {
            val propertyInfos = computePropertiesToInclude(annotation, declaredScope)
            val key = EqualsAndHashCodeGeneratorKey(
                propertyInfos = propertyInfos,
                callSuper = annotation.callSuper == CallSuperMode.Call,
            )

            equalsSymbol = createMemberFunction(
                owner = classSymbol,
                key = key,
                name = EQUALS_NAME,
                returnType = StandardTypes.Boolean,
            ) {
                modality = Modality.OPEN
                status {
                    isOverride = true
                    isOperator = true
                }
                valueParameter(OTHER, StandardTypes.NullableAny)
                withGeneratedDefaultBody()
            }.symbol

            hashCodeSymbol = createMemberFunction(
                owner = classSymbol,
                key = key,
                name = HASHCODE_NAME,
                returnType = StandardTypes.Int,
            ) {
                modality = Modality.OPEN
                status {
                    isOverride = true
                }
                withGeneratedDefaultBody()
            }.symbol
        }

        return EqualsAndHashCodeMembers(equalsSymbol, hashCodeSymbol)
    }

    private fun hasUserDeclaredEqualsOrHashCode(declaredScope: FirClassDeclaredMemberScope?): Boolean {
        var found = false

        declaredScope?.processAllFunctions {
            if (!it.isRelevantForConflictsCheck) return@processAllFunctions

            // Match the canonical `equals(other: Any?)` or `hashCode` signatures only.
            found = found || (it.name == EQUALS_NAME &&
                    it.valueParameterSymbols.singleOrNull()?.resolvedReturnType?.isNullableAny == true ||
                    it.name == HASHCODE_NAME &&
                    it.valueParameterSymbols.isEmpty())
        }

        return found
    }

    private fun computePropertiesToInclude(
        annotation: ConeLombokAnnotations.EqualsAndHashCode,
        declaredScope: FirClassDeclaredMemberScope?,
    ): List<EqualsAndHashCodePropertyInfo> {
        return buildList {
            val config = session.lombokService.config
            declaredScope?.processAllProperties { variableSymbol ->
                val property = variableSymbol as? FirPropertySymbol ?: return@processAllProperties

                val propertyName = property.name

                if (property.findAnnotationOnPropertyOrField(LombokNames.EQUALS_AND_HASH_CODE_EXCLUDE_ID, session) != null ||
                    propertyName.identifier in annotation.excludeFields
                ) {
                    return@processAllProperties
                }

                val includeAnnotation = property.findAnnotationOnPropertyOrField(LombokNames.EQUALS_AND_HASH_CODE_INCLUDE_ID, session)

                // The deprecated-but-still-supported `of` parameter pins selection to the listed names.
                if (annotation.ofFields != null) {
                    if (propertyName.identifier !in annotation.ofFields) return@processAllProperties
                } else if (includeAnnotation == null && annotation.onlyExplicitlyIncluded ?: config.equalsAndHashCodeOnlyExplicitlyIncluded) {
                    return@processAllProperties
                }

                // Same convention as ToString: properties without a backing field are treated like
                // computed/getter-only members. Include them only if explicitly opted in.
                val ignoreWithoutBackingField = includeAnnotation == null && annotation.ofFields == null

                add(EqualsAndHashCodePropertyInfo(propertyName, ignoreWithoutBackingField))
            }
        }
    }
}
