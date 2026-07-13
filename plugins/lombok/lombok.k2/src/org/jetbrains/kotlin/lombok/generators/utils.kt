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
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameter
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
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

abstract class LombokDeclarationKey : GeneratedDeclarationKey()
