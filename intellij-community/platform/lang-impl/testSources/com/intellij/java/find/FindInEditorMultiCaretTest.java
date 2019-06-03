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
package com.intellij.java.find;

import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.testFramework.fixtures.EditorMouseFixture;

public class FindInEditorMultiCaretTest extends AbstractFindInEditorTest {
  public void testBasic() {
    init("abc\n" +
         "abc\n" +
         "abc");
    initFind();
    setTextToFind("b");
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "abc\n" +
                      "abc");
    addOccurrence();
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "a<selection>b<caret></selection>c\n" +
                      "abc");
    nextOccurrence();
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "abc\n" +
                      "a<selection>b<caret></selection>c");
    prevOccurrence();
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "a<selection>b<caret></selection>c\n" +
                      "abc");
    removeOccurrence();
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "abc\n" +
                      "abc");
    allOccurrences();
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "a<selection>b<caret></selection>c\n" +
                      "a<selection>b<caret></selection>c");
    assertNull(getEditorSearchComponent());
  }

  public void testActionsInEditorWorkIndependently() {
    init("abc\n" +
         "abc\n" +
         "abc");
    initFind();
    setTextToFind("b");
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "abc\n" +
                      "abc");
    new EditorMouseFixture((EditorImpl)myFixture.getEditor()).clickAt(0, 1);
    addOccurrenceFromEditor();
    addOccurrenceFromEditor();
    checkResultByText("<selection>a<caret>bc</selection>\n" +
                      "<selection>a<caret>bc</selection>\n" +
                      "abc");
    nextOccurrenceFromEditor();
    checkResultByText("<selection>a<caret>bc</selection>\n" +
                      "abc\n" +
                      "<selection>a<caret>bc</selection>");
    prevOccurrenceFromEditor();
    checkResultByText("<selection>a<caret>bc</selection>\n" +
                      "<selection>a<caret>bc</selection>\n" +
                      "abc");
    removeOccurrenceFromEditor();
    checkResultByText("<selection>a<caret>bc</selection>\n" +
                      "abc\n" +
                      "abc");
    allOccurrencesFromEditor();
    checkResultByText("<selection>a<caret>bc</selection>\n" +
                      "<selection>a<caret>bc</selection>\n" +
                      "<selection>a<caret>bc</selection>");
    assertNotNull(getEditorSearchComponent());
  }

  public void testCloseRetainsMulticaretSelection() {
    init("abc\n" +
         "abc\n" +
         "abc");
    initFind();
    setTextToFind("b");
    addOccurrence();
    closeFind();
    checkResultByText("a<selection>b<caret></selection>c\n" +
                      "a<selection>b<caret></selection>c\n" +
                      "abc");
  }

  public void testTextModificationRemovesOldSelections() {
    init("abc\n" +
         "abc\n" +
         "abc");
    initFind();
    setTextToFind("b");
    addOccurrence();
    setTextToFind("bc");

    assertEquals(1, myFixture.getEditor().getCaretModel().getCaretCount());
    assertEquals("bc", myFixture.getEditor().getSelectionModel().getSelectedText());
  }

  public void testSecondFindNavigatesToTheSameOccurrence() {
    init("ab<caret>c\n" +
         "abc\n" +
         "abc");
    initFind();
    setTextToFind("abc");
    checkResultByText("abc\n" +
                      "<selection>abc<caret></selection>\n" +
                      "abc");
    closeFind();
    initFind();
    setTextToFind("abc");
    checkResultByText("abc\n" +
                      "<selection>abc<caret></selection>\n" +
                      "abc");
  }
  
  public void testFindNextRetainsOnlyOneCaretIfNotUsedAsMoveToNextOccurrence() {
    init("<caret>To be or not to be?");
    initFind();
    setTextToFind("be");
    checkResultByText("To <selection>be<caret></selection> or not to be?");
    closeFind();
    new EditorMouseFixture((EditorImpl)myFixture.getEditor()).alt().shift().clickAt(0, 8); // adding second caret
    checkResultByText("To <selection>be<caret></selection> or<caret> not to be?");
    nextOccurrenceFromEditor();
    checkResultByText("To be or not to <selection>be<caret></selection>?");
  }

  public void testSelectAllDuringReplace() {
    init("some text");
    initReplace();
    setTextToFind("e");
    allOccurrences();
    checkResultByText("som<selection>e<caret></selection> t<selection>e<caret></selection>xt");
  }
}
