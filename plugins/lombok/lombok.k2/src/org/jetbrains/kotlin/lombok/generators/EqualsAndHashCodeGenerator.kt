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
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf
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
 * Declaration key shared by the generated `equals`, `hashCode` and (optional) `canEqual` for the same class so
 * that the IR body filler builds all bodies from the same property snapshot.
 *
 * @param hasCanEqual whether this class also generates `canEqual`, so the `equals` body filler knows whether to
 *   call it
 */
class EqualsAndHashCodeGeneratorKey(
    val propertyInfos: List<EqualsAndHashCodePropertyInfo>,
    val callSuper: Boolean,
    val hasCanEqual: Boolean,
) : LombokDeclarationKey()

val FirDeclarationOrigin.isEqualsAndHashCode
    get() = this is FirDeclarationOrigin.Plugin && key is EqualsAndHashCodeGeneratorKey

/**
 * Holder for the (optional) `equals`/`hashCode`/`canEqual` symbols generated for a single class.
 * All three share the same [EqualsAndHashCodeGeneratorKey] instance so that the IR body filler
 * sees a consistent property selection across the functions.
 */
private class EqualsAndHashCodeMembers(
    val equals: FirNamedFunctionSymbol,
    val hashCode: FirNamedFunctionSymbol,
    val canEqual: FirNamedFunctionSymbol?,
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
        val members = cache.getValue(classSymbol, context) ?: return emptySet()
        return if (members.canEqual != null) callableNames + AccessorGenerator.CAN_EQUAL else callableNames
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        val members = cache.getValue(classSymbol, context) ?: return emptyList()
        return listOf(
            when (callableId.callableName) {
                EQUALS_NAME -> members.equals
                HASHCODE_NAME -> members.hashCode
                AccessorGenerator.CAN_EQUAL -> members.canEqual ?: return emptyList()
                else -> shouldNotBeCalled()
            }
        )
    }

    private fun initializeMembersIfNeeded(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): EqualsAndHashCodeMembers? {
        // Only a plain class gets `equals`/`hashCode`: an annotation class can hold no member at all, an enum's
        // `equals`/`hashCode` are final, and an object is compared by identity. Every other kind is already
        // reported as `ANNOTATION_HAS_NO_EFFECT`.
        if (classSymbol !is FirRegularClassSymbol || !classSymbol.isPlainClass) return null

        val annotation = session.lombokService.getEqualsAndHashCode(classSymbol) ?: return null
        val declaredScope = context.declaredScope

        // Skip-both rule: if the user has explicitly declared either `equals` or `hashCode`, generate neither.
        // The checker reports the partial-override error or the "both already exist" warning.
        if (hasUserDeclaredEqualsOrHashCode(declaredScope)) return null

        // Lombok's own rule: skip `canEqual` only for a class that can have no subclass worth rejecting anyway -
        // final and with nothing but `Any` to chain to. Note that a non-trivial superclass alone already forces
        // generation, finality notwithstanding.
        val generatesCanEqual = !(classSymbol.looksFinal && !classSymbol.hasNonTrivialSuperclass(session))

        // The whole ancestor chain matters, not just the immediate superclass: Kotlin's override check considers
        // every ancestor, so a generated `canEqual` two or more levels below the one that first declared it still
        // needs `override`, or it fails to compile with "hides member of supertype".
        val canEqualIsOverride = generatesCanEqual && classSymbol.hasCanEqualInHierarchy(session)

        val key by lazy(LazyThreadSafetyMode.NONE) {
            val propertyInfos = computePropertiesToInclude(annotation, declaredScope)

            EqualsAndHashCodeGeneratorKey(
                propertyInfos = propertyInfos,
                callSuper = annotation.shouldCallSuper(
                    session.lombokService.config.equalsAndHashCodeCallSuper,
                    classSymbol,
                    session,
                ),
                hasCanEqual = generatesCanEqual,
            )
        }

        val equalsSymbol = createJavaOrKotlinMemberFunction(
            owner = classSymbol,
            name = EQUALS_NAME,
            valueParameters = listOf(ConeLombokValueParameter(OTHER, session.builtinTypes.nullableAnyType)),
            returnTypeRef = session.builtinTypes.booleanType,
            visibility = Visibilities.Public,
            modality = Modality.OPEN,
            isOverride = true,
            createKey = { key },
        )
        val hashCodeSymbol = createJavaOrKotlinMemberFunction(
            owner = classSymbol,
            name = HASHCODE_NAME,
            valueParameters = emptyList(),
            returnTypeRef = session.builtinTypes.intType,
            visibility = Visibilities.Public,
            modality = Modality.OPEN,
            isOverride = true,
            createKey = { key },
        )
        val canEqualSymbol = runIf(generatesCanEqual) {
            createJavaOrKotlinMemberFunction(
                owner = classSymbol,
                name = AccessorGenerator.CAN_EQUAL,
                valueParameters = listOf(ConeLombokValueParameter(OTHER, session.builtinTypes.nullableAnyType)),
                returnTypeRef = session.builtinTypes.booleanType,
                visibility = Visibilities.Protected,
                modality = Modality.OPEN,
                isOverride = canEqualIsOverride,
                createKey = { key },
            )
        }

        return EqualsAndHashCodeMembers(equalsSymbol, hashCodeSymbol, canEqualSymbol)
    }

    /**
     * Whether [this] is final, without resolving [FirRegularClassSymbol.resolvedStatus]: this can be called from
     * `getCallableNamesForClass`, itself callable as early as the SUPERTYPES stage, at which point requesting a
     * lazy resolve to STATUS - even for this class's own declaration - is a phase contract violation.
     *
     * A `null` raw modality is safe to read as final here because a *class* (as opposed to a member) has no
     * context-dependent default: unlike a member, which can inherit openness, a class with no explicit modality
     * modifier is always final, so nothing about this needs the class's status to actually be resolved.
     */
    @OptIn(SymbolInternals::class)
    private val FirRegularClassSymbol.looksFinal: Boolean
        get() = fir.status.modality.let { it == null || it == Modality.FINAL }

    /**
     * Whether [this] (or any of its ancestors, up to but excluding [Any]) already declares a `canEqual(other:
     * Any?): Boolean`, hand-written or itself Lombok-generated.
     */
    private tailrec fun FirClassSymbol<*>.hasCanEqualInHierarchy(session: FirSession): Boolean {
        val superClassSymbol = getSuperClassSymbolOrAny(session) ?: return false
        if (superClassSymbol.classId == StandardClassIds.Any) return false
        if (superClassSymbol.declaresCanEqual(session)) return true
        return superClassSymbol.hasCanEqualInHierarchy(session)
    }

    private fun FirClassSymbol<*>.declaresCanEqual(session: FirSession): Boolean {
        var found = false
        // TYPES, not the default STATUS: this runs from within this very class's own STATUS-phase status
        // computation, and a phase cannot request a lazy resolve into itself for another declaration - only into
        // a strictly earlier one. Matching a `canEqual(other: Any?)` shape only needs value parameter types,
        // which TYPES already resolves.
        processAllDeclaredCallables(session, memberRequiredPhase = FirResolvePhase.TYPES) { callable ->
            if (!found &&
                callable is FirNamedFunctionSymbol &&
                callable.name == AccessorGenerator.CAN_EQUAL &&
                !callable.hasReceiverOrContextParameters &&
                callable.valueParameterSymbols.singleOrNull()?.resolvedReturnType?.isNullableAny == true
            ) {
                found = true
            }
        }
        return found
    }

    private fun hasUserDeclaredEqualsOrHashCode(declaredScope: FirClassDeclaredMemberScope?): Boolean {
        var found = false

        declaredScope?.processAllFunctions {
            if (it.hasReceiverOrContextParameters) return@processAllFunctions

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

                // See the same guard in `ToStringGenerator`: a static property is never part of the generated
                // members, and its getter takes no dispatch receiver for IR to pass `this`/`other` in (KT-88367).
                if (property.isStatic) return@processAllProperties

                val propertyName = property.name

                if (property.findAnnotationOnPropertyOrField(LombokNames.EQUALS_AND_HASH_CODE_EXCLUDE_ID, session) != null) {
                    return@processAllProperties
                }

                val includeAnnotation = property.findAnnotationOnPropertyOrField(LombokNames.EQUALS_AND_HASH_CODE_INCLUDE_ID, session)

                if (includeAnnotation == null && property.isExcludedByDollarPrefix) return@processAllProperties

                if (includeAnnotation == null && annotation.onlyExplicitlyIncluded ?: config.equalsAndHashCodeOnlyExplicitlyIncluded) {
                    return@processAllProperties
                }

                // Same convention as ToString: properties without a backing field are treated like
                // computed/getter-only members. Include them only if explicitly opted in.
                add(EqualsAndHashCodePropertyInfo(propertyName, ignoreWithoutBackingField = includeAnnotation == null))
            }
        }
    }
}
