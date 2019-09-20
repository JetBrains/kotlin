// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find;

import com.intellij.find.editorHeaderActions.AddOccurrenceAction;
import com.intellij.find.editorHeaderActions.RemoveOccurrenceAction;
import com.intellij.find.editorHeaderActions.ToggleSelectionOnlyAction;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.HeadlessDataManager;
import com.intellij.idea.IdeaTestApplication;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.UIUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public class FindInEditorFunctionalTest extends AbstractFindInEditorTest {
  @Override
  protected void setUp() throws Exception {
    IdeaTestApplication.getInstance();
    Application application = ApplicationManager.getApplication();
    // Necessary to properly update button states
    ServiceContainerUtil.replaceService(application, DataManager.class, new HeadlessDataManager() {
      @NotNull
      @Override
      public DataContext getDataContext(Component component) {
        if (component instanceof SearchReplaceComponent) {
          return ((SearchReplaceComponent)component)::getData;
        }
        return super.getDataContext(component);
      }
    }, getTestRootDisposable());
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      EditorSearchSession component = getEditorSearchComponent();
      if (component != null) {
        FindModel model = component.getFindModel();
        model.setRegularExpressions(false);
        model.setWholeWordsOnly(false);
        model.setCaseSensitive(false);
      }
    }
    finally {
      super.tearDown();
    }
  }

  public void testFindInSelection() {
    String origText = "first foo\n<selection>foo bar baz\nbaz bar foo</selection>\nlast foo";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    SearchReplaceComponent component = session.getComponent();
    Editor editor = getEditor();
    assertSame(editor, session.getEditor());
    Map<Class<?>, ActionButton> actions = StreamEx.of(UIUtil.findComponentsOfType(component, ActionButton.class))
      .toMap(button -> button.getAction().getClass(), Function.identity(), (a, b) -> null);
    model.setGlobal(false);// 'Find' action puts multiline text in search field (in contrast to 'Replace' action)
    assertFalse(model.isGlobal()); // multiline selection = no-global
    assertEquals(ActionButtonComponent.PUSHED, actions.get(ToggleSelectionOnlyAction.class).getPopState());
    assertFalse(actions.get(AddOccurrenceAction.class).isEnabled());
    assertFalse(actions.get(RemoveOccurrenceAction.class).isEnabled());
    ShortcutSet shortcuts = actions.get(ToggleSelectionOnlyAction.class).getAction().getShortcutSet();
    assertEquals(ActionManager.getInstance().getAction(IdeActions.ACTION_TOGGLE_FIND_IN_SELECTION_ONLY).getShortcutSet(), shortcuts);

    model.setStringToFind("foo");
    assertEquals(ApplicationBundle.message("editorsearch.matches", 2), component.getStatusText());
    checkResultByText(origText);

    model.setGlobal(true);
    assertEquals(ActionButtonComponent.NORMAL, actions.get(ToggleSelectionOnlyAction.class).getPopState());
    assertTrue(actions.get(AddOccurrenceAction.class).isEnabled());
    assertTrue(actions.get(RemoveOccurrenceAction.class).isEnabled());
    assertEquals(ApplicationBundle.message("editorsearch.matches", 4), component.getStatusText());
    checkResultByText("first foo\n<selection>foo</selection> bar baz\n" +
                      "baz bar foo\nlast foo");
    model.setGlobal(false); // restore selection
    checkResultByText(origText);
    assertEquals(ApplicationBundle.message("editorsearch.matches", 2), component.getStatusText());
    // React on selection change
    editor.getSelectionModel().setSelection(0, editor.getDocument().getLineEndOffset(2));
    assertEquals(ApplicationBundle.message("editorsearch.matches", 3), component.getStatusText());
    editor.getSelectionModel().removeSelection();
    assertEquals(ApplicationBundle.message("editorsearch.noselection"), component.getStatusText());

    closeFind();
    init(origText);
    editor.getSelectionModel().removeSelection();
    initFind();
    session = getEditorSearchComponent();
    model = session.getFindModel();
    assertEquals("foo", model.getStringToFind());

    closeFind();
    init(origText);
    initFind();
    session = getEditorSearchComponent();
    model = session.getFindModel();
    assertEquals("foo bar baz\nbaz bar foo", editor.getSelectionModel().getSelectedText());
    assertEquals(editor.getSelectionModel().getSelectedText(), model.getStringToFind());//Don't use multilite selection as string to find
  }

  public void testFindEmptyText() {
    String origText = "first foo\n<selection>foo bar baz\nbaz bar foo</selection>\nlast foo";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    SearchReplaceComponent component = session.getComponent();
    model.setStringToFind("");
    model.setGlobal(false);
    Shortcut shortcut = ArrayUtil.getFirstElement(ActionManager.getInstance().getAction(IdeActions.ACTION_TOGGLE_FIND_IN_SELECTION_ONLY).getShortcutSet().getShortcuts());
    if (shortcut != null) {
      assertEquals(ApplicationBundle.message("editorsearch.in.selection.with.hint", KeymapUtil.getShortcutText(shortcut)),
                   ((ComponentWithEmptyText)component.getSearchTextComponent()).getEmptyText().getText());
    } else {
      assertEquals(ApplicationBundle.message("editorsearch.in.selection"),
                   ((ComponentWithEmptyText)component.getSearchTextComponent()).getEmptyText().getText());

    }
    getEditor().getSelectionModel().removeSelection();
    assertEquals(ApplicationBundle.message("editorsearch.in.selection"), ((ComponentWithEmptyText)component.getSearchTextComponent()).getEmptyText().getText());
  }

  public void testFindToggleInSelection() {
    String origText = "first foo\n<selection>foo bar baz\nbaz bar foo</selection>\nlast foo";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    assertTrue(model.isGlobal());
    assertEquals("foo bar baz\nbaz bar foo", model.getStringToFind());
    model.setGlobal(false);
    assertEquals("", model.getStringToFind());
  }
  public void testReplaceToggleInSelection() {
    String origText = "first foo\n<selection>foo bar baz\nbaz bar foo</selection>\nlast foo";
    init(origText);
    initReplace();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    assertFalse(model.isGlobal());
    assertEquals("", model.getStringToFind());
    model.setGlobal(true);
    assertEquals("foo bar baz\nbaz bar foo", model.getStringToFind());
    model.setGlobal(false);
    assertEquals("", model.getStringToFind());
  }

  public void testFindSingleWord() {
    String origText = "first foo\n<selection>foo</selection> bar baz\nbaz bar foo\nlast foo";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    assertEquals("foo", model.getStringToFind());
    assertTrue(model.isGlobal());
  }

  public void testFindSetTextInField() {
    String origText = "first foo\nfoo bar baz\nbaz bar foo\nlast foo";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    session.setTextInField("baz bar");
    assertEquals("baz bar", model.getStringToFind());
    assertEquals("baz bar", session.getComponent().getSearchTextComponent().getText());
  }

  public void testFindRegexp() {
    String origText = "first foo\nfoo bar baz\nbaz bar foo\nlast foo";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    SearchReplaceComponent component = session.getComponent();
    JBCheckBox checkBox = ContainerUtil.find(UIUtil.findComponentsOfType(component, JBCheckBox.class),
                                             cb -> cb.getText().equals("Regex"));

    assertFalse(model.isRegularExpressions()); // default is false
    assertFalse(checkBox.isSelected());
    model.setStringToFind("[");
    assertEquals(ApplicationBundle.message("editorsearch.matches", 0), component.getStatusText());

    model.setRegularExpressions(true);
    assertTrue(checkBox.isSelected());
    assertEquals(SearchSession.INCORRECT_REGEX_MESSAGE, component.getStatusText());
    model.setStringToFind("|");
    assertEquals(ApplicationBundle.message("editorsearch.empty.string.matches"), component.getStatusText());
    model.setStringToFind("ba.?");
    assertEquals(ApplicationBundle.message("editorsearch.matches", 4), component.getStatusText());
  }

  public void testFindWholeWords() {
    String origText = "ab abc abcd";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    SearchReplaceComponent component = session.getComponent();
    JBCheckBox checkBox = ContainerUtil.find(UIUtil.findComponentsOfType(component, JBCheckBox.class),
                                             cb -> cb.getText().equals("Words"));

    assertFalse(model.isWholeWordsOnly()); // default is false
    assertFalse(checkBox.isSelected());
    model.setStringToFind("abc");
    assertEquals(ApplicationBundle.message("editorsearch.matches", 2), component.getStatusText());
    model.setWholeWordsOnly(true);
    assertTrue(checkBox.isSelected());
    assertEquals(ApplicationBundle.message("editorsearch.matches", 1), component.getStatusText());
  }

  public void testFindCaseSensitive() {
    String origText = "ab Ab AB aB";
    init(origText);
    initFind();
    EditorSearchSession session = getEditorSearchComponent();
    FindModel model = session.getFindModel();
    SearchReplaceComponent component = session.getComponent();
    JBCheckBox checkBox = ContainerUtil.find(UIUtil.findComponentsOfType(component, JBCheckBox.class),
                                             cb -> cb.getText().equals("Match Case"));

    assertFalse(model.isCaseSensitive()); // default is false
    assertFalse(checkBox.isSelected());
    model.setStringToFind("ab");
    assertEquals(ApplicationBundle.message("editorsearch.matches", 4), component.getStatusText());
    model.setCaseSensitive(true);
    assertTrue(checkBox.isSelected());
    assertEquals(ApplicationBundle.message("editorsearch.matches", 1), component.getStatusText());
  }

  public void testNextFromFoldedRegion() {
    init("<caret>first line\nsecond line");
    initFind();
    setTextToFind("line");
    checkResultByText("first <selection>line</selection>\nsecond line");
    getEditor().getFoldingModel().runBatchFoldingOperation(() ->
                                                             getEditor().getFoldingModel().addFoldRegion(0, 11, "...").setExpanded(false));
    checkResultByText("<caret>first line\nsecond line");
    nextOccurrence();
    checkResultByText("first <selection>line</selection>\nsecond line");
    nextOccurrence();
    checkResultByText("first line\nsecond <selection>line</selection>");
  }

  public void testAllHighlightersAreRemovedAfterSessionFinish() {
    init("some text");
    initFind();
    setTextToFind("e");
    allOccurrences();
    assertEmpty(getEditor().getMarkupModel().getAllHighlighters());
  }
}
