/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.packageDependencies.ui;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.Gray;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PackageDependenciesNode extends DefaultMutableTreeNode implements Navigatable{
  private static final EmptyIcon EMPTY_ICON = EmptyIcon.create(0, IconUtil.getEmptyIcon(false).getIconHeight());

  private Set<VirtualFile> myRegisteredFiles = null;
  private boolean myHasUnmarked = false;
  private boolean myHasMarked = false;
  private boolean myEquals;
  protected Color myColor = null;
  protected static final Color NOT_CHANGED = Gray._0;
  protected Project myProject;
  private boolean mySorted;

  public PackageDependenciesNode(@NotNull Project project) {
    myProject = project;
  }

  public void setEquals(final boolean equals) {
    myEquals = equals;
  }

  public boolean isEquals() {
    return myEquals;
  }

  public void fillFiles(Set<? super PsiFile> set, boolean recursively) {
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    for (VirtualFile vFile : getRegisteredFiles()) {
      final PsiFile psiFile = psiManager.findFile(vFile);
      if (psiFile != null && psiFile.isValid()) {
        set.add(psiFile);
      }
    }
  }

  public void addFile(VirtualFile file, boolean isMarked) {
    getRegisteredFiles().add(file);
    updateMarked(!isMarked, isMarked);
  }

  public Icon getIcon() {
    return EMPTY_ICON;
  }

  public int getWeight() {
    return 0;
  }

  public boolean hasUnmarked() {
    return myHasUnmarked;
  }

  public boolean hasMarked() {
    return myHasMarked;
  }

  @Nullable
  public PsiElement getPsiElement() {
    return null;
  }

  @Nullable
  public Color getColor() {
    return myColor;
  }

  public void updateColor() {
    myColor = null;
  }

  public int getContainingFiles(){
    int result = 0;
    for (int i = 0; i < getChildCount(); i++) {
      result += ((PackageDependenciesNode)getChildAt(i)).getContainingFiles();
    }
    return result;
  }

  public String getPresentableFilesCount(){
    final int filesCount = getContainingFiles();
    return filesCount > 0 ? " (" + AnalysisScopeBundle.message("package.dependencies.node.items.count", filesCount) + ")" : "";
  }

  @Override
  public void add(MutableTreeNode newChild) {
    super.add(newChild);
    boolean hasUnmarked = ((PackageDependenciesNode)newChild).hasUnmarked();
    boolean hasMarked = ((PackageDependenciesNode)newChild).hasMarked();
    updateMarked(hasUnmarked, hasMarked);
  }

  private void updateMarked(boolean hasUnmarked, boolean hasMarked) {
    if (hasUnmarked && !myHasUnmarked || hasMarked && !myHasMarked) {
      myHasUnmarked |= hasUnmarked;
      myHasMarked |= hasMarked;
      PackageDependenciesNode parent = (PackageDependenciesNode)getParent();
      if (parent != null) {
        parent.updateMarked(myHasUnmarked, myHasMarked);
      }
    }
  }

  @Override
  public void navigate(boolean focus) {
    if (canNavigate()) {
      PsiElement psiElement = getPsiElement();
      if (psiElement != null) {
        NavigationUtil.openFileWithPsiElement(psiElement, focus, focus);
      }
    }
  }

  @Override
  public boolean canNavigate() {
    final PsiElement psiElement = getPsiElement();
    if (psiElement == null) return false;
    PsiFile containingFile = psiElement.getContainingFile();
    if (containingFile == null) return false;
    final VirtualFile virtualFile = containingFile.getVirtualFile();
    return virtualFile != null && virtualFile.isValid();
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }

  @Override
  public Object getUserObject() {
    return toString();
  }

  public boolean isValid() {
    return true;
  }

  public Set<VirtualFile> getRegisteredFiles() {
    if (myRegisteredFiles == null) {
      myRegisteredFiles = new HashSet<>();
    }
    return myRegisteredFiles;
  }

  @Nullable
  public String getComment() {
    return null;
  }

  public boolean canSelectInLeftTree(Map<PsiFile, Set<PsiFile>> deps){
    return false;
  }

  public boolean isSorted() {
    return mySorted;
  }

  public void setSorted(boolean sorted) {
    mySorted = sorted;
  }

  public void sortChildren() {
    if (isSorted()) return;
    TreeUtil.sortChildren(this, new DependencyNodeComparator());
    setSorted(true);
  }
}
