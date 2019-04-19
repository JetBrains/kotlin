/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.util;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.StructureViewComposite;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.NodeProvider;
import com.intellij.ide.util.treeView.smartTree.ProvidingTreeModel;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class StructureViewCompositeModel extends StructureViewModelBase
  implements Disposable,
             StructureViewModel.ElementInfoProvider, 
             StructureViewModel.ExpandInfoProvider {
  private final List<? extends StructureViewComposite.StructureViewDescriptor> myViews;

  public StructureViewCompositeModel(@NotNull PsiFile file,
                                     @Nullable Editor editor,
                                     @NotNull List<? extends StructureViewComposite.StructureViewDescriptor> views) {
    super(file, editor, createRootNode(file, views));
    myViews = views;
  }
  
  @NotNull
  private JBIterable<StructureViewModel> getModels() {
    return JBIterable.from(myViews).map(o -> o.structureModel);
  }

  @Override
  public Object getCurrentEditorElement() {
    return getModels().filterMap(o -> o.getCurrentEditorElement()).first();
  }

  @NotNull
  private static StructureViewTreeElement createRootNode(@NotNull PsiFile file,
                                                         @NotNull List<? extends StructureViewComposite.StructureViewDescriptor> views) {
    JBIterable<TreeElement> children = JBIterable.from(views).map(o -> createTreeElementFromView(file, o));
    return new StructureViewTreeElement() {
      @Override
      public Object getValue() {
        return file;
      }

      @Override
      public void navigate(boolean requestFocus) {
        file.navigate(requestFocus);
      }

      @Override
      public boolean canNavigate() {
        return file.canNavigate();
      }

      @Override
      public boolean canNavigateToSource() {
        return file.canNavigateToSource();
      }

      @NotNull
      @Override
      public ItemPresentation getPresentation() {
        return file.getPresentation();
      }

      @NotNull
      @Override
      public TreeElement[] getChildren() {
        List<TreeElement> elements = children.toList();
        return elements.toArray(TreeElement.EMPTY_ARRAY);
      }
    };
  }

  @NotNull
  @Override
  public Collection<NodeProvider> getNodeProviders() {
    return getModels().filter(ProvidingTreeModel.class).flatMap(ProvidingTreeModel::getNodeProviders).toSet();
  }

  @NotNull
  @Override
  public Filter[] getFilters() {
    Set<Filter> filters = getModels().flatMap(o -> JBIterable.of(o.getFilters())).toSet();
    return filters.toArray(Filter.EMPTY_ARRAY);
  }

  @Override
  public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
    for (ElementInfoProvider p : getModels().filter(ElementInfoProvider.class)) {
      if (p.isAlwaysShowsPlus(element)) return true;
    }
    return false;
  }

  @Override
  public boolean isAlwaysLeaf(StructureViewTreeElement element) {
    for (ElementInfoProvider p : getModels().filter(ElementInfoProvider.class)) {
      if (p.isAlwaysLeaf(element)) return true;
    }
    return false;
  }

  @Override
  public boolean isAutoExpand(@NotNull StructureViewTreeElement element) {
    if (element.getValue() instanceof StructureViewComposite.StructureViewDescriptor) return true;
    for (ExpandInfoProvider p : getModels().filter(ExpandInfoProvider.class)) {
      if (p.isAutoExpand(element)) return true;
    }
    return false;
  }

  @Override
  public boolean isSmartExpand() {
    boolean result = false;
    for (ExpandInfoProvider p : getModels().filter(ExpandInfoProvider.class)) {
      if (!p.isSmartExpand()) return false;
      result = true;
    }
    return result;
  }

  @NotNull
  private static TreeElement createTreeElementFromView(final PsiFile file, final StructureViewComposite.StructureViewDescriptor view) {
    return new StructureViewTreeElement() {
      @Override
      public Object getValue() {
        return view;
      }

      @Override
      public void navigate(boolean requestFocus) {
        file.navigate(requestFocus);
      }

      @Override
      public boolean canNavigate() {
        return file.canNavigate();
      }

      @Override
      public boolean canNavigateToSource() {
        return file.canNavigateToSource();
      }

      @NotNull
      @Override
      public ItemPresentation getPresentation() {
        return new ItemPresentation() {
          @Nullable
          @Override
          public String getPresentableText() {
            return view.title;
          }

          @Nullable
          @Override
          public String getLocationString() {
            return null;
          }

          @Nullable
          @Override
          public Icon getIcon(boolean unused) {
            return view.icon;
          }
        };
      }

      @NotNull
      @Override
      public TreeElement[] getChildren() {
        return view.structureModel.getRoot().getChildren();
      }
    };
  }
}
