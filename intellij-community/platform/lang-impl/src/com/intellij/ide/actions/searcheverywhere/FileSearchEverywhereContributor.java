// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.gotoByName.FileTypeRef;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.ide.util.gotoByName.GotoFileConfiguration;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.ui.IdeUICustomization;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 * @author Mikhail Sokolov
 */
public class FileSearchEverywhereContributor extends AbstractGotoSEContributor {
  private final GotoFileModel myModelForRenderer;
  private final PersistentSearchEverywhereContributorFilter<FileTypeRef> myFilter;

  public FileSearchEverywhereContributor(@NotNull AnActionEvent event) {
    super(event);
    Project project = event.getRequiredData(CommonDataKeys.PROJECT);
    myModelForRenderer = new GotoFileModel(project);
    myFilter = createFileTypeFilter(project);
  }

  @NotNull
  @Override
  public String getGroupName() {
    return IdeBundle.message("search.everywhere.group.name.files");
  }

  public String includeNonProjectItemsText() {
    return IdeUICustomization.getInstance().projectMessage("checkbox.include.non.project.files");
  }

  @Override
  public int getSortWeight() {
    return 200;
  }

  @Override
  public int getElementPriority(@NotNull Object element, @NotNull String searchPattern) {
    return super.getElementPriority(element, searchPattern) + 2;
  }

  @NotNull
  @Override
  protected FilteringGotoByModel<FileTypeRef> createModel(@NotNull Project project) {
    GotoFileModel model = new GotoFileModel(project);
    if (myFilter != null) {
      model.setFilterItems(myFilter.getSelectedElements());
    }
    return model;
  }

  @NotNull
  @Override
  public List<AnAction> getActions(@NotNull Runnable onChanged) {
    return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
  }

  @NotNull
  @Override
  public ListCellRenderer<Object> getElementsRenderer() {
    return new SERenderer() {
      @NotNull
      @Override
      protected ItemMatchers getItemMatchers(@NotNull JList list, @NotNull Object value) {
        ItemMatchers defaultMatchers = super.getItemMatchers(list, value);
        if (!(value instanceof PsiFileSystemItem) || myModelForRenderer == null) {
          return defaultMatchers;
        }

        return GotoFileModel.convertToFileItemMatchers(defaultMatchers, (PsiFileSystemItem)value, myModelForRenderer);
      }
    };
  }

  @Override
  public boolean processSelectedItem(@NotNull Object selected, int modifiers, @NotNull String searchText) {
    if (selected instanceof PsiFile) {
      VirtualFile file = ((PsiFile)selected).getVirtualFile();
      if (file != null && myProject != null) {
        Pair<Integer, Integer> pos = getLineAndColumn(searchText);
        OpenFileDescriptor descriptor = new OpenFileDescriptor(myProject, file, pos.first, pos.second);
        descriptor.setUseCurrentWindow(openInCurrentWindow(modifiers));
        if (descriptor.canNavigate()) {
          descriptor.navigate(true);
          return true;
        }
      }
    }

    return super.processSelectedItem(selected, modifiers, searchText);
  }

  @Override
  public Object getDataForItem(@NotNull Object element, @NotNull String dataId) {
    if (CommonDataKeys.PSI_FILE.is(dataId) && element instanceof PsiFile) {
      return element;
    }

    if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION.is(dataId)
        && (element instanceof PsiFile || element instanceof PsiDirectory)) {
      String path = ((PsiFileSystemItem)element).getVirtualFile().getPath();
      path = FileUtil.toSystemIndependentName(path);
      if (myProject != null) {
        String basePath = myProject.getBasePath();
        if (basePath != null) {
          path = FileUtil.getRelativePath(basePath, path, '/');
        }
      }
      return path;
    }

    return super.getDataForItem(element, dataId);
  }

  public static class Factory implements SearchEverywhereContributorFactory<Object> {
    @NotNull
    @Override
    public SearchEverywhereContributor<Object> createContributor(@NotNull AnActionEvent initEvent) {
      return new FileSearchEverywhereContributor(initEvent);
    }
  }

  @NotNull
  public static PersistentSearchEverywhereContributorFilter<FileTypeRef> createFileTypeFilter(@NotNull Project project) {
    List<FileTypeRef> items = FileTypeRef.forAllFileTypes();
    return new PersistentSearchEverywhereContributorFilter<>(items, GotoFileConfiguration.getInstance(project), FileTypeRef::getName,
                                                             FileTypeRef::getIcon);
  }
}
