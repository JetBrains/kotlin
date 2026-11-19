/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object FirSerializationErrors : KtDiagnosticsContainer() {
    val INLINE_CLASSES_NOT_SUPPORTED by error2<String, String>()

    val PLUGIN_IS_NOT_ENABLED by warning0()
    val ANONYMOUS_OBJECTS_NOT_SUPPORTED by error0()
    val INNER_CLASSES_NOT_SUPPORTED by error0()

    val EXPLICIT_SERIALIZABLE_IS_REQUIRED by warning0()

    val COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED by error1<FirRegularClassSymbol>()
    val COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS by error2<ConeKotlinType, ConeKotlinType>()
    val COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS by warning2<ConeKotlinType, ConeKotlinType>()

    val COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS by error1<FirRegularClassSymbol>()
    val SERIALIZER_FUNCTION_CLASH_IN_COMPANION by error1<String>()
    val PRIVATE_COMPANION_OF_SERIALIZABLE by warning2<FirClassSymbol<*>, FirClassSymbol<*>>()

    val SERIALIZABLE_ANNOTATION_IGNORED by error0()
    val NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR by error0()
    val PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY by error0()
    val DUPLICATE_SERIAL_NAME by error1<String>()
    val DUPLICATE_SERIAL_NAME_ENUM by error3<FirClassSymbol<*>, String, String>()
    val SERIALIZER_NOT_FOUND by error1<ConeKotlinType>()
    val SERIALIZER_NULLABILITY_INCOMPATIBLE by error2<ConeKotlinType, ConeKotlinType>()
    val SERIALIZER_TYPE_INCOMPATIBLE by warning3<ConeKotlinType, ConeKotlinType, ConeKotlinType>()
    val ABSTRACT_SERIALIZER_TYPE by error2<ConeKotlinType, ConeKotlinType>()
    val LOCAL_SERIALIZER_USAGE by error1<ConeKotlinType>()
    val CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT by error3<ConeKotlinType, ConeKotlinType, String>()
    val CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE by error3<ConeKotlinType, ConeKotlinType, String>()
    val CUSTOM_SERIALIZER_MAY_BE_INACCESSIBLE by warning2<FirClassLikeSymbol<*>, FirClassLikeSymbol<*>>()
    val SERIALIZABLE_WITH_ON_TYPE_HAS_NO_EFFECT by warning0()

    val GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED by error0()
    val DYNAMIC_TYPE_NOT_SUPPORTED by error0()
    val TRANSIENT_MISSING_INITIALIZER by error0()

    val TRANSIENT_IS_REDUNDANT by warning0()
    val INCORRECT_TRANSIENT by warning0()

    val REQUIRED_KOTLIN_TOO_HIGH by error3<String, String, String>()
    val PROVIDED_RUNTIME_TOO_LOW by error3<String, String, String>()

    val INCONSISTENT_INHERITABLE_SERIALINFO by error2<ConeKotlinType, ConeKotlinType>()
    val META_SERIALIZABLE_NOT_APPLICABLE by error0()
    val SERIALIZABLE_ANNOTATION_TYPEALIAS_UNSUPPORTED by warning0()
    val INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE by error0()
    val SERIALINFO_INAPPLICABLE_TARGET by warning1<String>()
    val INHERITABLE_SERIALINFO_INAPPLICABLE_TARGET by warning1<String>()

    val EXTERNAL_SERIALIZER_USELESS by warning1<FirClassSymbol<*>>()
    val EXTERNAL_CLASS_NOT_SERIALIZABLE by error2<FirClassSymbol<*>, ConeKotlinType>()
    val EXTERNAL_CLASS_IN_ANOTHER_MODULE by error2<FirClassSymbol<*>, ConeKotlinType>()
    val EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR by error3<FirClassSymbol<*>, ConeKotlinType, String>()

    val KEEP_SERIALIZER_ANNOTATION_USELESS by error0()
    val KEEP_SERIALIZER_ANNOTATION_ON_POLYMORPHIC by error0()

    val PROTOBUF_PROTO_NUM_DUPLICATED by warning2<String, String>()
    val PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE by warning3<String, String, ConeKotlinType>()

    val JSON_FORMAT_REDUNDANT_DEFAULT by warning0()
    val JSON_FORMAT_REDUNDANT by warning0()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDefaultErrorMessagesSerialization
}
