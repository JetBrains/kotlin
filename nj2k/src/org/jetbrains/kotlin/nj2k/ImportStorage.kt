/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.name.FqName

class ImportStorage {
    private val imports = mutableSetOf<FqName>()

    fun addImport(import: FqName) {
        if (isImportNeeded(import)) {
            imports += import
        }
    }

    fun getImports(): Set<FqName> = imports

    companion object {
        fun isImportNeeded(fqName: FqName): Boolean {
            if (fqName in NULLABILITY_ANNOTATIONS) return false
            if (fqName in JAVA_TYPE_WRAPPERS_WHICH_HAVE_CONFLICTS_WITH_KOTLIN_ONES) return false
            return true
        }

        private val JAVA_TYPE_WRAPPERS_WHICH_HAVE_CONFLICTS_WITH_KOTLIN_ONES = setOf(
            FqName(CommonClassNames.JAVA_LANG_BYTE),
            FqName(CommonClassNames.JAVA_LANG_SHORT),
            FqName(CommonClassNames.JAVA_LANG_LONG),
            FqName(CommonClassNames.JAVA_LANG_FLOAT),
            FqName(CommonClassNames.JAVA_LANG_DOUBLE)
        )

        fun isImportNeeded(fqName: String): Boolean = isImportNeeded(FqName(fqName))
    }
}