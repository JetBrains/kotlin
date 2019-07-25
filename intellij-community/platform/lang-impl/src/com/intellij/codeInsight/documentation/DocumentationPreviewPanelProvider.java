// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation;

import com.intellij.openapi.preview.PreviewPanelProvider;
import com.intellij.openapi.preview.PreviewProviderId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class DocumentationPreviewPanelProvider extends PreviewPanelProvider<Couple<PsiElement>, DocumentationComponent> {
  public static final PreviewProviderId<Couple<PsiElement>, DocumentationComponent> ID = PreviewProviderId.create("Documentation");
  private final DocumentationComponent myDocumentationComponent;
  private final DocumentationManager myDocumentationManager;

  public DocumentationPreviewPanelProvider(@NotNull Project project) {
    super(ID);

    myDocumentationManager = DocumentationManager.getInstance(project);
    myDocumentationComponent = new DocumentationComponent(myDocumentationManager) {
      @Override
      public String toString() {
        return "Preview DocumentationComponent (" + (isEmpty() ? "empty" : "not empty") + ")";
      }
    };
  }

  @Override
  public void dispose() {
    Disposer.dispose(myDocumentationComponent);
  }

  @NotNull
  @Override
  protected JComponent getComponent() {
    return myDocumentationComponent;
  }

  @NotNull
  @Override
  protected String getTitle(@NotNull Couple<PsiElement> content) {
    return DocumentationManager.getTitle(content.getFirst(), false);
  }

  @Nullable
  @Override
  protected Icon getIcon(@NotNull Couple<PsiElement> content) {
    return content.getFirst().getIcon(0);
  }

  @Override
  public float getMenuOrder() {
    return 1;
  }

  @Override
  public void showInStandardPlace(@NotNull Couple<PsiElement> content) {
    myDocumentationManager.showJavaDocInfo(content.getFirst(), content.getSecond());
  }

  @Override
  public void release(@NotNull Couple<PsiElement> content) {
  }

  @Override
  public boolean contentsAreEqual(@NotNull Couple<PsiElement> content1, @NotNull Couple<PsiElement> content2) {
    return content1.getFirst().getManager().areElementsEquivalent(content1.getFirst(), content2.getFirst());
  }

  @Override
  public boolean isModified(Couple<PsiElement> content, boolean beforeReuse) {
    return beforeReuse;
  }

  @Override
  protected DocumentationComponent initComponent(Couple<PsiElement> content, boolean requestFocus) {
    if (!content.getFirst().getManager().areElementsEquivalent(myDocumentationComponent.getElement(), content.getFirst())) {
      myDocumentationManager.fetchDocInfo(content.getFirst(), myDocumentationComponent);
    }
    return myDocumentationComponent;
  }
}
