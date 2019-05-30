// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.internal.psiView;

import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.lang.ASTNode;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ViewerTreeStructure extends AbstractTreeStructure {
  private boolean myShowWhiteSpaces = true;
  private boolean myShowTreeNodes = true;

  private final Project myProject;
  private PsiElement myRootPsiElement = null;
  private final Object myRootElement = new Object();

  public ViewerTreeStructure(Project project) {
    myProject = project;
  }

  public void setRootPsiElement(PsiElement rootPsiElement) {
    myRootPsiElement = rootPsiElement;
  }

  public PsiElement getRootPsiElement() {
    return myRootPsiElement;
  }

  @NotNull
  @Override
  public Object getRootElement() {
    return myRootElement;
  }

  @NotNull
  @Override
  public Object[] getChildElements(@NotNull final Object element) {
    if (myRootElement == element) {
      if (myRootPsiElement == null) {
        return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
      }
      if (!(myRootPsiElement instanceof PsiFile)) {
        return new Object[]{myRootPsiElement};
      }
      List<PsiFile> files = ((PsiFile)myRootPsiElement).getViewProvider().getAllFiles();
      return PsiUtilCore.toPsiFileArray(files);
    }
    final Object[][] children = new Object[1][];
    children[0] = ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    ApplicationManager.getApplication().runReadAction(() -> {
      final Object[] result;
      if (myShowTreeNodes) {
        final ArrayList<Object> list = new ArrayList<>();
        ASTNode root = element instanceof PsiElement? SourceTreeToPsiMap.psiElementToTree((PsiElement)element) :
                             element instanceof ASTNode? (ASTNode)element : null;
        if (element instanceof Inject) {
          root = SourceTreeToPsiMap.psiElementToTree(((Inject)element).getPsi());
        }

        if (root != null) {
          ASTNode child = root.getFirstChildNode();
          while (child != null) {
            if (myShowWhiteSpaces || child.getElementType() != TokenType.WHITE_SPACE) {
              final PsiElement childElement = child.getPsi();
              list.add(childElement == null ? child : childElement);
            }
            child = child.getTreeNext();
          }
          final PsiElement psi = root.getPsi();
          if (psi instanceof PsiLanguageInjectionHost) {
            InjectedLanguageManager.getInstance(myProject).enumerate(psi, (injectedPsi, places) -> list.add(new Inject(psi, injectedPsi)));
          }
        }
        result = ArrayUtil.toObjectArray(list);
      }
      else {
        final PsiElement[] elementChildren = ((PsiElement)element).getChildren();
        if (!myShowWhiteSpaces) {
          final List<PsiElement> childrenList = new ArrayList<>(elementChildren.length);
          for (PsiElement psiElement : elementChildren) {
            if (!myShowWhiteSpaces && psiElement instanceof PsiWhiteSpace) {
              continue;
            }
            childrenList.add(psiElement);
          }
          result = PsiUtilCore.toPsiElementArray(childrenList);
        }
        else {
          result = elementChildren;
        }
      }
      children[0] = result;
    });
    return children[0];
  }

  @Override
  public Object getParentElement(@NotNull Object element) {
    if (element == myRootElement) {
      return null;
    }
    if (element == myRootPsiElement) {
      return myRootElement;
    }
    if (element instanceof PsiFile &&
        InjectedLanguageManager.getInstance(((PsiFile)element).getProject()).getInjectionHost(((PsiFile)element)) != null) {
      return new Inject(InjectedLanguageManager.getInstance(((PsiFile)element).getProject()).getInjectionHost(((PsiFile)element)),
                        (PsiElement)element);
    }
    return element instanceof Inject ? ((Inject)element).getParent() : ((PsiElement)element).getContext();
  }

  @Override
  public void commit() {
  }

  @Override
  public boolean hasSomethingToCommit() {
    return false;
  }

  @Override
  @NotNull
  public NodeDescriptor createDescriptor(@NotNull Object element, NodeDescriptor parentDescriptor) {
    if (element == myRootElement) {
      return new NodeDescriptor(myProject, null) {
        @Override
        public boolean update() {
          return false;
        }
        @Override
        public Object getElement() {
          return myRootElement;
        }
      };
    }
    return new ViewerNodeDescriptor(myProject, element, parentDescriptor);
  }

  public boolean isShowWhiteSpaces() {
    return myShowWhiteSpaces;
  }

  public void setShowWhiteSpaces(boolean showWhiteSpaces) {
    myShowWhiteSpaces = showWhiteSpaces;
  }

  public boolean isShowTreeNodes() {
    return myShowTreeNodes;
  }

  public void setShowTreeNodes(final boolean showTreeNodes) {
    myShowTreeNodes = showTreeNodes;
  }

  static class Inject {
    private final PsiElement myParent;
    private final PsiElement myPsi;

    Inject(PsiElement parent, PsiElement psi) {
      myParent = parent;
      myPsi = psi;
    }

    public PsiElement getParent() {
      return myParent;
    }

    public PsiElement getPsi() {
      return myPsi;
    }

    @Override
    public String toString() {
      return "INJECTION " + myPsi.getLanguage();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Inject inject = (Inject)o;

      if (!myParent.equals(inject.myParent)) return false;
      if (!myPsi.equals(inject.myPsi)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myParent.hashCode();
      result = 31 * result + myPsi.hashCode();
      return result;
    }
  }
}
