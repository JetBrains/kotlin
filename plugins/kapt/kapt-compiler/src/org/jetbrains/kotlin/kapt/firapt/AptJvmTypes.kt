/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.org.objectweb.asm.Type

/** Maps [type] like the JVM backend maps it in stubs, unresolved types become `error.NonExistentClass`. */
internal fun FirJvmTypeMapper.mapStubType(
    type: ConeKotlinType,
    session: FirSession,
    mode: TypeMappingMode = TypeMappingMode.DEFAULT,
    sw: JvmSignatureWriter? = null,
): Type = mapType(type.forJvmSignature(session), mode, sw) { NON_EXISTENT_CLASS_NAME }

/** The type as the JVM backend sees it: anonymous objects are their supertype. */
internal fun ConeKotlinType.forJvmSignature(session: FirSession): ConeKotlinType =
    JvmSignatureTypeSubstitutor(session).substituteOrSelf(expandTypeAliases(session))

/** Like IR types: arguments of an expanded type alias can be type aliases themselves. */
private fun ConeKotlinType.expandTypeAliases(session: FirSession): ConeKotlinType {
    val expanded = fullyExpandedType(session)
    if (expanded !is ConeClassLikeType || expanded.typeArguments.isEmpty()) return expanded
    val arguments = expanded.typeArguments.map { argument ->
        if (argument is ConeKotlinTypeProjection) argument.replaceType(argument.type.expandTypeAliases(session)) else argument
    }
    return if (arguments.indices.all { arguments[it] == expanded.typeArguments[it] }) expanded
    else expanded.withArguments(arguments.toTypedArray())
}

private class JvmSignatureTypeSubstitutor(private val session: FirSession) : AbstractConeSubstitutor(session.typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        // The JVM backend's error type has no arguments.
        if (type is ConeErrorType) {
            return if (type.typeArguments.isEmpty()) null else ConeErrorType(type.diagnostic, nullable = type.nullable)
        }
        // Like Fir2Ir: the lower bound's classifier with the upper bound's nullability, and raw types without arguments.
        if (type is ConeFlexibleType) {
            val upper = type.upperBound
            val lower = type.lowerBound
            if (upper !is ConeClassLikeType) return substituteOrSelf(upper)
            val intermediate = when {
                lower !is ConeClassLikeType -> lower
                type.attributes.contains(CompilerConeAttributes.RawType) -> lower.withArguments(emptyArray())
                lower.lookupTag == upper.lookupTag -> lower.withArguments(upper.typeArguments)
                else -> lower
            }
            return substituteOrSelf(intermediate.withNullability(upper.isMarkedNullable, session.typeContext))
        }
        // Without bodies, local classes are not generated: the JVM backend sees their first supertype.
        val lookupTag = (type as? ConeClassLikeType)?.lookupTag ?: return null
        @OptIn(org.jetbrains.kotlin.name.ClassIdBasedLocality::class)
        if (!lookupTag.classId.isLocal) return null
        val localClass = lookupTag.toSymbol(session) as? FirClassSymbol<*> ?: return null
        val superType = localClass.resolvedSuperTypes.firstOrNull() ?: session.builtinTypes.anyType.coneType
        return substituteOrSelf(superType).withNullability(type.isMarkedNullable, session.typeContext)
    }
}

/** The value of a literal as a constant in a class file: unsigned values are their signed counterparts. */
internal fun FirLiteralExpression.toAsmConstant(): Any? {
    val value = when (val value = value ?: return null) {
        is UByte -> value.toByte()
        is UShort -> value.toShort()
        is UInt -> value.toInt()
        is ULong -> value.toLong()
        else -> value
    }
    return when (kind) {
        ConstantValueKind.Boolean -> value as Boolean
        ConstantValueKind.Char -> value as Char
        ConstantValueKind.Byte, ConstantValueKind.UnsignedByte -> (value as Number).toByte()
        ConstantValueKind.Short, ConstantValueKind.UnsignedShort -> (value as Number).toShort()
        ConstantValueKind.Int, ConstantValueKind.UnsignedInt -> (value as Number).toInt()
        ConstantValueKind.Long, ConstantValueKind.UnsignedLong -> (value as Number).toLong()
        ConstantValueKind.Float -> (value as Number).toFloat()
        ConstantValueKind.Double -> (value as Number).toDouble()
        ConstantValueKind.String -> value as String
        ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> (value as Number).toInt()
        else -> null
    }
}
