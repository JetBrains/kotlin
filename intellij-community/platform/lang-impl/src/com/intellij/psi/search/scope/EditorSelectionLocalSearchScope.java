// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search.scope;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EditorSelectionLocalSearchScope extends LocalSearchScope {
  private final Editor myEditor;
  private final Project myProject;
  private PsiElement[] myPsiElements;
  private final boolean myIgnoreInjectedPsi;
  @NotNull
  private final String myDisplayName;

  private LocalSearchScope myLocalSearchScope;

  private void init() {
    ReadAction.run(() -> {
      PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
      if (psiFile == null) {
        myPsiElements = PsiElement.EMPTY_ARRAY;
        return;
      }

      SelectionModel selectionModel = myEditor.getSelectionModel();
      if (!selectionModel.hasSelection()) {
        myPsiElements = PsiElement.EMPTY_ARRAY;
        return;
      }

      int[] selectionStarts = selectionModel.getBlockSelectionStarts();
      int[] selectionEnds = selectionModel.getBlockSelectionEnds();
      final List<PsiElement> elements = new ArrayList<>();

      for (int i = 0; i < selectionStarts.length; ++i) {
        int start = selectionStarts[i];
        final PsiElement startElement = psiFile.findElementAt(start);
        if (startElement == null) {
          continue;
        }
        int end = selectionEnds[i];
        final PsiElement endElement = psiFile.findElementAt(end);
        if (endElement == null) {
          continue;
        }
        final PsiElement parent = PsiTreeUtil.findCommonParent(startElement, endElement);
        if (parent == null) {
          continue;
        }

        final PsiElement[] children = parent.getChildren();
        TextRange selection = new TextRange(start, end);
        for (PsiElement child : children) {
          if (!(child instanceof PsiWhiteSpace) &&
              child.getContainingFile() != null &&
              selection.contains(child.getTextOffset())) {
            elements.add(child);
          }
        }
      }

      myPsiElements = elements.toArray(PsiElement.EMPTY_ARRAY);
    });
  }

  private PsiElement[] getPsiElements() {
    if (myPsiElements == null) init();
    return myPsiElements;
  }

  private TextRange[] getRanges() {
    SelectionModel selectionModel = myEditor.getSelectionModel();
    if (!selectionModel.hasSelection()) {
      return TextRange.EMPTY_ARRAY;
    }

    int[] selectionStarts = selectionModel.getBlockSelectionStarts();
    int[] selectionEnds = selectionModel.getBlockSelectionEnds();
    TextRange[] ranges = new TextRange[selectionStarts.length];
    for (int i = 0; i < selectionStarts.length; ++i) {
      ranges[i] = new TextRange(selectionStarts[i], selectionEnds[i]);
    }
    return ranges;
  }

  public EditorSelectionLocalSearchScope(@NotNull Editor editor, Project project,
                                         @NotNull final String displayName) {
    this(editor, project, displayName, false);
  }

  public EditorSelectionLocalSearchScope(@NotNull Editor editor, Project project,
                                         @NotNull final String displayName,
                                         final boolean ignoreInjectedPsi) {
    super(PsiElement.EMPTY_ARRAY);
    myEditor = editor;
    myProject = project;
    myDisplayName = displayName;
    myIgnoreInjectedPsi = ignoreInjectedPsi;
  }

  @Override
  public boolean isIgnoreInjectedPsi() {
    return myIgnoreInjectedPsi;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return myDisplayName;
  }

  // Do not instantiate LocalSearchScope for getVirtualFiles, calcHashCode, equals, toString and containsRange.
  @NotNull
  @Override
  public VirtualFile[] getVirtualFiles() {
    SelectionModel selectionModel = myEditor.getSelectionModel();
    if (!selectionModel.hasSelection()) {
      return VirtualFile.EMPTY_ARRAY;
    }
    VirtualFile[] files = new VirtualFile[selectionModel.getBlockSelectionStarts().length];
    Arrays.fill(files, FileDocumentManager.getInstance().getFile(myEditor.getDocument()));
    return files;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EditorSelectionLocalSearchScope)) return false;
    EditorSelectionLocalSearchScope other = (EditorSelectionLocalSearchScope)o;

    TextRange[] ranges = getRanges();
    TextRange[] otherRanges = other.getRanges();
    if (ranges.length != otherRanges.length) return false;

    VirtualFile[] files = getVirtualFiles();
    VirtualFile[] otherFiles = other.getVirtualFiles();
    for (int i = 0; i < ranges.length; ++i) {
      if (!Comparing.equal(files[i], otherFiles[i])) return false;
      if (!Comparing.equal(ranges[i], otherRanges[i])) return false;
    }

    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    TextRange[] ranges = getRanges();
    VirtualFile[] files = getVirtualFiles();
    for (int i = 0; i < ranges.length; ++i) {
      if (i > 0) builder.append(',');
      builder.append('{');
      builder.append(files[i].toString());
      builder.append(ranges[i].toString());
      builder.append('}');
    }

    return builder.toString();
  }

  @Override
  protected int calcHashCode() {
    int result = 0;
    TextRange[] ranges = getRanges();
    VirtualFile[] files = getVirtualFiles();
    for (int i = 0; i < ranges.length; ++i) {
      result += files[i].hashCode();
      result += ranges[i].hashCode();
    }
    return result;
  }

  @Override
  public boolean containsRange(@NotNull PsiFile file, @NotNull TextRange range) {
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) virtualFile = file.getNavigationElement().getContainingFile().getVirtualFile();
    TextRange[] ranges = getRanges();
    VirtualFile[] files = getVirtualFiles();
    for (int i = 0; i < ranges.length; ++i) {
      if (files[i].equals(virtualFile) && ranges[i].contains(range)) {
        return true;
      }
    }

    return false;
  }

  private void createIfNeeded() {
    if (myLocalSearchScope == null) {
      myLocalSearchScope = new LocalSearchScope(getPsiElements(), myDisplayName, myIgnoreInjectedPsi);
    }
  }

  @NotNull
  @Override
  public PsiElement[] getScope() {
    createIfNeeded();
    return myLocalSearchScope.getScope();
  }

  @NotNull
  @Override
  public LocalSearchScope intersectWith(@NotNull LocalSearchScope scope2) {
    createIfNeeded();
    return myLocalSearchScope.intersectWith(scope2);
  }

  @NotNull
  @Override
  public SearchScope intersectWith(@NotNull SearchScope scope2) {
    createIfNeeded();
    return myLocalSearchScope.intersectWith(scope2);
  }

  @NotNull
  @Override
  public SearchScope union(@NotNull SearchScope scope) {
    createIfNeeded();
    return myLocalSearchScope.union(scope);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    createIfNeeded();
    return myLocalSearchScope.contains(file);
  }

  @Override
  public boolean isInScope(@NotNull VirtualFile file) {
    createIfNeeded();
    return myLocalSearchScope.isInScope(file);
  }
}
