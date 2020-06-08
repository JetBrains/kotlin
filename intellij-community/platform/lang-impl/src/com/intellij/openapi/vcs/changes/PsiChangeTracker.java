// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.vcs.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public final class PsiChangeTracker {
  private PsiChangeTracker() {
  }

  public static <T extends PsiElement> Map<T, FileStatus> getElementsChanged(PsiElement file,
                                                                             PsiElement oldFile,
                                                                             final PsiFilter<T> filter) {
    final HashMap<T, FileStatus> result = new HashMap<>();
    final List<T> oldElements = new ArrayList<>();
    final List<T> elements = new ArrayList<>();

    if (file == null) {
      oldFile.accept(filter.createVisitor(oldElements));
      calculateStatuses(elements, oldElements, result, filter);
      return result;
    }

    final Project project = file.getProject();

    file.accept(filter.createVisitor(elements));
    final VirtualFile vf = file.getContainingFile().getVirtualFile();
    FileStatus status = vf == null ? null : FileStatusManager.getInstance(project).getStatus(vf);
    if (status == null && oldFile == null) {
      status = FileStatus.ADDED;
    }
    if (status == FileStatus.ADDED ||
        status == FileStatus.DELETED ||
        status == FileStatus.DELETED_FROM_FS ||
        status == FileStatus.UNKNOWN) {
      for (T element : elements) {
        result.put(element, status);
      }
      return result;
    }

    if (oldFile == null) return result;
    oldFile.accept(filter.createVisitor(oldElements));
    calculateStatuses(elements, oldElements, result, filter);

    return result;
  }

  private static <T extends PsiElement> Map<T, FileStatus> calculateStatuses(List<? extends T> elements,
                                                                             List<? extends T> oldElements,
                                                                             Map<T, FileStatus> result, PsiFilter<? super T> filter) {
    for (T element : elements) {
      T e = null;
      for (T oldElement : oldElements) {
        if (filter.areEquivalent(element, oldElement)) {
          e = oldElement;
          break;
        }
      }
      if (e != null) {
        oldElements.remove(e);
        if (!element.getText().equals(e.getText())) {
          result.put(element, FileStatus.MODIFIED);
        }
      }
      else {
        result.put(element, FileStatus.ADDED);
      }
    }

    for (T oldElement : oldElements) {
      result.put(oldElement, FileStatus.DELETED);
    }

    return result;
  }
}
