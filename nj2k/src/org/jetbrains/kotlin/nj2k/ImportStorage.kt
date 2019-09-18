/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny

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

        fun isImportNeededForCall(qualifiedExpression: KtQualifiedExpression): Boolean {
            val shortName = qualifiedExpression.getCalleeExpressionIfAny()?.text ?: return true
            if (shortName !in SHORT_NAMES) return true
            val fqName = qualifiedExpression.selectorExpression?.mainReference?.resolve()?.getKotlinFqName() ?: return true
            return isImportNeeded(fqName)
        }

        fun isImportNeeded(fqName: String): Boolean = isImportNeeded(FqName(fqName))


        private val JAVA_TYPE_WRAPPERS_WHICH_HAVE_CONFLICTS_WITH_KOTLIN_ONES = setOf(
            FqName(CommonClassNames.JAVA_LANG_BYTE),
            FqName(CommonClassNames.JAVA_LANG_SHORT),
            FqName(CommonClassNames.JAVA_LANG_LONG),
            FqName(CommonClassNames.JAVA_LANG_FLOAT),
            FqName(CommonClassNames.JAVA_LANG_DOUBLE)
        )

        private val SHORT_NAMES =
            (NULLABILITY_ANNOTATIONS + JAVA_TYPE_WRAPPERS_WHICH_HAVE_CONFLICTS_WITH_KOTLIN_ONES)
                .map { it.shortName().asString() }
                .toSet()
    }
}