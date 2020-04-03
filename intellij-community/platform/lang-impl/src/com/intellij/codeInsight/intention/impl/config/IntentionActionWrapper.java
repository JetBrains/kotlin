// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionBean;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.openapi.actionSystem.ShortcutProvider;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.PossiblyDumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IntentionActionWrapper implements IntentionAction, ShortcutProvider, IntentionActionDelegate, PossiblyDumbAware {
  private final IntentionActionBean myExtension;
  private String myFullFamilyName;
  private String myFamilyName;

  IntentionActionWrapper(@NotNull IntentionActionBean extension) {
    myExtension = extension;
  }

  @NotNull
  String getDescriptionDirectoryName() {
    return getDescriptionDirectoryName(getImplementationClassName());
  }

  @NotNull
  static String getDescriptionDirectoryName(@NotNull String fqn) {
    return fqn.substring(fqn.lastIndexOf('.') + 1).replaceAll("\\$", "");
  }

  @Override
  @NotNull
  public String getText() {
    return getDelegate().getText();
  }

  @Override
  @NotNull
  public String getFamilyName() {
    String result = myFamilyName;
    if (result == null) {
      myFamilyName = result = getDelegate().getFamilyName();
    }
    return result;
  }

  @Override
  public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file) {
    return getDelegate().isAvailable(project, editor, file);
  }

  @Override
  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    getDelegate().invoke(project, editor, file);
  }

  @Override
  public boolean startInWriteAction() {
    return getDelegate().startInWriteAction();
  }

  @Override
  public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
    return getDelegate().getFileModifierForPreview(target);
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile file) {
    return getDelegate().getElementToMakeWritable(file);
  }

  @NotNull
  public String getFullFamilyName(){
    String result = myFullFamilyName;
    if (result == null) {
      String[] myCategories = myExtension.getCategories();
      myFullFamilyName = result = myCategories != null ? StringUtil.join(myCategories, "/") + "/" + getFamilyName() : getFamilyName();
    }
    return result;
  }

  @Override
  public boolean isDumbAware() {
    return DumbService.isDumbAware(getDelegate());
  }

  @NotNull
  @Override
  public IntentionAction getDelegate() {
    return myExtension.getInstance();
  }

  @Override
  @NotNull
  public String getImplementationClassName() {
    return myExtension.className;
  }

  @NotNull
  ClassLoader getImplementationClassLoader() {
    return myExtension.getLoaderForClass();
  }

  @Override
  public String toString() {
    String text;
    try {
      text = getText();
    }
    catch (PsiInvalidElementAccessException e) {
      text = e.getMessage();
    }
    return "Intention: (" + getDelegate().getClass() + "): '" + text + "'";
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj) || getDelegate().equals(obj);
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    IntentionAction delegate = getDelegate();
    return delegate instanceof ShortcutProvider ? ((ShortcutProvider)delegate).getShortcut() : null;
  }
}
