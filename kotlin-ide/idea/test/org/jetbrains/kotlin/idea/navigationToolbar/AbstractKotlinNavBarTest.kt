/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigationToolbar

import com.intellij.ide.navigationToolbar.NavBarModel
import com.intellij.ide.navigationToolbar.NavBarModelExtension
import com.intellij.ide.navigationToolbar.NavBarPresentation
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.ex.EditorEx
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils


abstract class AbstractKotlinNavBarTest : KotlinLightCodeInsightFixtureTestCase() {

    // inspired by: com.intellij.ide.navigationToolbar.JavaNavBarTest#assertNavBarModel
    protected fun doTest(testPath: String) {
        val psiFile = myFixture.configureByFile(testDataFile().name)
        val model = NavBarModel(myFixture.project)

        model::class.java.getDeclaredMethod("updateModel", DataContext::class.java)
            .apply { isAccessible = true }
            .invoke(model, (myFixture.editor as EditorEx).dataContext)

        val actualItems = (0 until model.size()).map {
            NavBarPresentation.calcPresentableText(model[it], false)
        }
        val expectedItems = InTextDirectivesUtils.findListWithPrefixes(psiFile.text, "// NAV_BAR_ITEMS:")
        assertOrderedEquals(actualItems, expectedItems)
    }
}
