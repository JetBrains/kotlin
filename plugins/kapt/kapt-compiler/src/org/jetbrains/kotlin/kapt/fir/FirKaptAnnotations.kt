/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isValue
import org.jetbrains.kotlin.fir.declarations.getAnnotationRetention
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.ir.types.impl.IrErrorClassImpl
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode

/** Writes FIR annotations as ASM annotation nodes, the way the JVM backend writes them to class files. */
@OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
internal class FirKaptAnnotations(private val session: FirSession, private val typeMapper: FirJvmTypeMapper) {
    class Written(val visible: List<AnnotationNode>, val invisible: List<AnnotationNode>, val sources: List<FirKaptSourceAnnotation>)

    fun write(annotations: List<FirAnnotation>): Written {
        val visible = mutableListOf<AnnotationNode>()
        val invisible = mutableListOf<AnnotationNode>()
        val sources = mutableListOf<FirKaptSourceAnnotation>()
        for (annotation in annotations) {
            val classId = annotation.annotationClassId()
            if (classId != null && classId in SKIPPED_ANNOTATIONS) continue
            val classSymbol = classId?.let { session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol }
            if (classSymbol != null && classSymbol.isOptionalAnnotationClass()) continue

            val retention = classSymbol?.getAnnotationRetention(session) ?: AnnotationRetention.RUNTIME
            val node = annotationNode(annotation) ?: continue
            if (retention == AnnotationRetention.RUNTIME) visible += node else invisible += node
            sources += FirKaptSourceAnnotation(annotation, annotation.resolvedType.containsErrorTypes()) { constantValue(annotation) }
        }
        return Written(visible, invisible, sources)
    }

    private fun FirRegularClassSymbol.isOptionalAnnotationClass(): Boolean =
        resolvedAnnotationClassIds.contains(StandardClassIds.Annotations.OptionalExpectation)

    private fun FirAnnotation.annotationClassId(): ClassId? =
        annotationTypeRef.coneTypeOrNull?.fullyExpandedType(session)?.classId

    fun annotationNode(annotation: FirAnnotation): AnnotationNode? {
        val type = annotation.annotationTypeRef.coneTypeOrNull ?: return null
        val node = AnnotationNode(typeMapper.mapStubType(type, session).descriptor)
        val mapping = annotation.argumentMapping.mapping
        val parameters = annotationParameters(type)
        if (parameters == null) {
            for ([name, argument] in mapping) node.addValue(name, argument.toAnnotationValue(null))
            return node
        }
        // The JVM backend writes the arguments in the parameter order, and an empty array for an omitted vararg.
        for (parameter in parameters) {
            val argument = mapping[parameter.name]
            val value = when {
                argument != null -> argument.toAnnotationValue(parameter.fir.returnTypeRef.coneTypeOrNull)
                parameter.isVararg && !parameter.hasDefaultValue -> emptyList<Any>()
                else -> null
            }
            node.addValue(parameter.name, value)
        }
        return node
    }

    private fun AnnotationNode.addValue(name: Name, value: Any?) {
        if (value == null) return
        values = (values ?: mutableListOf()).apply { add(name.asString()); add(value) }
    }

    @OptIn(DirectDeclarationsAccess::class)
    private fun annotationParameters(type: ConeKotlinType): List<FirValueParameterSymbol>? {
        val classSymbol = type.fullyExpandedType(session).toRegularClassSymbol(session) ?: return null
        val constructor = classSymbol.declarationSymbols.firstOrNull { it is FirConstructorSymbol && it.isPrimary } as? FirConstructorSymbol
            ?: return null
        return constructor.valueParameterSymbols
    }

    fun annotationValue(expression: FirExpression, expectedType: ConeKotlinType?): Any? = expression.toAnnotationValue(expectedType)

    /** The value of [annotation] as the IR annotation the JVM backend would have for it. */
    private fun constantValue(annotation: FirAnnotation): ConstantValue<*> {
        val type = annotation.annotationTypeRef.coneTypeOrNull?.fullyExpandedType(session)
        val classId = type?.takeUnless { it is ConeErrorType }?.classId ?: ERROR_CLASS_ID
        val mapping = annotation.argumentMapping.mapping
        val parameters = type?.let(::annotationParameters)
        val arguments = LinkedHashMap<Name, ConstantValue<*>>()
        if (parameters == null) {
            for ([name, argument] in mapping) argument.toConstantValueOrNull()?.let { arguments[name] = it }
        } else {
            for (parameter in parameters) {
                val argument = mapping[parameter.name]
                val value = when {
                    argument != null -> argument.toConstantValueOrNull()
                    parameter.isVararg && !parameter.hasDefaultValue -> ArrayValue(emptyList())
                    else -> null
                }
                if (value != null) arguments[parameter.name] = value
            }
        }
        return AnnotationValue.create(classId, arguments)
    }

    private fun FirExpression.toConstantValueOrNull(): ConstantValue<*>? {
        when (this) {
            is FirVarargArgumentsExpression -> return ArrayValue(arguments.flatMap { it.toConstantElements() })
            is FirCollectionLiteral -> return ArrayValue(argumentList.arguments.flatMap { it.toConstantElements() })
            is FirFunctionCall if isArrayOfCall() -> return ArrayValue(arguments.flatMap { it.toConstantElements() })
            is FirGetClassCall -> return classLiteralConstant()
            is FirAnnotation -> return constantValue(this)
            is FirFunctionCall if calleeReference.toResolvedCallableSymbol() is FirConstructorSymbol &&
                    resolvedType.toRegularClassSymbol(session)?.classKind == org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS -> {
                val arguments = resolvedArgumentMapping.orEmpty().entries.mapNotNull { [argument, parameter] ->
                    argument.toConstantValueOrNull()?.let { parameter.name to it }
                }.toMap()
                return AnnotationValue.create(resolvedType.fullyExpandedType(session).classId ?: ERROR_CLASS_ID, arguments)
            }
            else -> {}
        }
        if (this is FirQualifiedAccessExpression) {
            val entry = calleeReference.toResolvedEnumEntrySymbol()
            val enumClassId = entry?.callableId?.classId
            if (entry != null && enumClassId != null) return EnumValue(enumClassId, entry.name)
        }
        val literal = (this as? FirLiteralExpression) ?: evaluateAs<FirLiteralExpression>(session) ?: return null
        val value = literal.toAsmConstant() ?: return if (literal.kind == ConstantValueKind.Null) NullValue else null
        return when (literal.kind) {
            ConstantValueKind.UnsignedByte -> UByteValue(value as Byte)
            ConstantValueKind.UnsignedShort -> UShortValue(value as Short)
            ConstantValueKind.UnsignedInt -> UIntValue(value as Int)
            ConstantValueKind.UnsignedLong -> ULongValue(value as Long)
            else -> when (value) {
                is Boolean -> BooleanValue(value)
                is Char -> CharValue(value)
                is Byte -> ByteValue(value)
                is Short -> ShortValue(value)
                is Int -> IntValue(value)
                is Long -> LongValue(value)
                is Float -> FloatValue(value)
                is Double -> DoubleValue(value)
                is String -> StringValue(value)
                else -> null
            }
        }
    }

    private fun FirExpression.toConstantElements(): List<ConstantValue<*>> {
        if (this is FirSpreadArgumentExpression) {
            val value = expression.toConstantValueOrNull()
            return (value as? ArrayValue)?.value ?: listOfNotNull(value)
        }
        return listOfNotNull(toConstantValueOrNull())
    }

    private fun FirGetClassCall.classLiteralConstant(): KClassValue? {
        var type = resolvedType.typeArguments.singleOrNull()?.type?.fullyExpandedType(session) ?: return null
        var arrayDimensions = 0
        while (type.classId == StandardClassIds.Array) {
            arrayDimensions++
            type = type.typeArguments.singleOrNull()?.type?.fullyExpandedType(session)
                ?: return KClassValue(StandardClassIds.Any, arrayDimensions)
        }
        if (type is ConeErrorType) {
            // Class literals of unresolved classes are dropped, arrays of them are not.
            return if (arrayDimensions == 0) null else KClassValue(ERROR_CLASS_ID, arrayDimensions)
        }
        val classId = type.classId ?: return null
        return KClassValue(classId, arrayDimensions)
    }

    private fun FirExpression.toAnnotationValue(expectedType: ConeKotlinType?): Any? {
        val elementType = expectedType?.fullyExpandedType(session)?.arrayElementTypeOrNull()
        when (this) {
            is FirVarargArgumentsExpression -> return arguments.flatMap { it.toArrayElements(elementType) }
            is FirCollectionLiteral -> return argumentList.arguments.flatMap { it.toArrayElements(elementType) }
            is FirFunctionCall if isArrayOfCall() -> return arguments.flatMap { it.toArrayElements(elementType) }
            is FirGetClassCall -> return classLiteralType()
            is FirAnnotation -> return annotationNode(this)
            is FirFunctionCall if calleeReference.toResolvedCallableSymbol() is FirConstructorSymbol &&
                    resolvedType.toRegularClassSymbol(session)?.classKind == org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS ->
                return nestedAnnotationNode(this)
            else -> {}
        }
        enumEntry()?.let { return it }
        val literal = (this as? FirLiteralExpression) ?: evaluateAs<FirLiteralExpression>(session) ?: return null
        return literal.toAsmConstant()
    }

    private fun FirExpression.toArrayElements(elementType: ConeKotlinType?): List<Any?> {
        if (this is FirSpreadArgumentExpression) {
            val value = expression.toAnnotationValue(elementType?.let { null })
            return (value as? List<*>) ?: listOfNotNull(value)
        }
        return listOfNotNull(toAnnotationValue(elementType))
    }

    private fun FirFunctionCall.isArrayOfCall(): Boolean {
        val callable = calleeReference.toResolvedCallableSymbol() ?: return false
        val id = callable.callableId ?: return false
        return id.packageName == StandardClassIds.BASE_KOTLIN_PACKAGE && id.className == null &&
                (id.callableName == ARRAY_OF || id.callableName.asString().endsWith("ArrayOf") || id.callableName == EMPTY_ARRAY)
    }

    private fun nestedAnnotationNode(call: FirFunctionCall): AnnotationNode {
        val node = AnnotationNode(typeMapper.mapStubType(call.resolvedType, session).descriptor)
        val arguments = call.resolvedArgumentMapping.orEmpty().entries.associate { [argument, parameter] -> parameter.name to argument }
        for (parameter in annotationParameters(call.resolvedType).orEmpty()) {
            val argument = arguments[parameter.name]
            val value = when {
                argument != null -> argument.toAnnotationValue(parameter.fir.returnTypeRef.coneTypeOrNull)
                parameter.isVararg && !parameter.hasDefaultValue -> emptyList<Any>()
                else -> null
            }
            node.addValue(parameter.name, value)
        }
        return node
    }

    private fun FirGetClassCall.classLiteralType(): Type? {
        val classType = resolvedType.typeArguments.singleOrNull()?.type?.fullyExpandedType(session) ?: return null
        // The JVM backend drops class literals of unresolved classes, but keeps arrays of them.
        if (classType is ConeErrorType) return null
        val classSymbol = classType.toRegularClassSymbol(session)
        if (classSymbol != null && classSymbol.isValue) {
            return Type.getObjectType(JvmClassName.internalNameByClassId(classSymbol.classId))
        }
        return typeMapper.mapStubType(classType, session)
    }

    private fun FirExpression.enumEntry(): Array<String>? {
        val entry = when (this) {
            is FirQualifiedAccessExpression -> calleeReference.toResolvedEnumEntrySymbol()
            else -> null
        } ?: return null
        val enumClassId = entry.callableId.classId ?: return null
        return arrayOf("L" + JvmClassName.internalNameByClassId(enumClassId) + ";", entry.name.asString())
    }

    private fun ConeKotlinType.arrayElementTypeOrNull(): ConeKotlinType? =
        if (isArrayOrPrimitiveArray) arrayElementType() else null

    companion object {
        private val ARRAY_OF = Name.identifier("arrayOf")
        private val EMPTY_ARRAY = Name.identifier("emptyArray")

        /** Annotations the JVM backend never writes to class files. */
        private val SKIPPED_ANNOTATIONS: Set<ClassId> = setOf(
            StandardClassIds.Annotations.FlexibleNullability,
            StandardClassIds.Annotations.FlexibleMutability,
            StandardClassIds.Annotations.RawTypeAnnotation,
            StandardClassIds.Annotations.EnhancedNullability,
            JvmStandardClassIds.Annotations.JvmDefault,
        )
    }
}

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

/** Same depth limit as `IrType.containsErrorTypes`, so that the same types get corrected. */
internal fun ConeKotlinType.containsErrorTypes(allowedDepth: Int = 10): Boolean {
    if (allowedDepth <= 0) return false
    if (this is ConeErrorType) return true
    return typeArguments.any { it.type?.containsErrorTypes(allowedDepth - 1) == true }
}

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

/** The class id of unresolved types in IR, which the stub converter treats as `error.NonExistentClass`. */
private val ERROR_CLASS_ID: ClassId = IrErrorClassImpl.classId!!

/** Maps [type] like the JVM backend maps it in stubs, unresolved types become `error.NonExistentClass`. */
internal fun FirJvmTypeMapper.mapStubType(
    type: ConeKotlinType,
    session: FirSession,
    mode: TypeMappingMode = TypeMappingMode.DEFAULT,
    sw: JvmSignatureWriter? = null,
): Type = mapType(type.forJvmSignature(session), mode, sw) { NON_EXISTENT_CLASS_NAME }
