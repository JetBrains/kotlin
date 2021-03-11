/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.printing

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.nj2k.JKImportStorage
import org.jetbrains.kotlin.nj2k.escaped
import org.jetbrains.kotlin.nj2k.symbols.*
import org.jetbrains.kotlin.nj2k.tree.JKClassAccessExpression
import org.jetbrains.kotlin.nj2k.tree.JKQualifiedExpression
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class JKSymbolRenderer(private val importStorage: JKImportStorage, project: Project) {
    private val canBeShortenedClassNameCache = CanBeShortenedCache(project)

    private fun JKSymbol.isFqNameExpected(owner: JKTreeElement?): Boolean {
        if (owner?.isSelectorOfQualifiedExpression() == true) return false
        return this is JKClassSymbol || isStaticMember || isEnumConstant
    }

    private fun JKSymbol.isFromJavaLangPackage() =
        fqName.startsWith(JAVA_LANG_FQ_PREFIX)

    fun renderSymbol(symbol: JKSymbol, owner: JKTreeElement?): String {
        val name = symbol.name.escaped()
        if (!symbol.isFqNameExpected(owner)) return name
        val fqName = symbol.getDisplayFqName().escapedAsQualifiedName()
        if (owner is JKClassAccessExpression && symbol.isFromJavaLangPackage()) return fqName

        return when {
            symbol is JKClassSymbol && canBeShortenedClassNameCache.canBeShortened(symbol) -> {
                importStorage.addImport(fqName)
                name
            }
            symbol.isStaticMember && symbol.containingClass?.isUnnamedCompanion == true -> {
                val containingClass = symbol.containingClass ?: return fqName
                val classContainingCompanion = containingClass.containingClass ?: return fqName
                if (!canBeShortenedClassNameCache.canBeShortened(classContainingCompanion)) return fqName
                importStorage.addImport(classContainingCompanion.getDisplayFqName())
                "${classContainingCompanion.name.escaped()}.${SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT}.$name"
            }

            symbol.isEnumConstant || symbol.isStaticMember -> {
                val containingClass = symbol.containingClass ?: return fqName
                if (!canBeShortenedClassNameCache.canBeShortened(containingClass)) return fqName
                importStorage.addImport(containingClass.getDisplayFqName())
                "${containingClass.name.escaped()}.$name"
            }

            else -> fqName
        }
    }

    private fun JKTreeElement.isSelectorOfQualifiedExpression() =
        parent?.safeAs<JKQualifiedExpression>()?.selector == this

    companion object {
        private const val JAVA_LANG_FQ_PREFIX = "java.lang"
    }
}

private class CanBeShortenedCache(project: Project) {
    private val shortNameCache = PsiShortNamesCache.getInstance(project)
    private val searchScope = GlobalSearchScope.allScope(project)
    private val canBeShortenedCache = mutableMapOf<String, Boolean>().apply {
        CLASS_NAMES_WHICH_HAVE_DIFFERENT_MEANINGS_IN_KOTLIN_AND_JAVA.forEach { name ->
            this[name] = false
        }
    }

    fun canBeShortened(symbol: JKClassSymbol): Boolean = canBeShortenedCache.getOrPut(symbol.name) {
        var symbolsWithSuchNameCount = 0
        val processSymbol = { _: PsiClass ->
            symbolsWithSuchNameCount++
            symbolsWithSuchNameCount <= 1 //stop if met more than one symbol with such name
        }
        shortNameCache.processClassesWithName(symbol.name, processSymbol, searchScope, null)
        symbolsWithSuchNameCount == 1
    }

    companion object {
        private val CLASS_NAMES_WHICH_HAVE_DIFFERENT_MEANINGS_IN_KOTLIN_AND_JAVA = setOf(
            "Function",
            "Serializable"
        )
    }
}