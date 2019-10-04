/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices

class JKImportStorage(languageSettings: LanguageVersionSettings) {
    private val imports = mutableSetOf<FqName>()

    private val defaultImports: Set<FqName> =
        JvmPlatformAnalyzerServices.getDefaultImports(
            languageSettings,
            includeLowPriorityImports = true
        ).mapNotNull { import ->
            if (import.isAllUnder) null else import.fqName
        }.toSet()

    fun addImport(import: FqName) {
        if (isImportNeeded(import, allowSingleIdentifierImport = false)) {
            imports += import
        }
    }

    fun addImport(import: String) {
        addImport(FqName(import))
    }

    fun getImports(): Set<FqName> = imports

    fun isImportNeeded(fqName: FqName, allowSingleIdentifierImport: Boolean = false): Boolean {
        if (!allowSingleIdentifierImport && fqName.asString().count { it == '.' } < 1) return false
        if (fqName in NULLABILITY_ANNOTATIONS) return false
        if (fqName in defaultImports) return false
        return true
    }

    fun isImportNeeded(fqName: String, allowSingleIdentifierImport: Boolean = false): Boolean =
        isImportNeeded(FqName(fqName), allowSingleIdentifierImport)

    companion object {
        private val JAVA_TYPE_WRAPPERS_WHICH_HAVE_CONFLICTS_WITH_KOTLIN_ONES = setOf(
            FqName(CommonClassNames.JAVA_LANG_BYTE),
            FqName(CommonClassNames.JAVA_LANG_SHORT),
            FqName(CommonClassNames.JAVA_LANG_LONG),
            FqName(CommonClassNames.JAVA_LANG_FLOAT),
            FqName(CommonClassNames.JAVA_LANG_DOUBLE)
        )

        private val SHORT_NAMES = JAVA_TYPE_WRAPPERS_WHICH_HAVE_CONFLICTS_WITH_KOTLIN_ONES
            .map { it.shortName().identifier }
            .toSet()

        fun isImportNeededForCall(qualifiedExpression: KtQualifiedExpression): Boolean {
            val shortName = qualifiedExpression.getCalleeExpressionIfAny()?.text ?: return true
            if (shortName !in SHORT_NAMES) return true
            val fqName = qualifiedExpression.selectorExpression?.mainReference?.resolve()?.getKotlinFqName() ?: return true
            return fqName !in JAVA_TYPE_WRAPPERS_WHICH_HAVE_CONFLICTS_WITH_KOTLIN_ONES
        }
    }
}