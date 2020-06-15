/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search

import com.intellij.psi.search.PsiTodoSearchHelper
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.junit.internal.runners.JUnit38ClassRunner
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.TestRoot
import org.junit.runner.RunWith

@TestRoot("idea")
@TestMetadata("testData/search/todo")
@RunWith(JUnit38ClassRunner::class)
class TodoSearchTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    fun testTodoCall() {
        val file = myFixture.configureByFile("todoCall.kt")
        val todoItems = PsiTodoSearchHelper.SERVICE.getInstance(myFixture.project).findTodoItems(file)

        val actualItems = todoItems.map { it.textRange.substring(it.file.text) }
        assertOrderedEquals(
            listOf(
                "TODO(\"Fix me\")",
                "TODO()",
                "TODO(\"Fix me in lambda\")"
            ).sorted(),
            actualItems.sorted()
        )
    }

    fun testTodoDef() {
        val file = myFixture.configureByFile("todoDecl.kt")
        assertEquals(0, PsiTodoSearchHelper.SERVICE.getInstance(myFixture.project).getTodoItemsCount(file))
    }
}
