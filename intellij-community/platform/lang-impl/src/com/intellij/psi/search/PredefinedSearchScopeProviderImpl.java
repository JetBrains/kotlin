// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyBrowserBase;
import com.intellij.ide.scratch.ScratchesSearchScope;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbUnawareHider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.EditorSelectionLocalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class PredefinedSearchScopeProviderImpl extends PredefinedSearchScopeProvider {

  @NotNull
  @Override
  public List<SearchScope> getPredefinedScopes(@NotNull Project project,
                                               @Nullable DataContext dataContext,
                                               boolean suggestSearchInLibs,
                                               boolean prevSearchFiles,
                                               boolean currentSelection,
                                               boolean usageView,
                                               boolean showEmptyScopes) {
    Collection<SearchScope> result = new LinkedHashSet<>();
    result.add(GlobalSearchScope.everythingScope(project));
    result.add(GlobalSearchScope.projectScope(project));
    if (suggestSearchInLibs) {
      result.add(GlobalSearchScope.allScope(project));
    }

    for (SearchScopeProvider each : SearchScopeProvider.EP_NAME.getExtensions()) {
      result.addAll(each.getGeneralSearchScopes(project));
    }

    if (ModuleUtil.hasTestSourceRoots(project)) {
      result.add(GlobalSearchScopesCore.projectProductionScope(project));
      result.add(GlobalSearchScopesCore.projectTestScope(project));
    }

    result.add(ScratchesSearchScope.getScratchesScope(project));

    GlobalSearchScope recentFilesScope = recentFilesScope(project, false);
    ContainerUtil.addIfNotNull(
      result, recentFilesScope != GlobalSearchScope.EMPTY_SCOPE ? recentFilesScope :
              showEmptyScopes ? new LocalSearchScope(PsiElement.EMPTY_ARRAY, IdeBundle.message("scope.recent.files")) : null);
    GlobalSearchScope recentModFilesScope = recentFilesScope(project, true);
    ContainerUtil.addIfNotNull(
      result, recentModFilesScope != GlobalSearchScope.EMPTY_SCOPE ? recentModFilesScope :
              showEmptyScopes ? new LocalSearchScope(PsiElement.EMPTY_ARRAY, IdeBundle.message("scope.recent.modified.files")) : null);
    GlobalSearchScope openFilesScope = GlobalSearchScopes.openFilesScope(project);
    ContainerUtil.addIfNotNull(
      result, openFilesScope != GlobalSearchScope.EMPTY_SCOPE ? openFilesScope :
              showEmptyScopes ? new LocalSearchScope(PsiElement.EMPTY_ARRAY, IdeBundle.message("scope.open.files")) : null);

    Editor selectedTextEditor = ApplicationManager.getApplication().isDispatchThread()
                                ? FileEditorManager.getInstance(project).getSelectedTextEditor()
                                : null;
    PsiFile psiFile = selectedTextEditor == null ? null : PsiDocumentManager.getInstance(project).getPsiFile(selectedTextEditor.getDocument());
    PsiFile currentFile = psiFile;

    if (dataContext != null) {
      PsiElement dataContextElement = CommonDataKeys.PSI_FILE.getData(dataContext);
      if (dataContextElement == null) {
        dataContextElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      }

      if (dataContextElement == null && psiFile != null) {
        dataContextElement = psiFile;
      }

      if (dataContextElement != null) {
        if (!PlatformUtils.isCidr() && !PlatformUtils.isRider()) { // TODO: have an API to disable module scopes.
          Module module = ModuleUtilCore.findModuleForPsiElement(dataContextElement);
          if (module == null) {
            module = LangDataKeys.MODULE.getData(dataContext);
          }
          if (module != null && !ModuleType.isInternal(module)) {
            result.add(module.getModuleScope());
          }
        }
        if (currentFile == null) {
          currentFile = dataContextElement.getContainingFile();
        }
      }
    }

    if (currentFile != null || showEmptyScopes) {
      PsiElement[] scope = currentFile != null ? new PsiElement[] {currentFile} : PsiElement.EMPTY_ARRAY;
      result.add(new LocalSearchScope(scope, IdeBundle.message("scope.current.file")));
    }

    if (currentSelection && selectedTextEditor != null && psiFile != null) {
      SelectionModel selectionModel = selectedTextEditor.getSelectionModel();
      if (selectionModel.hasSelection()) {
        result.add(new EditorSelectionLocalSearchScope(selectedTextEditor, project, IdeBundle.message("scope.selection")));
      }
    }

    if (usageView) {
      addHierarchyScope(project, result);
      UsageView selectedUsageView = UsageViewManager.getInstance(project).getSelectedUsageView();
      if (selectedUsageView != null && !selectedUsageView.isSearchInProgress()) {
        final Set<Usage> usages = new THashSet<>(selectedUsageView.getUsages());
        usages.removeAll(selectedUsageView.getExcludedUsages());

        if (prevSearchFiles) {
          final Set<VirtualFile> files = collectFiles(usages, true);
          if (!files.isEmpty()) {
            GlobalSearchScope prev = new GlobalSearchScope(project) {
              private Set<VirtualFile> myFiles;

              @NotNull
              @Override
              public String getDisplayName() {
                return IdeBundle.message("scope.files.in.previous.search.result");
              }

              @Override
              public synchronized boolean contains(@NotNull VirtualFile file) {
                if (myFiles == null) {
                  myFiles = collectFiles(usages, false);
                }
                return myFiles.contains(file);
              }

              @Override
              public boolean isSearchInModuleContent(@NotNull Module aModule) {
                return true;
              }

              @Override
              public boolean isSearchInLibraries() {
                return true;
              }
            };
            result.add(prev);
          }
        }
        else {
          final List<PsiElement> results = new ArrayList<>(usages.size());
          for (Usage usage : usages) {
            if (usage instanceof PsiElementUsage) {
              final PsiElement element = ((PsiElementUsage)usage).getElement();
              if (element != null && element.isValid() && element.getContainingFile() != null) {
                results.add(element);
              }
            }
          }

          if (!results.isEmpty()) {
            result.add(new LocalSearchScope(PsiUtilCore.toPsiElementArray(results), IdeBundle.message("scope.previous.search.results")));
          }
        }
      }
    }

    ContainerUtil.addIfNotNull(result, getSelectedFilesScope(project, dataContext, currentFile));

    return new ArrayList<>(result);
  }

  private static void addHierarchyScope(@NotNull Project project, Collection<? super SearchScope> result) {
    final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.HIERARCHY);
    if (toolWindow == null) {
      return;
    }
    final ContentManager contentManager = toolWindow.getContentManager();
    final Content content = contentManager.getSelectedContent();
    if (content == null) {
      return;
    }
    final String name = content.getDisplayName();
    JComponent component = content.getComponent();
    if (component instanceof DumbUnawareHider) {
      component = ((DumbUnawareHider)component).getContent();
    }
    final HierarchyBrowserBase hierarchyBrowserBase = (HierarchyBrowserBase)component;
    final PsiElement[] elements = hierarchyBrowserBase.getAvailableElements();
    if (elements.length > 0) {
      result.add(new LocalSearchScope(elements, "Hierarchy '" + name + "' (visible nodes only)"));
    }
  }

  @NotNull
  public static GlobalSearchScope recentFilesScope(@NotNull Project project, boolean changedOnly) {
    String name = changedOnly ? IdeBundle.message("scope.recent.modified.files") : IdeBundle.message("scope.recent.files");
    List<VirtualFile> files = changedOnly ? Arrays.asList(IdeDocumentHistory.getInstance(project).getChangedFiles()) :
                              JBIterable.from(EditorHistoryManager.getInstance(project).getFileList())
                                .append(FileEditorManager.getInstance(project).getOpenFiles()).unique().toList();

    return files.isEmpty() ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.filesScope(project, files, name);
  }

  @Nullable
  private static SearchScope getSelectedFilesScope(final Project project,
                                                   @Nullable DataContext dataContext,
                                                   @Nullable PsiFile currentFile) {
    final VirtualFile[] filesOrDirs = (dataContext == null) ? null : CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
    if (filesOrDirs == null || filesOrDirs.length == 0 ||
        filesOrDirs.length == 1 && currentFile != null && filesOrDirs[0].equals(currentFile.getVirtualFile())) {
      return null;
    }
    return new SelectedFilesScope(project, filesOrDirs);
  }

  @NotNull
  protected static Set<VirtualFile> collectFiles(Set<? extends Usage> usages, boolean findFirst) {
    final Set<VirtualFile> files = new HashSet<>();
    for (Usage usage : usages) {
      if (usage instanceof PsiElementUsage) {
        PsiElement psiElement = ((PsiElementUsage)usage).getElement();
        if (psiElement != null && psiElement.isValid()) {
          PsiFile psiFile = psiElement.getContainingFile();
          if (psiFile != null) {
            VirtualFile file = psiFile.getVirtualFile();
            if (file != null) {
              files.add(file);
              if (findFirst) return files;
            }
          }
        }
      }
    }
    return files;
  }

  static class SelectedFilesScope extends GlobalSearchScope {

    private final Set<VirtualFile> myFiles = new THashSet<>();
    private final Set<VirtualFile> myDirectories = new THashSet<>();

    SelectedFilesScope(Project project, VirtualFile... filesOrDirs) {
      super(project);
      if (filesOrDirs.length == 0) {
        throw new IllegalArgumentException("array is empty");
      }
      for (VirtualFile fileOrDir : filesOrDirs) {
        if (fileOrDir.isDirectory()) {
          myDirectories.add(fileOrDir);
        }
        else {
          myFiles.add(fileOrDir);
        }
      }
    }

    @Override
    public boolean isSearchInModuleContent(@NotNull Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInLibraries() {
      return true;
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
      for (VirtualFile virtualFile : myFiles) {
        if (virtualFile.equals(file)) {
          return true;
        }
      }
      return VfsUtilCore.isUnder(file, myDirectories);
    }

    @NotNull
    @Override
    public String getDisplayName() {
      if (myFiles.isEmpty()) {
        return IdeBundle.message("scope.selected.directories", myDirectories.size());
      }
      if (myDirectories.isEmpty()) {
        return IdeBundle.message("scope.selected.files", myFiles.size());
      }
      return IdeBundle.message("scope.selected.files.and.directories", myFiles.size(), myDirectories.size());
    }
  }
}
