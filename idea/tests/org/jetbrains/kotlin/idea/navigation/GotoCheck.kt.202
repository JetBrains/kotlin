/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.ide.util.gotoByName.FilteringGotoByModel
import com.intellij.ide.util.gotoByName.LanguageRef
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.junit.Assert
import kotlin.test.assertEquals

object GotoCheck {
    @JvmStatic
    @JvmOverloads
    fun checkGotoDirectives(
        model: FilteringGotoByModel<LanguageRef>,
        editor: Editor,
        nonProjectSymbols: Boolean = false,
        checkNavigation: Boolean = false
    ) {
        val documentText = editor.document.text
        val searchTextList = InTextDirectivesUtils.findListWithPrefixes(documentText, "// SEARCH_TEXT:")
        Assert.assertFalse(
            "There's no search text in test data file given. Use '// SEARCH_TEXT:' directive",
            searchTextList.isEmpty()
        )

        val expectedReferences =
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(documentText, "// REF:").map { input -> input.trim { it <= ' ' } }
        val includeNonProjectSymbols = nonProjectSymbols || InTextDirectivesUtils.isDirectiveDefined(documentText, "// CHECK_BOX")

        val searchText = searchTextList.first()

        val foundSymbols = model.getNames(includeNonProjectSymbols).filter { it?.startsWith(searchText) ?: false }.flatMap {
            model.getElementsByName(it, includeNonProjectSymbols, "$it*").toList()
        }

        val inexactMatching = InTextDirectivesUtils.isDirectiveDefined(documentText, "// ALLOW_MORE_RESULTS")
        val renderedSymbols = foundSymbols.map { (it as PsiElement).renderAsGotoImplementation() }

        if (checkNavigation && (expectedReferences.size != 1 || inexactMatching)) {
            error("Cannot check navigation targets when multiple references are expected")
        }

        if (inexactMatching) {
            UsefulTestCase.assertContainsElements(renderedSymbols, expectedReferences)
        } else {
            UsefulTestCase.assertOrderedEquals(renderedSymbols.sorted(), expectedReferences)
        }
        if (!checkNavigation) return

        assertNavigationElementMatches(foundSymbols.single() as PsiElement, documentText)
    }

    @JvmStatic
    fun assertNavigationElementMatches(resolved: PsiElement, textWithDirectives: String) {
        val expectedBinaryFile = InTextDirectivesUtils.findStringWithPrefixes(textWithDirectives, "// BINARY:")
        val expectedSourceFile = InTextDirectivesUtils.findStringWithPrefixes(textWithDirectives, "// SRC:")
        assertEquals(expectedBinaryFile, getFileWithDir(resolved))
        val srcElement = resolved.navigationElement
        Assert.assertNotEquals(srcElement, resolved)
        assertEquals(expectedSourceFile, getFileWithDir(srcElement))
    }

    // TODO: move somewhere
    fun getFileWithDir(resolved: PsiElement): String {
        val targetFile = resolved.containingFile
        val targetDir = targetFile.parent
        return targetDir!!.name + "/" + targetFile.name
    }
}