/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.konan.impl.ForwardDeclarationsFqNames

internal val DEPRECATED_ANNOTATION_FQN: FqName = FqName(Deprecated::class.java.name).intern()
internal val DEPRECATED_ANNOTATION_CID: ClassId = internedClassId(DEPRECATED_ANNOTATION_FQN)

internal val ANY_CID: ClassId = internedClassId(StandardNames.FqNames.any.toSafe().intern())
private val NOTHING_CID: ClassId = internedClassId(StandardNames.FqNames.nothing.toSafe().intern())

internal val SPECIAL_CLASS_WITHOUT_SUPERTYPES_CIDS = listOf(
    ANY_CID,
    NOTHING_CID
)

private val STANDARD_KOTLIN_PACKAGES = listOf(
    StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString(),
    "kotlinx"
)

private val KOTLIN_NATIVE_SYNTHETIC_PACKAGES = ForwardDeclarationsFqNames.syntheticPackages
    .map { fqName ->
        check(!fqName.isRoot)
        fqName.asString()
    }

private const val CINTEROP_PACKAGE = "kotlinx.cinterop"
private const val DARWIN_PACKAGE = "platform.darwin"

private val OBJC_INTEROP_CALLABLE_ANNOTATIONS = listOf(
    "ObjCMethod",
    "ObjCConstructor",
    "ObjCFactory"
)

internal val DEFAULT_CONSTRUCTOR_NAME = Name.identifier("<init>").intern()
internal val DEFAULT_SETTER_VALUE_NAME = Name.identifier("value").intern()

internal fun Name.strip(): String =
    asString().removeSurrounding("<", ">")

internal val FqName.isUnderStandardKotlinPackages: Boolean
    get() = hasAnyPrefix(STANDARD_KOTLIN_PACKAGES)

internal val FqName.isUnderKotlinNativeSyntheticPackages: Boolean
    get() = hasAnyPrefix(KOTLIN_NATIVE_SYNTHETIC_PACKAGES)

internal val FqName.isUnderDarwinPackage: Boolean
    get() = asString().hasPrefix(DARWIN_PACKAGE)

@Suppress("NOTHING_TO_INLINE")
private inline fun FqName.hasAnyPrefix(prefixes: List<String>): Boolean =
    asString().let { fqName -> prefixes.any(fqName::hasPrefix) }

private fun String.hasPrefix(prefix: String): Boolean {
    val lengthDifference = length - prefix.length
    return when {
        lengthDifference == 0 -> this == prefix
        lengthDifference > 0 -> this[prefix.length] == '.' && this.startsWith(prefix)
        else -> false
    }
}

internal val ClassId.isObjCInteropCallableAnnotation: Boolean
    get() = packageFqName.asString() == CINTEROP_PACKAGE && relativeClassName.asString() in OBJC_INTEROP_CALLABLE_ANNOTATIONS

internal val AnnotationDescriptor.isObjCInteropCallableAnnotation: Boolean
    get() {
        val classifier = type.declarationDescriptor
        return classifier.name.asString() in OBJC_INTEROP_CALLABLE_ANNOTATIONS
                && (classifier.containingDeclaration as? PackageFragmentDescriptor)?.fqName?.asString() == CINTEROP_PACKAGE
    }
