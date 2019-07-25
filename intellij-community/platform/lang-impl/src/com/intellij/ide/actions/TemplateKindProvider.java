// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiDirectory;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public abstract class TemplateKindProvider {

  private final static ExtensionPointName<TemplateKindProvider> EP_NAME =
    ExtensionPointName.create("com.intellij.javaee.templateKindProvider");

  public static void addAdditionalKinds(AnAction action, PsiDirectory dir, CreateFileFromTemplateDialog.Builder builder) {
    String id = ActionManager.getInstance().getId(action);
    for (TemplateKindProvider provider : EP_NAME.getExtensionList()) {
      for (Kind kind : provider.getAdditionalKinds(dir)) {
        builder.addKind(kind.name, kind.icon, kind.templateName);
      }
    }
  }
  public abstract boolean isAvailable(Class<? extends AnAction> actionClass);
  public abstract Kind[] getAdditionalKinds(PsiDirectory dir);

  public static class Kind {
    public final String name;
    public final String templateName;
    public final Icon icon;

    public Kind(String name, String templateName, Icon icon) {
      this.name = name;
      this.templateName = templateName;
      this.icon = icon;
    }
  }
}
