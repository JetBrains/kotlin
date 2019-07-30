// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                                  @NotNull NewFileLocation newFileLocation,
                                  @NotNull String fixLocaleKey) {
    super(element);

    myNewFileName = newFileLocation.getNewFileName();
    myDirectories = newFileLocation.getDirectories();
    mySubPath = newFileLocation.getSubPath();
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

        if (editor == null || ApplicationManager.getApplication().isUnitTestMode()) {
          // run on first item of sorted list in batch mode
          apply(myStartElement.getProject(), directories.get(0));
        }
        else {
          showOptionsPopup(project, editor, directories);
        }
      }
    }
  }

  protected abstract void apply(@NotNull Project project, TargetDirectory directory) throws IncorrectOperationException;

  protected static PsiDirectory findOrCreateSubdirectory(PsiDirectory directory, String subDirectoryName) {
    PsiDirectory existingDirectory = directory.findSubdirectory(subDirectoryName);
    if (existingDirectory == null) {
      return directory.createSubdirectory(subDirectoryName);
    }
    return existingDirectory;
  }

  protected void showOptionsPopup(@NotNull Project project,
                                  @NotNull Editor editor,
                                  List<TargetDirectory> directories) {
    List<TargetDirectoryListItem> items = getTargetDirectoryListItems(directories);

    String filePath = myNewFileName;
    if (mySubPath.length > 0) {
      filePath = StringUtil.join(mySubPath, VFS_SEPARATOR_CHAR + "") + VFS_SEPARATOR_CHAR + myNewFileName;
    }

    BaseListPopupStep<TargetDirectoryListItem> step =
      new BaseListPopupStep<TargetDirectoryListItem>(CodeInsightBundle.message(myKey, filePath), items) {
        @Override
        public Icon getIconFor(TargetDirectoryListItem value) {
          return value.getIcon();
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
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file != null) {
          DaemonCodeAnalyzer.getInstance(project).restart(file);
        }
      }
    });

    popup.showInBestPositionFor(editor);
  }

  @NotNull
  private static List<TargetDirectoryListItem> getTargetDirectoryListItems(List<TargetDirectory> directories) {
    return ContainerUtil.map(directories, targetDirectory -> {
      PsiDirectory d = targetDirectory.getDirectory();
      assert d != null : "Invalid PsiDirectory instances found";

      String presentablePath = getPresentableContentRootPath(d, targetDirectory.getPathToCreate());
      Icon icon = getContentRootIcon(d);

      return new TargetDirectoryListItem(targetDirectory, icon, presentablePath);
    });
  }

  @NotNull
  private static Icon getContentRootIcon(@NotNull PsiDirectory directory) {
    VirtualFile file = directory.getVirtualFile();

    Project project = directory.getProject();
    ProjectFileIndexImpl projectFileIndex = (ProjectFileIndexImpl)ProjectRootManager.getInstance(project).getFileIndex();
    SourceFolder sourceFolder = projectFileIndex.getSourceFolder(file);
    if (sourceFolder != null && sourceFolder.getFile() != null) {
      return IconUtil.getIcon(sourceFolder.getFile(), 0, project);
    }

    return IconUtil.getIcon(file, 0, project);
  }

  @NotNull
  private static String getPresentableContentRootPath(@NotNull PsiDirectory directory,
                                                      @NotNull String[] pathToCreate) {
    VirtualFile f = directory.getVirtualFile();
    Project project = directory.getProject();

    String toProjectPath = ProjectUtil.calcRelativeToProjectPath(f, project, true, false, true);
    if (pathToCreate.length > 0) {
      toProjectPath += VFS_SEPARATOR_CHAR + StringUtil.join(pathToCreate, VFS_SEPARATOR_CHAR + "");
    }

    return toProjectPath;
  }

  protected static class TargetDirectoryListItem {
    private final TargetDirectory myTargetDirectory;
    private final Icon myIcon;
    private final String myPresentablePath;

    public TargetDirectoryListItem(@NotNull TargetDirectory targetDirectory,
                                   Icon icon, @NotNull String presentablePath) {
      myTargetDirectory = targetDirectory;
      myIcon = icon;
      myPresentablePath = presentablePath;
    }

    public Icon getIcon() {
      return myIcon;
    }

    private String getPresentablePath() {
      return myPresentablePath;
    }

    private TargetDirectory getTarget() {
      return myTargetDirectory;
    }
  }
}
