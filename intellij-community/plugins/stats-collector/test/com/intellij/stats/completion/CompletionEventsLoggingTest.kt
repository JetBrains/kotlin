/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.stats.completion.Action.*
import com.intellij.stats.completion.events.ExplicitSelectEvent
import org.assertj.core.api.Assertions.assertThat


class CompletionEventsLoggingTest : CompletionLoggingTestBase() {

    fun `test item selected on just typing`() {
        myFixture.type('.')
        myFixture.completeBasic()
        val itemsOnStart = lookup.items
        myFixture.type("ru")

        trackedEvents.assertOrder(
          COMPLETION_STARTED,
          TYPE
        )

        myFixture.type("n)")

        trackedEvents.assertOrder(
          COMPLETION_STARTED,
          TYPE,
          TYPE,
          TYPE,
          EXPLICIT_SELECT //should be TYPED_SELECT investigate
        )

        checkLoggedAllElements(itemsOnStart)
        checkSelectedCorrectId(itemsOnStart, "run")
    }

    private fun checkLoggedAllElements(itemsOnStart: MutableList<LookupElement>) {
        assertThat(completionStartedEvent.newCompletionListItems).hasSize(itemsOnStart.size)
        assertThat(completionStartedEvent.completionListIds).hasSize(itemsOnStart.size)
    }

    private fun checkSelectedCorrectId(itemsOnStart: MutableList<LookupElement>, selectedString: String) {
        val selectedIndex = itemsOnStart.indexOfFirst { it.lookupString == selectedString }
        val selectedId = completionStartedEvent.completionListIds[selectedIndex]
        val select = trackedEvents.last() as ExplicitSelectEvent
        assertThat(select.selectedId).isEqualTo(selectedId)
    }

    fun `test wrong typing`() {
        myFixture.type('.')
        myFixture.completeBasic()

        myFixture.type('r')
        myFixture.type('u')
        myFixture.type('x')

        lookup.hide() //figure out why needed here

        trackedEvents.assertOrder(
          COMPLETION_STARTED,
          TYPE,
          TYPE,
          COMPLETION_CANCELED
        )
    }
    
    fun `test down up buttons`() {
        myFixture.type('.')
        myFixture.completeBasic()
        val elementsOnStart = lookup.items

        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)

        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)

        myFixture.type('\n')


        trackedEvents.assertOrder(
          COMPLETION_STARTED,
          DOWN,
          DOWN,
          DOWN,
          UP,
          UP,
          UP,
          EXPLICIT_SELECT
        )

        checkLoggedAllElements(elementsOnStart)
        checkSelectedCorrectId(elementsOnStart, elementsOnStart.first().lookupString)
    }


    fun `test backspace`() {
        myFixture.type('.')
        myFixture.completeBasic()
        val elementsOnStart = lookup.items

        myFixture.type("ru")
        myFixture.performEditorAction(IdeActions.ACTION_EDITOR_BACKSPACE)
        myFixture.type('u')
        myFixture.type('\n')

        trackedEvents.assertOrder(
          COMPLETION_STARTED,
          TYPE,
          TYPE,
          BACKSPACE,
          TYPE,
          EXPLICIT_SELECT
        )

        checkSelectedCorrectId(elementsOnStart, "run")
        checkLoggedAllElements(elementsOnStart)
    }

    fun `test if typed prefix is correct completion variant, pressing dot will select it`() {
        myFixture.completeBasic()
        val elementsOnStart = lookup.items
        myFixture.type('.')

        trackedEvents.assertOrder(
          COMPLETION_STARTED,
          EXPLICIT_SELECT
        )

        checkLoggedAllElements(elementsOnStart)
    }
    
}