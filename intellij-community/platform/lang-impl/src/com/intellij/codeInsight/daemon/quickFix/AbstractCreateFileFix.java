// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import java.util.List;

import static com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR;

public abstract class AbstractCreateFileFix extends LocalQuickFixAndIntentionActionOnPsiElement {

  private static final int REFRESH_INTERVAL = 1000;

  protected final String myNewFileName;
  protected final List<TargetDirectory> myDirectories;
  protected final String[] mySubPath;
  @NotNull
  protected final String myKey;

  protected boolean myIsAvailable;
  protected long myIsAvailableTimeStamp;

  protected AbstractCreateFileFix(@Nullable PsiElement element,
                                  String newFileName,
                                  List<TargetDirectory> directories,
                                  String[] subPath,
                                  @NotNull String fixLocaleKey) {
    super(element);

    myNewFileName = newFileName;
    myDirectories = directories;
    mySubPath = subPath;
    myKey = fixLocaleKey;
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    long current = System.currentTimeMillis();

    if (ApplicationManager.getApplication().isUnitTestMode() || current - myIsAvailableTimeStamp > REFRESH_INTERVAL) {
      if (myDirectories.size() == 1) {
        PsiDirectory myDirectory = myDirectories.get(0).getDirectory();
        myIsAvailable &= myDirectory != null && myDirectory.getVirtualFile().findChild(myNewFileName) == null;
        myIsAvailableTimeStamp = current;
      }
      else {
        // do not check availability for multiple roots
      }
    }

    return myIsAvailable;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    if (isAvailable(project, null, file)) {
      if (myDirectories.size() == 1) {
        apply(myStartElement.getProject(), myDirectories.get(0));
      }
      else {
        List<TargetDirectory> directories = ContainerUtil.filter(myDirectories, d -> d.getDirectory() != null);
        if (directories.isEmpty()) {
          // there are no valid PsiDirectory items
          return;
        }

        List<TargetDirectoryListItem> sortedDirectories = getTargetDirectoryListItems(directories);
        sortWithResourcePriority(file, sortedDirectories);

        if (editor == null) {
          // run on first item of sorted list in batch mode
          apply(myStartElement.getProject(), directories.get(0));
        }
        else {
          showOptionsPopup(project, editor, sortedDirectories);
        }
      }
    }
  }

  protected abstract void apply(@NotNull Project project, TargetDirectory directory) throws IncorrectOperationException;

  // todo move sorting and source sets logic to extension point like FileReferenceHelper ?
  protected void sortWithResourcePriority(@NotNull PsiFile file, List<TargetDirectoryListItem> sortedDirectories) {
    // sort only if we have resource roots
    if (sortedDirectories.stream().anyMatch(AbstractCreateFileFix::isResourceRoot)) {
      ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(file.getProject());
      if (projectFileIndex.isInTestSourceContent(file.getVirtualFile())) {
        sortedDirectories.sort(AbstractCreateFileFix::compareTargetsForTests);
      }
      else if (projectFileIndex.isInSourceContent(file.getVirtualFile())) {
        sortedDirectories.sort(AbstractCreateFileFix::compareTargetsForProduction);
      }
    }
  }

  protected static PsiDirectory findOrCreateSubdirectory(PsiDirectory directory, String subDirectoryName) {
    PsiDirectory existingDirectory = directory.findSubdirectory(subDirectoryName);
    if (existingDirectory == null) {
      return directory.createSubdirectory(subDirectoryName);
    }
    return existingDirectory;
  }

  protected void showOptionsPopup(@NotNull Project project,
                                  @NotNull Editor editor,
                                  List<TargetDirectoryListItem> items) {
    String filePath = myNewFileName;
    if (mySubPath.length > 0) {
      filePath = StringUtil.join(mySubPath, VFS_SEPARATOR_CHAR + "") + VFS_SEPARATOR_CHAR + myNewFileName;
    }

    BaseListPopupStep<TargetDirectoryListItem> step =
      new BaseListPopupStep<TargetDirectoryListItem>(CodeInsightBundle.message(myKey, filePath), items) {
        @Override
        public Icon getIconFor(TargetDirectoryListItem value) {
          JpsModuleSourceRootType type = value.getSourceRootType();

          if (isSourceItem(type)) return AllIcons.Modules.SourceRoot;
          if (isTestSourceItem(type)) return AllIcons.Nodes.TestSourceFolder;
          if (isResourceItem(type)) return AllIcons.Modules.ResourcesRoot;
          if (isTestResourceItem(type)) return AllIcons.Modules.TestResourcesRoot;

          return PlatformIcons.FOLDER_ICON;
        }

        @NotNull
        @Override
        public String getTextFor(TargetDirectoryListItem value) {
          return value.getPresentablePath();
        }

        @Nullable
        @Override
        public PopupStep onChosen(TargetDirectoryListItem selectedValue, boolean finalChoice) {
          WriteCommandAction.writeCommandAction(project)
            .withName(CodeInsightBundle.message("create.file.text", myNewFileName))
            .run(() -> apply(project, selectedValue.getTarget()));

          return super.onChosen(selectedValue, finalChoice);
        }
      };

    ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
    popup.addListener(new JBPopupListener() {
      @Override
      public void onClosed(@NotNull LightweightWindowEvent event) {
        // rerun code-insight after popup close
        DaemonCodeAnalyzer.getInstance(project).restart();
      }
    });

    popup.showInBestPositionFor(editor);
  }

  @NotNull
  private static List<TargetDirectoryListItem> getTargetDirectoryListItems(List<TargetDirectory> directories) {
    return ContainerUtil.map(directories, targetDirectory -> {
      PsiDirectory d = targetDirectory.getDirectory();
      assert d != null : "Invalid PsiDirectory instances found";

      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(d.getProject()).getFileIndex();

      Module targetModule = projectFileIndex.getModuleForFile(d.getVirtualFile());
      JpsModuleSourceRootType sourceRootType = null;
      if (targetModule != null) {
        SourceFolder folder = getSourceFolder(targetModule, d);
        if (folder != null) {
          sourceRootType = folder.getRootType();
        }
      }

      String presentablePath = getPresentableContentRootPath(d.getProject(), d.getVirtualFile(), targetDirectory.getPathToCreate());

      return new TargetDirectoryListItem(targetDirectory, sourceRootType, presentablePath);
    });
  }

  private static int getTestsTargetOrdinal(TargetDirectoryListItem item) {
    JpsModuleSourceRootType type = item.getSourceRootType();

    if (isSourceItem(type)) return 4;
    if (isTestSourceItem(type)) return 3;
    if (isResourceItem(type)) return 2;
    if (isTestResourceItem(type)) return 1;

    return 0;
  }

  private static int getSourcesTargetOrdinal(TargetDirectoryListItem item) {
    JpsModuleSourceRootType type = item.getSourceRootType();

    if (isTestSourceItem(type)) return 4;
    if (isSourceItem(type)) return 3;
    if (isTestResourceItem(type)) return 2;
    if (isResourceItem(type)) return 1;

    return 0;
  }

  private static boolean isTestResourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaResourceRootType.TEST_RESOURCE;
  }

  private static boolean isResourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaResourceRootType.RESOURCE;
  }

  private static boolean isTestSourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaSourceRootType.TEST_SOURCE;
  }

  private static boolean isSourceItem(@Nullable JpsModuleSourceRootType type) {
    return type == JavaSourceRootType.SOURCE;
  }

  @NotNull
  private static String getPresentableContentRootPath(@NotNull Project project,
                                                      @NotNull VirtualFile f,
                                                      @NotNull String[] pathToCreate) {
    String toProjectPath = ProjectUtil.calcRelativeToProjectPath(f, project, true, false, true);
    if (pathToCreate.length > 0) {
      toProjectPath += VFS_SEPARATOR_CHAR + StringUtil.join(pathToCreate, VFS_SEPARATOR_CHAR + "");
    }

    return toProjectPath;
  }

  protected static int compareTargetsForTests(@NotNull TargetDirectoryListItem d1, @NotNull TargetDirectoryListItem d2) {
    int o1 = getTestsTargetOrdinal(d1);
    int o2 = getTestsTargetOrdinal(d2);

    if (o1 > 0 && o2 > 0) {
      return Integer.compare(o1, o2);
    }

    return compareDirectoryPaths(d1, d2);
  }

  protected static int compareTargetsForProduction(@NotNull TargetDirectoryListItem d1, @NotNull TargetDirectoryListItem d2) {
    int o1 = getSourcesTargetOrdinal(d1);
    int o2 = getSourcesTargetOrdinal(d2);

    if (o1 > 0 && o2 > 0) {
      return Integer.compare(o1, o2);
    }

    return compareDirectoryPaths(d1, d2);
  }

  private static int compareDirectoryPaths(@NotNull TargetDirectoryListItem d1, @NotNull TargetDirectoryListItem d2) {
    PsiDirectory directory1 = d1.getTarget().getDirectory();
    PsiDirectory directory2 = d2.getTarget().getDirectory();

    assert directory1 != null : "Invalid PsiDirectory instances found";
    assert directory2 != null : "Invalid PsiDirectory instances found";

    VirtualFile f1 = directory1.getVirtualFile();
    VirtualFile f2 = directory2.getVirtualFile();
    return f1.getPath().compareTo(f2.getPath());
  }

  protected static boolean isResourceRoot(TargetDirectoryListItem d) {
    return isResourceItem(d.getSourceRootType()) ||
           isTestResourceItem(d.getSourceRootType());
  }

  protected static class TargetDirectoryListItem {
    private final TargetDirectory myTargetDirectory;
    private final JpsModuleSourceRootType mySourceRootType;
    private final String myPresentablePath;

    public TargetDirectoryListItem(@NotNull TargetDirectory targetDirectory,
                                   @Nullable JpsModuleSourceRootType type,
                                   @NotNull String presentablePath) {
      myTargetDirectory = targetDirectory;
      mySourceRootType = type;
      myPresentablePath = presentablePath;
    }

    @Nullable
    private JpsModuleSourceRootType getSourceRootType() {
      return mySourceRootType;
    }

    private String getPresentablePath() {
      return myPresentablePath;
    }

    private TargetDirectory getTarget() {
      return myTargetDirectory;
    }
  }

  @Nullable
  private static SourceFolder getSourceFolder(@NotNull Module module, @NotNull PsiDirectory directory) {
    ContentEntry[] entries = ModuleRootManager.getInstance(module).getContentEntries();
    for (ContentEntry contentEntry : entries) {
      for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
        if (sourceFolder.getFile() != null
            && VfsUtilCore.isAncestor(sourceFolder.getFile(), directory.getVirtualFile(), false)) {
          return sourceFolder;
        }
      }
    }

    return null;
  }
}
