// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * ComboBox with Editor and Strings as item
 *
 * @author dsl
 */
public class StringComboboxEditor extends EditorComboBoxEditor {
  public static final Key<JComboBox> COMBO_BOX_KEY = Key.create("COMBO_BOX_KEY");
  public static final Key<Boolean> USE_PLAIN_PREFIX_MATCHER = Key.create("USE_PLAIN_PREFIX_MATCHER");

  private final Project myProject;

  public StringComboboxEditor(final Project project, final FileType fileType, ComboBox comboBox) {
    this(project, fileType, comboBox, false);
  }

  public StringComboboxEditor(final Project project, final FileType fileType, ComboBox comboBox, boolean usePlainMatcher) {
    super(project, fileType);
    myProject = project;
    final PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("a.dummy", FileTypes.PLAIN_TEXT, "", 0, true);
    final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    assert document != null;
    document.putUserData(COMBO_BOX_KEY, comboBox);
    if (usePlainMatcher) {
      document.putUserData(USE_PLAIN_PREFIX_MATCHER, true);
    }

    super.setItem(document);
  }

  @Override
  protected void onEditorCreate(final EditorEx editor) {
    Disposer.register(((EditorImpl)editor).getDisposable(), new Disposable() {
      @Override
      public void dispose() {
        editor.getDocument().putUserData(COMBO_BOX_KEY, null);
      }
    });
  }

  @NotNull
  @Override
  public Object getItem() {
    return ((Document)super.getItem()).getText();
  }

  @Override
  public void setItem(Object anObject) {
    if (anObject == null) anObject = "";
    if (anObject.equals(getItem())) return;
    final String s = (String)anObject;
    WriteCommandAction.writeCommandAction(myProject).run(() -> getDocument().setText(s));

    final Editor editor = getEditor();
    if (editor != null) editor.getCaretModel().moveToOffset(s.length());
  }
}
