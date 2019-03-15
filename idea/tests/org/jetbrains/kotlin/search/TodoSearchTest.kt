/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search

import com.intellij.psi.search.PsiTodoSearchHelper
import com.intellij.psi.search.TodoItem
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

class TodoSearchTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/search/todo").path + File.separator
    }

    fun testTodoCall() {
        val file = myFixture.configureByFile("todoCall.kt")
        val todoItems = PsiTodoSearchHelper.SERVICE.getInstance(myFixture.project).findTodoItems(file)
        assertEquals(3, todoItems.size)
        verifyTodoItemText(todoItems[0], "TODO(\"Fix me\")")
        verifyTodoItemText(todoItems[1], "TODO()")
        verifyTodoItemText(todoItems[2], "TODO(\"Fix me in lambda\")")
    }

    private fun verifyTodoItemText(todoItem: TodoItem, s: String) {
        assertEquals(s, todoItem.textRange.substring(todoItem.file.text))
    }

    fun testTodoDef() {
        val file = myFixture.configureByFile("todoDecl.kt")
        assertEquals(0, PsiTodoSearchHelper.SERVICE.getInstance(myFixture.project).getTodoItemsCount(file))
    }
}
