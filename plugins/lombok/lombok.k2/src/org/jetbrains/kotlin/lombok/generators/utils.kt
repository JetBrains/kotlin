/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isAnnotationClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.lombok.AccessorNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
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
 * Whether Lombok generates nothing at all into [this] class, because it is a kind of class Lombok's own model has
 * no counterpart for: an interface holds no state to generate from and no constructor to generate, and an
 * annotation class can hold no member at all - the platform reports `ANNOTATION_CLASS_MEMBER` for one.
 *
 * Both are reported as `ANNOTATION_HAS_NO_EFFECT` already, so generating anyway makes the checker contradict the
 * generators, and the output is not merely useless: a constructor in an interface is rejected outright by the
 * backend, and a builder for an interface has no constructor to call (KT-87871).
 */
val FirClassSymbol<*>.isUnsupportedLombokTarget: Boolean
    get() = isInterface || isAnnotationClass

/**
 * Whether `@Builder` and `@EqualsAndHashCode` generate nothing at all into [this] class: everything
 * [isUnsupportedLombokTarget] covers, plus an enum class. Lombok's own handlers draw the line in the same place -
 * they accept a class only, while `@Log` and `@ToString` accept an enum too.
 *
 * An enum can neither be built nor compared by a generated member:
 *  - its constructors take the synthetic name and ordinal parameters, so a generated `build()` calls a signature
 *    that doesn't exist and fails with `NoSuchMethodError` (KT-87871);
 *  - `equals` and `hashCode` are final in `java.lang.Enum`, so generated ones don't even load - the whole class
 *    fails to verify with "class Color overrides final method java.lang.Enum.equals" (KT-88507).
 */
val FirClassSymbol<*>.isUnsupportedLombokTargetOrEnumClass: Boolean
    get() = isUnsupportedLombokTarget || isEnumClass

/**
 * Whether [this] has an extension receiver or context parameters.
 *
 * Lombok models Java, which has neither, so such a declaration falls outside everything the plugin generates.
 * Two consequences follow:
 *  - it can never clash with a generated member, so the conflict checks skip it;
 *  - it cannot carry `@Builder`, whose every value parameter becomes a builder field with a setter named after
 *    it, and neither a receiver nor a context parameter carries a name to derive one from. The generator skips
 *    these declarations and `FirLombokBuilderChecker` reports them.
 */
val FirCallableSymbol<*>.hasReceiverOrContextParameters: Boolean
    get() = isExtension || hasContextParameters

abstract class LombokDeclarationKey : GeneratedDeclarationKey()
