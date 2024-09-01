/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal

import kotlinx.cinterop.ExperimentalForeignApi

internal value class TypeInfoNames(private val typeInfoPtr: NativePtr) {
    init {
        require(!typeInfoPtr.isNull())
    }

    /**
     * The last component of the name if it exists and is allowed by TF_REFLECTION_SHOW_REL_NAME.
     */
    val simpleName: String?
        get() {
            // TODO: consider replacing '$' by another delimeter that can't be used in class name specified with backticks (``)
            return getRelativeName(typeInfoPtr, true)?.substringAfterLast('.')?.substringAfterLast('$')
        }

    /**
     * The fully qualified name if it exists and is allowed by both TF_REFLECTION_SHOW_REL_NAME and TF_REFLECTION_SHOW_PKG_NAME
     */
    val qualifiedName: String?
        get() {
            val packageName = getPackageName(typeInfoPtr, true) ?: return null
            val relativeName = getRelativeName(typeInfoPtr, true) ?: return null
            return if (packageName.isEmpty()) relativeName else "$packageName.$relativeName"
        }

    /**
     * The fully qualified name if it exists. Ignores TF_REFLECTION_SHOW_REL_NAME and TF_REFLECTION_SHOW_PKG_NAME
     */
    val fullName: String?
        get() {
            val relativeName = getRelativeName(typeInfoPtr, false) ?: return null
            val packageName: String? = getPackageName(typeInfoPtr, false)
            return if (packageName?.isEmpty() != false) relativeName else "$packageName.$relativeName"
        }
}

@GCUnsafeCall("Kotlin_TypeInfo_getPackageName")
private external fun getPackageName(typeInfo: NativePtr, checkFlags: Boolean): String?

@GCUnsafeCall("Kotlin_TypeInfo_getRelativeName")
private external fun getRelativeName(typeInfo: NativePtr, checkFlags: Boolean): String?