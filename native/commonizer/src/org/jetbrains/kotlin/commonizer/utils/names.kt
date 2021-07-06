/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import kotlinx.metadata.ClassName
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.konan.impl.ForwardDeclarationsFqNames

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

private val KOTLIN_NATIVE_SYNTHETIC_PACKAGES: List<CirPackageName> = ForwardDeclarationsFqNames.syntheticPackages
    .map { packageFqName ->
        check(!packageFqName.isRoot)
        CirPackageName.create(packageFqName)
    }

private val CINTEROP_PACKAGE: CirPackageName = CirPackageName.create("kotlinx.cinterop")

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


