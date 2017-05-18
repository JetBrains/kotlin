/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.search

import com.intellij.psi.search.PsiTodoSearchHelper
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
        assertEquals(1, todoItems.size)
        assertEquals("TODO(\"Fix me\")", todoItems[0].textRange.substring(todoItems[0].file.text))
    }

    fun testTodoDef() {
        val file = myFixture.configureByFile("todoDecl.kt")
        assertEquals(0, PsiTodoSearchHelper.SERVICE.getInstance(myFixture.project).getTodoItemsCount(file))
    }
}
