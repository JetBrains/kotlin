/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.firapt

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.evaluateAs
import org.jetbrains.kotlin.fir.declarations.getAnnotationRetention
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/** An annotation of a FIR declaration, or one without arguments that stubs add, like the nullability annotations. */
internal class AptAnnotationMirror private constructor(
    private val env: AptEnv,
    val annotationElement: AptTypeElement,
    private val arguments: () -> Map<Name, FirExpression>,
) : AnnotationMirror {
    override fun getAnnotationType(): DeclaredType =
        if (annotationElement.isError) annotationElement.errorType else annotationElement.asType() as DeclaredType

    private val values: Map<ExecutableElement, AnnotationValue> by lazy {
        val result = LinkedHashMap<ExecutableElement, AnnotationValue>()
        for ([name, argument] in arguments()) {
            val method = annotationElement.methods.firstOrNull { it.simpleName.contentEquals(name.asString()) && it.parameters.isEmpty() }
                ?: continue
            annotationValue(env, argument, method.returnType)?.let { result[method] = it }
        }
        result
    }

    override fun getElementValues(): Map<out ExecutableElement, AnnotationValue> = values

    fun valuesWithDefaults(): Map<ExecutableElement, AnnotationValue> {
        val result = LinkedHashMap<ExecutableElement, AnnotationValue>()
        for (method in annotationElement.methods) {
            if (method.kind != ElementKind.METHOD) continue
            val value = values[method] ?: method.defaultValue ?: continue
            result[method] = value
        }
        return result
    }

    override fun toString() = buildString {
        append('@').append(annotationElement.qualifiedName)
        if (values.isNotEmpty()) values.entries.joinTo(this, ", ", "(", ")") { "${it.key.simpleName}=${it.value}" }
    }

    companion object {
        fun of(env: AptEnv, annotation: FirAnnotation): AptAnnotationMirror? {
            val classId = annotation.classIdOrNull(env)
            val element = when {
                classId != null -> env.typeElement(classId) ?: env.typeElementOrError(JvmClassName.internalNameByClassId(classId))
                else -> return null
            }
            return AptAnnotationMirror(env, element) { annotation.argumentMapping.mapping }
        }

        fun of(env: AptEnv, call: FirFunctionCall): AptAnnotationMirror? {
            val classId = call.resolvedType.fullyExpandedType(env.session).classId ?: return null
            val element = env.typeElement(classId) ?: return null
            return AptAnnotationMirror(env, element) {
                call.resolvedArgumentMapping.orEmpty().entries.associate { [argument, parameter] -> parameter.name to argument }
            }
        }

        fun marker(env: AptEnv, internalName: String): AptAnnotationMirror =
            AptAnnotationMirror(env, env.typeElementOrError(internalName)) { emptyMap() }
    }
}

internal fun FirAnnotation.classIdOrNull(env: AptEnv): ClassId? =
    annotationTypeRef.coneTypeOrNull?.fullyExpandedType(env.session)?.classId

/** Annotations the JVM backend never writes to class files. */
private val SKIPPED_ANNOTATIONS: Set<ClassId> = setOf(
    StandardClassIds.Annotations.FlexibleNullability,
    StandardClassIds.Annotations.FlexibleMutability,
    StandardClassIds.Annotations.RawTypeAnnotation,
    StandardClassIds.Annotations.EnhancedNullability,
    JvmStandardClassIds.Annotations.JvmDefault,
    StandardClassIds.Annotations.Deprecated,
    JvmStandardClassIds.Annotations.Java.Deprecated,
    ClassId.fromString("kotlin/jvm/internal/KaptIgnored"),
)

private val JAVA_DEPRECATED = "java/lang/Deprecated"

/**
 * The annotations of a Kotlin declaration as the stubs have them: the runtime ones first, `@kotlin.Deprecated` as `@java.lang.Deprecated`,
 * and without the annotations that do not reach class files.
 */
internal fun declarationAnnotations(
    env: AptEnv, declaration: FirDeclaration?, annotations: List<FirAnnotation>, isSource: Boolean,
): List<AptAnnotationMirror> {
    val visible = mutableListOf<AptAnnotationMirror>()
    val invisible = mutableListOf<AptAnnotationMirror>()
    var deprecated = false
    for (annotation in annotations) {
        val classId = annotation.classIdOrNull(env)
        if (classId == StandardClassIds.Annotations.Deprecated || classId == JvmStandardClassIds.Annotations.Java.Deprecated) {
            deprecated = true
        }
        if (classId != null && classId in SKIPPED_ANNOTATIONS) continue
        val classSymbol = classId?.let { env.session.symbolProvider.getClassLikeSymbolByClassId(it) as? FirRegularClassSymbol }
        if (classSymbol != null && StandardClassIds.Annotations.OptionalExpectation in classSymbol.resolvedAnnotationClassIds) continue
        val mirror = AptAnnotationMirror.of(env, annotation) ?: continue
        val retention = classSymbol?.getAnnotationRetention(env.session) ?: AnnotationRetention.RUNTIME
        if (retention == AnnotationRetention.RUNTIME) visible += mirror else invisible += mirror
    }
    if (deprecated && isSource) invisible += AptAnnotationMirror.marker(env, JAVA_DEPRECATED)
    return visible + invisible
}

/** The value of an annotation argument; [type] is the return type of the annotation method, it gives the primitive kind. */
internal fun annotationValue(env: AptEnv, expression: FirExpression, type: TypeMirror?): AptAnnotationValue? {
    val elementType = (type as? AptArrayType)?.componentType ?: type
    when (expression) {
        is FirVarargArgumentsExpression -> return AptAnnotationValue(expression.arguments.flatMap { arrayElements(env, it, elementType) })
        is FirCollectionLiteral -> return AptAnnotationValue(expression.argumentList.arguments.flatMap { arrayElements(env, it, elementType) })
        is FirFunctionCall if expression.isArrayOfCall() -> return AptAnnotationValue(expression.arguments.flatMap { arrayElements(env, it, elementType) })
        is FirGetClassCall -> return classLiteral(env, expression)?.let(::AptAnnotationValue)
        is FirAnnotation -> return AptAnnotationMirror.of(env, expression)?.let(::AptAnnotationValue)
        is FirFunctionCall if expression.calleeReference.toResolvedCallableSymbol() is FirConstructorSymbol &&
                expression.resolvedType.toRegularClassSymbol(env.session)?.classKind == org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS ->
            return AptAnnotationMirror.of(env, expression)?.let(::AptAnnotationValue)
        is FirEnumEntryDeserializedAccessExpression -> return enumConstant(env, expression.enumClassId, expression.enumEntryName)
        else -> {}
    }
    if (expression is FirQualifiedAccessExpression) {
        val entry = expression.calleeReference.toResolvedEnumEntrySymbol()
        val enumClassId = entry?.callableId?.classId
        if (entry != null && enumClassId != null) return enumConstant(env, enumClassId, entry.name)
    }
    val literal = (expression as? FirLiteralExpression) ?: expression.evaluateAs<FirLiteralExpression>(env.session) ?: return null
    val value = literal.toAsmConstant() ?: return null
    return AptAnnotationValue(coerce(value, elementType?.kind))
}

private fun arrayElements(env: AptEnv, expression: FirExpression, elementType: TypeMirror?): List<AptAnnotationValue> {
    if (expression is FirSpreadArgumentExpression) {
        val value = annotationValue(env, expression.expression, elementType?.let(::AptArrayType))
        @Suppress("UNCHECKED_CAST")
        return (value?.value as? List<AptAnnotationValue>) ?: listOfNotNull(value)
    }
    return listOfNotNull(annotationValue(env, expression, elementType))
}

private fun coerce(value: Any, kind: TypeKind?): Any = when {
    value !is Number && value !is Char -> value
    kind == TypeKind.BYTE -> (value as? Number)?.toByte() ?: value
    kind == TypeKind.SHORT -> (value as? Number)?.toShort() ?: value
    kind == TypeKind.INT -> (value as? Number)?.toInt() ?: value
    kind == TypeKind.LONG -> (value as? Number)?.toLong() ?: value
    kind == TypeKind.FLOAT -> (value as? Number)?.toFloat() ?: value
    kind == TypeKind.DOUBLE -> (value as? Number)?.toDouble() ?: value
    kind == TypeKind.CHAR -> (value as? Number)?.toInt()?.toChar() ?: value
    else -> value
}

private fun enumConstant(env: AptEnv, enumClassId: ClassId, name: Name): AptAnnotationValue {
    val element = env.typeElement(enumClassId)
    val constant = element?.fields?.firstOrNull { it.simpleName.contentEquals(name.asString()) }
    return AptAnnotationValue(constant ?: name.asString())
}

/**
 * The type of a class literal: erased, like `Class` values in class files. The class is the one referenced: the type of the call can be
 * the expected one, like `KClass<out Exception>` for a Java default value `IllegalArgumentException.class`.
 */
private fun classLiteral(env: AptEnv, call: FirGetClassCall): TypeMirror? {
    val referenced = (call.argument as? FirClassReferenceExpression)?.classTypeRef?.coneTypeOrNull
    val classType = (referenced ?: call.resolvedType.typeArguments.singleOrNull()?.type)?.fullyExpandedType(env.session) ?: return null
    if (classType is ConeErrorType) return null
    // A Java `void.class` is `Unit` in FIR.
    if (referenced != null && classType.isUnit) return env.voidType
    return erased(env.typeOf(classType, TypeMappingMode.DEFAULT) { env.objectType })
}

private fun erased(type: TypeMirror): TypeMirror = when (type) {
    is AptErrorType -> type
    is AptDeclaredType -> type.element.rawType
    is AptArrayType -> AptArrayType(erased(type.componentType))
    else -> type
}

private fun FirFunctionCall.isArrayOfCall(): Boolean {
    val id = calleeReference.toResolvedCallableSymbol()?.callableId ?: return false
    return id.packageName == StandardClassIds.BASE_KOTLIN_PACKAGE && id.className == null &&
            (id.callableName.asString() == "arrayOf" || id.callableName.asString().endsWith("ArrayOf") || id.callableName.asString() == "emptyArray")
}

internal class AptAnnotationValue(private val value: Any) : AnnotationValue {
    override fun getValue(): Any = value

    override fun <R, P> accept(v: AnnotationValueVisitor<R, P>, p: P): R = when (value) {
        is Boolean -> v.visitBoolean(value, p)
        is Byte -> v.visitByte(value, p)
        is Char -> v.visitChar(value, p)
        is Short -> v.visitShort(value, p)
        is Int -> v.visitInt(value, p)
        is Long -> v.visitLong(value, p)
        is Float -> v.visitFloat(value, p)
        is Double -> v.visitDouble(value, p)
        is String -> v.visitString(value, p)
        is TypeMirror -> v.visitType(value, p)
        is VariableElement -> v.visitEnumConstant(value, p)
        is AnnotationMirror -> v.visitAnnotation(value, p)
        is List<*> -> @Suppress("UNCHECKED_CAST") v.visitArray(value as List<AnnotationValue>, p)
        else -> v.visitUnknown(this, p)
    }

    override fun toString(): String = when (value) {
        is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Char -> "'$value'"
        is Long -> "${value}L"
        is Float -> "${value}f"
        is TypeMirror -> "$value.class"
        is VariableElement -> "${value.enclosingElement}.${value.simpleName}"
        is List<*> -> value.joinToString(", ", "{", "}")
        else -> value.toString()
    }
}

/** Implements [javax.lang.model.element.Element.getAnnotation]: class values throw [MirroredTypeException], as in javac. */
internal fun <A : Annotation> createAnnotationProxy(env: AptEnv, mirror: AptAnnotationMirror, annotationClass: Class<A>): A {
    val values = mirror.valuesWithDefaults().mapKeys { it.key.simpleName.toString() }
    val handler = InvocationHandler { proxy, method, args ->
        when (method.name) {
            "annotationType" if args.isNullOrEmpty() -> annotationClass
            "toString" if args.isNullOrEmpty() -> mirror.toString()
            "hashCode" if args.isNullOrEmpty() -> System.identityHashCode(proxy)
            "equals" if args?.size == 1 -> proxy === args[0]
            else -> {
                val value = values[method.name] ?: throw IncompleteAnnotationException(annotationClass, method.name)
                convert(env, value.value, method.returnType)
            }
        }
    }
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(annotationClass.classLoader, arrayOf(annotationClass), handler) as A
}

private class IncompleteAnnotationException(type: Class<*>, name: String) :
    java.lang.annotation.IncompleteAnnotationException(@Suppress("UNCHECKED_CAST") (type as Class<out Annotation>), name)

private fun convert(env: AptEnv, value: Any, type: Class<*>): Any? {
    if (type.isArray) {
        val items = (value as? List<*>)?.map { (it as AnnotationValue).value } ?: listOf(value)
        val component = type.componentType
        if (component == Class::class.java) throw MirroredTypesException(items.map { it as TypeMirror })
        val array = java.lang.reflect.Array.newInstance(component, items.size)
        items.forEachIndexed { i, item -> java.lang.reflect.Array.set(array, i, convert(env, item, component)) }
        return array
    }
    return when {
        type == Class::class.java -> throw MirroredTypeException(value as TypeMirror)
        type.isEnum -> {
            val name = (value as? VariableElement)?.simpleName?.toString() ?: value.toString()
            @Suppress("UNCHECKED_CAST")
            java.lang.Enum.valueOf(type as Class<out Enum<*>>, name)
        }
        type.isAnnotation -> {
            @Suppress("UNCHECKED_CAST")
            createAnnotationProxy(env, value as AptAnnotationMirror, type as Class<out Annotation>)
        }
        else -> value
    }
}
