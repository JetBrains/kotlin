/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isAnnotationClass
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.lombok.AccessorNames
import org.jetbrains.kotlin.lombok.config.CallSuperMode
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun String.capitalize(): String {
    return if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
}

fun String.decapitalize(): String {
    return if (isEmpty()) this else this[0].lowercaseChar() + substring(1)
}

fun String.normalizeAndCapitalize(isPrimitiveBoolean: Boolean): String {
    return (if (isPrimitiveBoolean && isPrefixed(AccessorNames.IS)) {
        this.removePrefix(AccessorNames.IS)
    } else {
        this
    }).capitalize()
}

fun String.isPrefixed(prefix: String): Boolean {
    return startsWith(prefix) && length > prefix.length && this[prefix.length].isUpperCase()
}

fun FirTypeRef.isPrimitiveBoolean(): Boolean {
    return when (this) {
        is FirJavaTypeRef -> (type as? JavaPrimitiveType)?.type == PrimitiveType.BOOLEAN
        else -> this.coneTypeOrNull?.lowerBoundIfFlexible()?.isBoolean ?: false
    }
}

@OptIn(ExperimentalContracts::class)
fun FirClassSymbol<*>.isSuitableJavaClass(): Boolean {
    contract {
        returns(true) implies (this@isSuitableJavaClass is FirRegularClassSymbol)
    }
    return (this is FirRegularClassSymbol) && origin == FirDeclarationOrigin.Java.Source
}

context(extension: FirExtension)
fun createJavaOrKotlinMemberFunction(
    owner: FirClassSymbol<*>,
    name: Name,
    valueParameters: List<ConeLombokValueParameter>,
    returnTypeRef: FirTypeRef,
    visibility: Visibility,
    modality: Modality,
    createKey: () -> GeneratedDeclarationKey,
    isStatic: Boolean = false,
    symbol: FirNamedFunctionSymbol? = null,
    typeParameters: Collection<FirTypeParameter> = emptyList(),
    isOverride: Boolean = false,
): FirNamedFunctionSymbol {
    return if (owner.hasJavaOrigin) {
        owner.createJavaMethod(
            name = name,
            valueParameters = valueParameters,
            returnTypeRef = returnTypeRef,
            visibility = visibility,
            modality = modality,
            isStatic = isStatic,
            methodSymbol = symbol,
            methodTypeParameters = typeParameters,
            isOverride = isOverride,
        ).symbol
    } else {
        extension.createMemberFunction(
            owner = owner,
            key = createKey(),
            name = name,
            returnType = returnTypeRef.coneType
        ) {
            this@createMemberFunction.modality = modality
            this@createMemberFunction.visibility = visibility

            for (parameter in valueParameters) {
                valueParameter(parameter.name, parameter.typeRef.coneType)
            }

            status {
                this@status.isOverride = isOverride
            }
        }.symbol
    }
}

fun FirClassSymbol<*>.createJavaMethod(
    name: Name,
    valueParameters: List<ConeLombokValueParameter>,
    returnTypeRef: FirTypeRef,
    visibility: Visibility,
    modality: Modality,
    dispatchReceiverType: ConeSimpleKotlinType? = this.defaultType(),
    isStatic: Boolean = false,
    methodSymbol: FirNamedFunctionSymbol? = null,
    methodTypeParameters: Collection<FirTypeParameter> = emptyList(),
    isOverride: Boolean = false,
): FirJavaMethod {
    return buildJavaMethod {
        containingClassSymbol = this@createJavaMethod
        moduleData = this@createJavaMethod.moduleData
        this.returnTypeRef = returnTypeRef
        this.dispatchReceiverType = dispatchReceiverType
        this.name = name
        symbol = methodSymbol ?: FirNamedFunctionSymbol(CallableId(classId, name))
        status = FirResolvedDeclarationStatusImpl(visibility, modality, visibility.toEffectiveVisibility(this@createJavaMethod)).apply {
            this.isStatic = isStatic
            this.isOverride = isOverride
        }
        isFromSource = true
        typeParameters += methodTypeParameters

        for (valueParameter in valueParameters) {
            this.valueParameters += buildJavaValueParameter {
                moduleData = this@createJavaMethod.moduleData
                this.returnTypeRef = valueParameter.typeRef
                containingDeclarationSymbol = this@buildJavaMethod.symbol
                this.name = valueParameter.name
                isVararg = false
                isFromSource = true
            }
        }
    }.apply {
        if (isStatic) {
            containingClassForStaticMemberAttr = this@createJavaMethod.toLookupTag()
        }
    }
}

class ConeLombokValueParameter(val name: Name, val typeRef: FirTypeRef)

val FirBasedSymbol<*>.hasJavaOrigin get() = origin is FirDeclarationOrigin.Java

/**
 * Whether Lombok generates anything at all into [this] class. An interface and an annotation class are the kinds
 * Lombok's own model has no counterpart for: an interface holds no state to generate from and no constructor to
 * generate, and an annotation class can hold no member at all - the platform reports `ANNOTATION_CLASS_MEMBER`
 * for one.
 *
 * Both are reported as `ANNOTATION_HAS_NO_EFFECT` already, so generating anyway makes the checker contradict the
 * generators, and the output is not merely useless: a constructor in an interface is rejected outright by the
 * backend, and a builder for an interface has no constructor to call (KT-87871).
 */
val FirClassSymbol<*>.isSupportedLombokTarget: Boolean
    get() = !isInterface && !isAnnotationClass

/**
 * Whether [this] is a plain class, that is, neither an interface, nor an annotation class, nor an enum class, nor
 * an object. It is the only kind `@Builder` and `@EqualsAndHashCode` generate anything into, and it mirrors
 * `isClass` in Lombok's own `JavacHandlerUtil`, which both of its handlers consult before generating - unlike
 * `@Log` and `@ToString`, which accept an enum class and an object as well.
 *
 * Neither annotation has anything to generate for the other kinds, and generating anyway used to produce code
 * that doesn't even run:
 *  - an enum constructor takes the synthetic name and ordinal parameters, so a generated `build()` calls a
 *    signature that doesn't exist and fails with `NoSuchMethodError` (KT-87871);
 *  - `equals` and `hashCode` are final in `java.lang.Enum`, so generated ones make the whole class fail
 *    verification with "class Color overrides final method java.lang.Enum.equals" (KT-88507);
 *  - an object is a single instance compared by identity and has no constructor to build it with, so both
 *    annotations only ever generated members that repeat what the object already does (KT-88507).
 *
 * A local class is a plain class: `@EqualsAndHashCode` supports one, and `@Builder` is stopped for it by
 * `isCompanionNeeded` instead, since a local class can't hold the companion object a `builder()` needs.
 */
val FirClassSymbol<*>.isPlainClass: Boolean
    get() = classKind == ClassKind.CLASS

/**
 * Whether [this] has an extension receiver or context parameters.
 *
 * Lombok models Java, which has neither, so such a declaration falls outside everything the plugin generates.
 * Two consequences follow:
 *  - it can never clash with a generated member, so the conflict checks skip it;
 *  - it cannot carry `@Builder`, since there is no single obvious way to model either of them. `@Builder` turns
 *    every value parameter into a builder field with a setter named after it: an extension receiver has no name
 *    to derive one from at all, and while a context parameter usually does, it could just as reasonably become a
 *    builder field, a context parameter of the generated `builder()`, or one of `build()`. Rather than pick, the
 *    generator skips these declarations and `FirLombokBuilderChecker` reports them.
 */
val FirCallableSymbol<*>.hasReceiverOrContextParameters: Boolean
    get() = isExtension || hasContextParameters

/**
 * Whether `@ToString` and `@EqualsAndHashCode` leave [this] property out of what they generate unless it is
 * explicitly opted in with their `@Include`.
 *
 * A `$` prefix marks a name as generated or internal by convention - Lombok's own generated fields use it - so
 * such a field is never part of a class's rendering or identity by default. See the "small print" of both
 * features: "any variables that start with a $ symbol are excluded automatically. You can only include them by
 * using the @Include annotation." An `@Exclude` on one is therefore redundant, which both checkers report.
 */
val FirPropertySymbol.isExcludedByDollarPrefix: Boolean
    get() = name.asString().startsWith('$')

/**
 * Whether [this] extends a class other than [Any] - Lombok's `isDirectDescendantOfObject`, inverted.
 *
 * It decides whether chaining a generated `toString`/`equals`/`hashCode` to `super` carries any information at
 * all: `Any` renders as a bare identity hash, compares by identity and hashes by it, so nothing it returns
 * belongs in a member that is supposed to speak about a class's own state.
 */
fun FirClassSymbol<*>.hasNonTrivialSuperclass(session: FirSession): Boolean =
    getSuperClassSymbolOrAny(session).let { it != null && it.classId != StandardClassIds.Any }

/**
 * Whether the member `@ToString`/`@EqualsAndHashCode` generates for [classSymbol] chains to the superclass one.
 *
 * This is the whole of that decision: the IR body builders chain whenever it says so, [Any] included.
 *
 * The annotation's own `callSuper` argument decides whenever it is there, and is never second-guessed. Lombok
 * honors an explicit `callSuper = true` on a class extending nothing but [Any] as well, even though [Any]
 * renders as a bare identity hash and compares by identity - `@ToString` only calls that "pretty much
 * meaningless" in its javadoc, while `@EqualsAndHashCode` refuses to generate at all, which
 * `CALL_SUPER_TO_ANY_IS_POINTLESS` reports.
 *
 * Otherwise [configCallSuperMode] - the `lombok.<feature>.callSuper` setting - decides, and only its `call`
 * chains, and only for a class that has a superclass worth chaining to: a project-wide setting cannot know that
 * this particular class extends nothing but [Any], so Lombok gates it on that and so does this (KT-88771).
 */
fun ConeLombokAnnotations.CallSuper.shouldCallSuper(
    configCallSuperMode: CallSuperMode,
    classSymbol: FirClassSymbol<*>,
    session: FirSession,
): Boolean = when (val explicitMode = callSuper) {
    null -> configCallSuperMode == CallSuperMode.Call && classSymbol.hasNonTrivialSuperclass(session)
    else -> explicitMode == CallSuperMode.Call
}

abstract class LombokDeclarationKey : GeneratedDeclarationKey()
