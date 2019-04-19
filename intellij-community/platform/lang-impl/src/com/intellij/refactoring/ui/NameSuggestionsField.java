// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.ui;

import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.EditorComboBoxEditor;
import com.intellij.ui.EditorComboBoxRenderer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.StringComboboxEditor;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class NameSuggestionsField extends JPanel {
  private final JComponent myComponent;
  private final EventListenerList myListenerList = new EventListenerList();
  private final MyComboBoxModel myComboBoxModel;
  private final Project myProject;
  private MyDocumentListener myDocumentListener;
  private MyComboBoxItemListener myComboBoxItemListener;

  private boolean myNonHumanChange = false;

  public NameSuggestionsField(Project project) {
    super(new BorderLayout());
    myProject = project;
    myComboBoxModel = new MyComboBoxModel();
    final ComboBox comboBox = new ComboBox(myComboBoxModel,-1);
    myComponent = comboBox;
    add(myComponent, BorderLayout.CENTER);
    setupComboBox(comboBox, StdFileTypes.JAVA);
  }

  public NameSuggestionsField(String[] nameSuggestions, Project project) {
    this(nameSuggestions, project, StdFileTypes.JAVA);
  }

  public NameSuggestionsField(String[] nameSuggestions, Project project, FileType fileType) {
    super(new BorderLayout());
    myProject = project;
    if (nameSuggestions == null || nameSuggestions.length <= 1) {
      myComponent = createTextFieldForName(nameSuggestions, fileType);
    }
    else {
      final ComboBox combobox = new ComboBox(nameSuggestions);
      combobox.setSelectedIndex(0);
      setupComboBox(combobox, fileType);
      myComponent = combobox;
    }
    add(myComponent, BorderLayout.CENTER);
    myComboBoxModel = null;
  }

  public NameSuggestionsField(final String[] suggestedNames, final Project project, final FileType fileType, @Nullable final Editor editor) {
    this(suggestedNames, project, fileType);
    if (editor == null) return;
    // later here because EditorTextField creates Editor during addNotify()
    final Runnable selectionRunnable = () -> {
      final int offset = editor.getCaretModel().getOffset();
      List<TextRange> ranges = new ArrayList<>();
      SelectWordUtil.addWordSelection(editor.getSettings().isCamelWords(), editor.getDocument().getCharsSequence(), offset, ranges);
      Editor myEditor = getEditor();
      if (myEditor == null) return;
      for (TextRange wordRange : ranges) {
        String word = editor.getDocument().getText(wordRange);
        if (!word.equals(getEnteredName())) continue;
        final SelectionModel selectionModel = editor.getSelectionModel();
        myEditor.getSelectionModel().removeSelection();
        final int wordRangeStartOffset = wordRange.getStartOffset();
        int myOffset = offset - wordRangeStartOffset;
        myEditor.getCaretModel().moveToOffset(myOffset);
        TextRange selected = new TextRange(Math.max(0, selectionModel.getSelectionStart() - wordRangeStartOffset),
                                           Math.max(0, selectionModel.getSelectionEnd() - wordRangeStartOffset));
        selected = selected.intersection(new TextRange(0, myEditor.getDocument().getTextLength()));
        if (selectionModel.hasSelection() && selected != null && !selected.isEmpty()) {
          myEditor.getSelectionModel().setSelection(selected.getStartOffset(), selected.getEndOffset());
        }
        else if (shouldSelectAll()) {
          myEditor.getSelectionModel().setSelection(0, myEditor.getDocument().getTextLength());
        }
        break;
      }
    };
    SwingUtilities.invokeLater(selectionRunnable);
  }

  protected boolean shouldSelectAll() {
    return true;
  }

  public void selectNameWithoutExtension() {
    SwingUtilities.invokeLater(() -> {
      Editor editor = getEditor();
      if (editor == null) return;
      final int pos = editor.getDocument().getText().lastIndexOf('.');
      if (pos > 0) {
        editor.getSelectionModel().setSelection(0, pos);
        editor.getCaretModel().moveToOffset(pos);
      }
    });

  }

  public void select(final int start, final int end) {
    SwingUtilities.invokeLater(() -> {
      Editor editor = getEditor();
      if (editor == null) return;
      editor.getSelectionModel().setSelection(start, end);
      editor.getCaretModel().moveToOffset(end);

    });
  }

  public void setSuggestions(final String[] suggestions) {
    if(myComboBoxModel == null) return;
    JComboBox comboBox = (JComboBox) myComponent;
    final String oldSelectedItem = (String)comboBox.getSelectedItem();
    final String oldItemFromTextField = (String) comboBox.getEditor().getItem();
    final boolean shouldUpdateTextField =
      oldItemFromTextField.equals(oldSelectedItem) || oldItemFromTextField.trim().length() == 0;
    myComboBoxModel.setSuggestions(suggestions);
    if(suggestions.length > 0 && shouldUpdateTextField) {
      if (oldSelectedItem != null) {
        comboBox.setSelectedItem(oldSelectedItem);
      } else {
        comboBox.setSelectedIndex(0);
      }
    }
    else {
      comboBox.getEditor().setItem(oldItemFromTextField);
    }
  }

  public JComponent getComponent() {
    return this;
  }

  public JComponent getFocusableComponent() {
    if(myComponent instanceof JComboBox) {
      return (JComponent) ((JComboBox) myComponent).getEditor().getEditorComponent();
    } else {
      return myComponent;
    }
  }

  public String getEnteredName() {
    if (myComponent instanceof JComboBox) {
      return (String)((JComboBox)myComponent).getEditor().getItem();
    } else {
      return ((EditorTextField) myComponent).getText();
    }
  }

  public boolean hasSuggestions() {
    return myComponent instanceof JComboBox;
  }

  private JComponent createTextFieldForName(String[] nameSuggestions, FileType fileType) {
    final String text;
    if (nameSuggestions != null && nameSuggestions.length > 0 && nameSuggestions[0] != null) {
      text = nameSuggestions[0];
    }
    else {
      text = "";
    }

    EditorTextField field = new EditorTextField(text, myProject, fileType);
    field.selectAll();
    return field;
  }

  private static class MyComboBoxModel extends DefaultComboBoxModel {
    private String[] mySuggestions;

    MyComboBoxModel() {
      mySuggestions = ArrayUtil.EMPTY_STRING_ARRAY;
    }

    // implements javax.swing.ListModel
    @Override
    public int getSize() {
      return mySuggestions.length;
    }

    // implements javax.swing.ListModel
    @Override
    public Object getElementAt(int index) {
      return mySuggestions[index];
    }

    public void setSuggestions(String[] suggestions) {
      fireIntervalRemoved(this, 0, mySuggestions.length);
      mySuggestions = suggestions;
      fireIntervalAdded(this, 0, mySuggestions.length);
    }

  }

  private void setupComboBox(final ComboBox combobox, FileType fileType) {
    final EditorComboBoxEditor comboEditor = new StringComboboxEditor(myProject, fileType, combobox) {
      @Override
      public void setItem(Object anObject) {
        myNonHumanChange = true;
        super.setItem(anObject);
      }
    };

    combobox.setEditor(comboEditor);
    combobox.setRenderer(new EditorComboBoxRenderer(comboEditor));

    combobox.setEditable(true);
    combobox.setMaximumRowCount(8);

    comboEditor.selectAll();
  }

  public Editor getEditor() {
    if (myComponent instanceof EditorTextField) {
      return ((EditorTextField)myComponent).getEditor();
    }
    else {
      return ((EditorTextField)((JComboBox)myComponent).getEditor().getEditorComponent()).getEditor();
    }
  }

  @FunctionalInterface
  public interface DataChanged extends EventListener {
    void dataChanged();
  }

  public void addDataChangedListener(DataChanged listener) {
    myListenerList.add(DataChanged.class, listener);
    attachListeners();
  }

  public void removeDataChangedListener(DataChanged listener) {
    myListenerList.remove(DataChanged.class, listener);
    if (myListenerList.getListenerCount() == 0) {
      detachListeners();
    }
  }

  private void attachListeners() {
    if (myDocumentListener == null) {
      myDocumentListener = new MyDocumentListener();
      ((EditorTextField) getFocusableComponent()).addDocumentListener(myDocumentListener);
    }
    if (myComboBoxItemListener == null && myComponent instanceof JComboBox) {
      myComboBoxItemListener = new MyComboBoxItemListener();
      ((JComboBox) myComponent).addItemListener(myComboBoxItemListener);
    }
  }

  private void detachListeners() {
    if (myDocumentListener != null) {
      ((EditorTextField) getFocusableComponent()).removeDocumentListener(myDocumentListener);
      myDocumentListener = null;
    }
    if (myComboBoxItemListener != null) {
      ((JComboBox) myComponent).removeItemListener(myComboBoxItemListener);
      myComboBoxItemListener = null;
    }
  }

  private void fireDataChanged() {
    Object[] list = myListenerList.getListenerList();

    for (Object aList : list) {
      if (aList instanceof DataChanged) {
        ((DataChanged)aList).dataChanged();
      }
    }
  }

  @Override
  public boolean requestFocusInWindow() {
    if(myComponent instanceof JComboBox) {
      return ((JComboBox) myComponent).getEditor().getEditorComponent().requestFocusInWindow();
    }
    else {
      return myComponent.requestFocusInWindow();
    }
  }

  @Override
  public void setEnabled (boolean enabled) {
    myComponent.setEnabled(enabled);
  }

  private class MyDocumentListener implements DocumentListener {
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
      if (!myNonHumanChange && myComponent instanceof JComboBox && ((JComboBox)myComponent).isPopupVisible()) {
        ((JComboBox)myComponent).hidePopup();
      }
      myNonHumanChange = false;

      fireDataChanged();
    }
  }

  private class MyComboBoxItemListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      fireDataChanged();
    }
  }
}
