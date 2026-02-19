/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import kotlin.metadata.ClassName
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.name.*

internal val DEPRECATED_ANNOTATION_FQN: FqName = FqName(Deprecated::class.java.name)
internal const val DEPRECATED_ANNOTATION_FULL_NAME: ClassName = "kotlin/Deprecated"
internal val DEPRECATED_ANNOTATION_CLASS_ID: CirEntityId = CirEntityId.create(DEPRECATED_ANNOTATION_FULL_NAME)

internal const val ANY_CLASS_FULL_NAME: ClassName = "kotlin/Any"
internal val ANY_CLASS_ID: CirEntityId = CirEntityId.create(ANY_CLASS_FULL_NAME)

internal val SPECIAL_CLASS_WITHOUT_SUPERTYPES_CLASS_IDS: List<CirEntityId> = listOf(
    ANY_CLASS_ID,
    CirEntityId.create("kotlin/Nothing")
)

// illegal Kotlin classifier name, for special purposes only
internal val NON_EXISTING_CLASSIFIER_ID = CirEntityId.create("$0")

internal val SPECIAL_CLASS_WITHOUT_SUPERTYPES_CLASS_NAMES: List<ClassName> =
    SPECIAL_CLASS_WITHOUT_SUPERTYPES_CLASS_IDS.map(CirEntityId::toString)

private val STANDARD_KOTLIN_PACKAGES: List<CirPackageName> = listOf(
    CirPackageName.create(StandardNames.BUILT_INS_PACKAGE_FQ_NAME),
    CirPackageName.create("kotlinx")
)

private val KOTLIN_NATIVE_SYNTHETIC_PACKAGES: List<CirPackageName> = NativeStandardInteropNames.ForwardDeclarations.syntheticPackages
    .map { packageFqName ->
        check(!packageFqName.isRoot)
        CirPackageName.create(packageFqName)
    }

internal val CNAMES_STRUCTS_PACKAGE = CirPackageName.create(NativeForwardDeclarationKind.Struct.packageFqName)

internal val OBJCNAMES_CLASSES_PACKAGE = CirPackageName.create(NativeForwardDeclarationKind.ObjCClass.packageFqName)

internal val OBJCNAMES_PROTOCOLS_PACKAGE = CirPackageName.create(NativeForwardDeclarationKind.ObjCProtocol.packageFqName)

private val CINTEROP_PACKAGE: CirPackageName = CirPackageName.create(NativeStandardInteropNames.cInteropPackage)

private val OBJC_INTEROP_CALLABLE_ANNOTATIONS: List<CirName> = listOf(
    CirName.create("ObjCMethod"),
    CirName.create("ObjCConstructor"),
    CirName.create("ObjCFactory")
)

internal val COMMONIZER_OBJC_INTEROP_CALLABLE_ANNOTATION_ID =
    CirEntityId.create(CirPackageName.create("kotlin.commonizer"), CirName.create("ObjCCallable"))

internal val DEFAULT_CONSTRUCTOR_NAME: CirName = CirName.create("<init>")
internal val DEFAULT_SETTER_VALUE_NAME: CirName = CirName.create("value")

internal fun Name.strip(): String =
    asString().removeSurrounding("<", ">")

internal val CirPackageName.isUnderStandardKotlinPackages: Boolean
    get() = STANDARD_KOTLIN_PACKAGES.any(::startsWith)

internal val CirPackageName.isUnderKotlinNativeSyntheticPackages: Boolean
    get() = KOTLIN_NATIVE_SYNTHETIC_PACKAGES.any(::startsWith)

internal val CirEntityId.isObjCInteropCallableAnnotation: Boolean
    get() = this == COMMONIZER_OBJC_INTEROP_CALLABLE_ANNOTATION_ID ||
            packageName == CINTEROP_PACKAGE && relativeNameSegments.singleOrNull() in OBJC_INTEROP_CALLABLE_ANNOTATIONS

// platform integers/optimistic commonization

val KOTLIN_BYTE_ID = ClassId.fromString("kotlin/Byte")
val KOTLIN_SHORT_ID = ClassId.fromString("kotlin/Short")
val KOTLIN_INT_ID = ClassId.fromString("kotlin/Int")
val KOTLIN_LONG_ID = ClassId.fromString("kotlin/Long")

val KOTLIN_UBYTE_ID = ClassId.fromString("kotlin/UByte")
val KOTLIN_USHORT_ID = ClassId.fromString("kotlin/UShort")
val KOTLIN_UINT_ID = ClassId.fromString("kotlin/UInt")
val KOTLIN_ULONG_ID = ClassId.fromString("kotlin/ULong")

val KOTLIN_FLOAT_ID = ClassId.fromString("kotlin/Float")
val KOTLIN_DOUBLE_ID = ClassId.fromString("kotlin/Double")

val BYTE_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/ByteVarOf")
val SHORT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/ShortVarOf")
val INT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/IntVarOf")
val LONG_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/LongVarOf")

val UBYTE_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/UByteVarOf")
val USHORT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/UShortVarOf")
val UINT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/UIntVarOf")
val ULONG_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/ULongVarOf")

val FLOAT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/FloatVarOf")
val DOUBLE_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/DoubleVarOf")

val INT_ARRAY_ID = ClassId.fromString("kotlin/IntArray")
val LONG_ARRAY_ID = ClassId.fromString("kotlin/LongArray")

val UINT_ARRAY_ID = ClassId.fromString("kotlin/UIntArray")
val ULONG_ARRAY_ID = ClassId.fromString("kotlin/ULongArray")

val INT_RANGE_ID = ClassId.fromString("kotlin/ranges/IntRange")
val LONG_RANGE_ID = ClassId.fromString("kotlin/ranges/LongRange")

val UINT_RANGE_ID = ClassId.fromString("kotlin/ranges/UIntRange")
val ULONG_RANGE_ID = ClassId.fromString("kotlin/ranges/ULongRange")

val INT_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/IntProgression")
val LONG_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/LongProgression")

val UINT_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/UIntProgression")
val ULONG_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/ULongProgression")

val PLATFORM_INT_ID = ClassId.fromString("kotlin/PlatformInt")
val PLATFORM_UINT_ID = ClassId.fromString("kotlin/PlatformUInt")

val PLATFORM_INT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/PlatformIntVarOf")
val PLATFORM_UINT_VAR_OF_ID = ClassId.fromString("kotlinx/cinterop/PlatformUIntVarOf")

val PLATFORM_INT_ARRAY_ID = ClassId.fromString("kotlin/PlatformIntArray")
val PLATFORM_UINT_ARRAY_ID = ClassId.fromString("kotlin/PlatformUIntArray")

val PLATFORM_INT_RANGE_ID = ClassId.fromString("kotlin/ranges/PlatformIntRange")
val PLATFORM_UINT_RANGE_ID = ClassId.fromString("kotlin/ranges/PlatformUIntRange")

val PLATFORM_INT_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/PlatformIntProgression")
val PLATFORM_UINT_PROGRESSION_ID = ClassId.fromString("kotlin/ranges/PlatformUIntProgression")
