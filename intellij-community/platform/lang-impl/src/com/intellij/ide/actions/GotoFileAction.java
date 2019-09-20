/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.searcheverywhere.FileSearchEverywhereContributor;
import com.intellij.ide.util.gotoByName.ChooseByNameFilter;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.GotoFileConfiguration;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * "Go to | File" action implementation.
 *
 * @author Eugene Belyaev
 * @author Constantine.Plotnikov
 */
public class GotoFileAction extends GotoActionBase implements DumbAware {
  public static final String ID = "GotoFile";

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (Registry.is("new.search.everywhere")) {
      showInSearchEverywherePopup(FileSearchEverywhereContributor.class.getSimpleName(), e, true, true);
    } else {
      super.actionPerformed(e);
    }
  }

  @Override
  public void gotoActionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");

    final GotoFileModel gotoFileModel = new GotoFileModel(project);
    GotoActionCallback<FileType> callback = new GotoActionCallback<FileType>() {
      @Override
      protected ChooseByNameFilter<FileType> createFilter(@NotNull ChooseByNamePopup popup) {
        return new GotoFileFilter(popup, gotoFileModel, project);
      }

      @Override
      public void elementChosen(final ChooseByNamePopup popup, final Object element) {
        if (element == null) return;
        ApplicationManager.getApplication().assertIsDispatchThread();
        Navigatable n = (Navigatable)element;
        //this is for better cursor position
        if (element instanceof PsiFile) {
          VirtualFile file = ((PsiFile)element).getVirtualFile();
          if (file == null) return;
          OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file, popup.getLinePosition(), popup.getColumnPosition());
          n = descriptor.setUseCurrentWindow(popup.isOpenInCurrentWindowRequested());
        }

        if (n.canNavigate()) {
          n.navigate(true);
        }
      }
    };
    showNavigationPopup(e, gotoFileModel, callback, IdeBundle.message("go.to.file.toolwindow.title"), true, true);
  }

  protected static class GotoFileFilter extends ChooseByNameFilter<FileType> {
    GotoFileFilter(final ChooseByNamePopup popup, GotoFileModel model, final Project project) {
      super(popup, model, GotoFileConfiguration.getInstance(project), project);
    }

    @Override
    @NotNull
    protected List<FileType> getAllFilterValues() {
      List<FileType> elements = new ArrayList<>();
      ContainerUtil.addAll(elements, FileTypeManager.getInstance().getRegisteredFileTypes());
      Collections.sort(elements, FileTypeComparator.INSTANCE);
      return elements;
    }

    @Override
    protected String textForFilterValue(@NotNull FileType value) {
      return value.getName();
    }

    @Override
    protected Icon iconForFilterValue(@NotNull FileType value) {
      return value.getIcon();
    }
  }

  /**
   * A file type comparator. The comparison rules are applied in the following order.
   * <ol>
   * <li>Unknown file type is greatest.</li>
   * <li>Text files are less then binary ones.</li>
   * <li>File type with greater name is greater (case is ignored).</li>
   * </ol>
   */
  public static class FileTypeComparator implements Comparator<FileType> {
    /**
     * an instance of comparator
     */
    public static final Comparator<FileType> INSTANCE = new FileTypeComparator();

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final FileType o1, final FileType o2) {
      if (o1 == o2) {
        return 0;
      }
      if (o1 == FileTypes.UNKNOWN) {
        return 1;
      }
      if (o2 == FileTypes.UNKNOWN) {
        return -1;
      }
      if (o1.isBinary() && !o2.isBinary()) {
        return 1;
      }
      if (!o1.isBinary() && o2.isBinary()) {
        return -1;
      }
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }
}
