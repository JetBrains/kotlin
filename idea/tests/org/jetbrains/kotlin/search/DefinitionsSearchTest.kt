/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

class DefinitionsSearchTest : AbstractSearcherTest() {
    fun testNestedClass() {
        doTest()
    }

    private fun doTest() {
        myFixture.configureByFile(fileName)
        val directives = InTextDirectivesUtils.findListWithPrefixes(FileUtil.loadFile(File(pathToFile), true), "// CLASS: ")
        assertFalse("Specify CLASS directive in test file", directives.isEmpty())
        val superClassName = directives[0]
        val psiClass = getPsiClass(superClassName)
        checkResult(getPathToFile(), DefinitionsScopedSearch.search(psiClass))

        val origin = (psiClass as? KtLightClass)?.kotlinOrigin!!
        checkResult(getPathToFile(), DefinitionsScopedSearch.search(origin))
    }

    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/search/definitions").path + File.separator

    }
}