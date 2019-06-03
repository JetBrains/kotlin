// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.java.find;

import com.intellij.find.EditorSearchSession;
import com.intellij.find.editorHeaderActions.*;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.impl.DataManagerImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import javax.swing.text.JTextComponent;

public abstract class AbstractFindInEditorTest extends LightPlatformCodeInsightFixtureTestCase {
  protected void setTextToFind(String text) {
    EditorSearchSession editorSearchSession = getEditorSearchComponent();
    assertNotNull(editorSearchSession);
    JTextComponent searchField = editorSearchSession.getComponent().getSearchTextComponent();
    assertNotNull(searchField);
    for (int i = 0; i <= text.length(); i++) {
      searchField.setText(text.substring(0, i)); // emulate typing chars one by one
      IdeEventQueue.getInstance().flushQueue();
    }
  }

  protected void nextOccurrence() {
    executeHeaderAction(new NextOccurrenceAction());
  }

  protected void prevOccurrence() {
    executeHeaderAction(new PrevOccurrenceAction());
  }

  protected void addOccurrence() {
    executeHeaderAction(new AddOccurrenceAction());
  }

  protected void removeOccurrence() {
    executeHeaderAction(new RemoveOccurrenceAction());
  }

  protected void allOccurrences() {
    executeHeaderAction(new SelectAllAction());
  }

  protected void nextOccurrenceFromEditor() {
    myFixture.performEditorAction(IdeActions.ACTION_FIND_NEXT);
  }

  protected void prevOccurrenceFromEditor() {
    myFixture.performEditorAction(IdeActions.ACTION_FIND_PREVIOUS);
  }

  protected void addOccurrenceFromEditor() {
    myFixture.performEditorAction(IdeActions.ACTION_SELECT_NEXT_OCCURENCE);
  }

  protected void removeOccurrenceFromEditor() {
    myFixture.performEditorAction(IdeActions.ACTION_UNSELECT_PREVIOUS_OCCURENCE);
  }

  protected void allOccurrencesFromEditor() {
    myFixture.performEditorAction(IdeActions.ACTION_SELECT_ALL_OCCURRENCES);
  }

  protected void closeFind() {
    EditorSearchSession editorSearchSession = getEditorSearchComponent();
    editorSearchSession.close();
  }

  protected void executeHeaderAction(AnAction action) {
    DataContext context = new DataManagerImpl.MyDataContext(getEditorSearchComponent().getComponent());
    AnActionEvent e = AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_TOOLBAR, null, context);
    action.beforeActionPerformedUpdate(e);
    if (e.getPresentation().isEnabled() && e.getPresentation().isVisible()) {
      action.actionPerformed(e);
    }
  }

  protected void initFind() {
    myFixture.performEditorAction(IdeActions.ACTION_FIND);
  }

  protected void initReplace() {
    myFixture.performEditorAction(IdeActions.ACTION_REPLACE);
  }

  protected EditorSearchSession getEditorSearchComponent() {
    return EditorSearchSession.get(myFixture.getEditor());
  }

  protected void init(String text) {
    myFixture.configureByText(getTestName(false) + ".txt", text);
  }

  protected void checkResultByText(String text) {
    myFixture.checkResult(text);
  }

  protected Editor getEditor() {
    return myFixture.getEditor();
  }
}
