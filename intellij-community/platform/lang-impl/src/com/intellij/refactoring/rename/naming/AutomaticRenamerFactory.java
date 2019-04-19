// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename.naming;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * When an element is renamed, allows to prompt the user to rename other elements with names derived from the name of the
 * element being renamed (for example, when a class is renamed, allows to prompt the user to rename variables of this class
 * that have names similar to the name of the class).
 *
 * @author yole
 */
public interface AutomaticRenamerFactory {
  ExtensionPointName<AutomaticRenamerFactory> EP_NAME = ExtensionPointName.create("com.intellij.automaticRenamerFactory");

  /**
   * Checks if this factory can provide additional elements to be renamed for the given element being renamed.
   *
   * @param element the element being renamed.
   */
  boolean isApplicable(@NotNull PsiElement element);

  /**
   * Returns the title of the checkbox shown in the rename dialog which enables or disables this renamer factory,
   * or null if the renamer factory does not require showing a checkbox in the rename dialog.
   *
   * @return the checkbox title.
   */
  @Nullable @Nls String getOptionName();

  /**
   * Returns true if this renamer factory is enabled (and the checkbox representing its state should be checked.)
   * Normally, the implementation of this method needs to load the persisted state of the checkbox.
   */
  boolean isEnabled();

  /**
   * Persists the state of the checkbox which enables or disables the renamer factory.
   *
   * @param enabled true if the checkbox is checked, false otherwise.
   */
  void setEnabled(boolean enabled);

  /**
   * Creates an automatic renamer for the given rename operation.
   *
   * @param element the primary element being renamed.
   * @param newName the new name of the element
   * @param usages  the list of usages of the primary element.
   * @return the renamer instance.
   */
  @NotNull AutomaticRenamer createRenamer(PsiElement element, String newName, Collection<UsageInfo> usages);
}