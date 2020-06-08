// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author Philipp Smorygo
 */
public interface SearchEverywhereClassifier {
  final class EP_Manager {
    private EP_Manager() {}

    public static boolean isClass(@Nullable Object o) {
      for (SearchEverywhereClassifier classifier : SearchEverywhereClassifier.EP_NAME.getExtensionList()) {
        if (classifier.isClass(o)) return true;
      }
      return false;
    }

    public static boolean isSymbol(@Nullable Object o) {
      for (SearchEverywhereClassifier classifier : SearchEverywhereClassifier.EP_NAME.getExtensionList()) {
        if (classifier.isSymbol(o)) return true;
      }
      return false;
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull Object o) {
      return SearchEverywhereClassifier.EP_NAME.getExtensionList().stream()
        .map(classifier -> classifier.getVirtualFile(o))
        .filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Nullable
    public static Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      return SearchEverywhereClassifier.EP_NAME.getExtensionList().stream()
        .map(classifier -> classifier.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)).filter(Objects::nonNull)
        .findFirst().orElse(null);
    }

    @Nullable
    public static GlobalSearchScope getProjectScope(@NotNull Project project) {
      return SearchEverywhereClassifier.EP_NAME.getExtensionList().stream()
        .map(classifier -> classifier.getProjectScope(project))
        .filter(Objects::nonNull).findFirst().orElse(null);
    }
  }

  ExtensionPointName<SearchEverywhereClassifier> EP_NAME = ExtensionPointName.create("com.intellij.searchEverywhereClassifier");

  boolean isClass(@Nullable Object o);

  boolean isSymbol(@Nullable Object o);

  @Nullable
  VirtualFile getVirtualFile(@NotNull Object o);

  @Nullable
  Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus);

  @Nullable
  default GlobalSearchScope getProjectScope(@NotNull Project project) { return null; }
}
