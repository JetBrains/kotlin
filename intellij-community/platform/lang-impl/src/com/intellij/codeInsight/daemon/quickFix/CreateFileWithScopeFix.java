// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
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
import java.util.function.Supplier;

public class CreateFileWithScopeFix extends AbstractCreateFileFix {
  private static final int REFRESH_INTERVAL = 1000;

  private final String myNewFileName;
  private final List<TargetDirectory> myDirectories;
  private final String myText;
  @NotNull
  private final String myKey;

  private boolean myIsAvailable;
  private long myIsAvailableTimeStamp;

  @Nullable
  private Supplier<String> myFileTextSupplier;

  // invoked from other module
  @SuppressWarnings("WeakerAccess")
  public CreateFileWithScopeFix(@NotNull String newFileName,
                                @NotNull PsiElement psiElement,
                                @NotNull List<TargetDirectory> directories,
                                @Nullable String fileText,
                                @NotNull String fixLocaleKey) {
    super(psiElement);

    myNewFileName = newFileName;
    myDirectories = directories;
    myText = fileText;
    myKey = fixLocaleKey;
    myIsAvailable = !FileTypeManager.getInstance().getFileTypeByFileName(newFileName).isBinary();
    myIsAvailableTimeStamp = System.currentTimeMillis();
  }

  public CreateFileWithScopeFix(@NotNull String newFileName,
                                @NotNull PsiElement psiElement,
                                @NotNull List<TargetDirectory> directories) {
    this(newFileName, psiElement, directories, null, "create.file.text");
  }

  public CreateFileWithScopeFix(@NotNull String newFileName,
                                @NotNull PsiElement psiElement,
                                @NotNull List<TargetDirectory> directories,
                                @NotNull Supplier<String> fileTextSupplier) {
    this(newFileName, psiElement, directories, null, "create.file.text");

    myFileTextSupplier = fileTextSupplier;
  }

  @Nullable
  @Override
  protected String getFileText() {
    if (myFileTextSupplier != null) {
      return myFileTextSupplier.get();
    }
    return myText;
  }

  @Override
  @NotNull
  public String getText() {
    return CodeInsightBundle.message(myKey, myNewFileName);
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return CodeInsightBundle.message("create.file.family");
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile file) {
    return null;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    if (isAvailable(project, null, file)) {
      if (myDirectories.size() == 1) {
        invoke(myStartElement.getProject(), myDirectories.get(0).getDirectory());
      }
      else {
        List<TargetDirectory> sortedDirectories = ContainerUtil.filter(myDirectories, d -> d.getDirectory() != null);
        if (sortedDirectories.isEmpty()) {
          // there are no valid PsiDirectory items
          return;
        }

        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(file.getProject());
        if (projectFileIndex.isInTestSourceContent(file.getVirtualFile())) {
          sortedDirectories.sort(CreateFileWithScopeFix::compareTargetsForTests);
        }
        else if (projectFileIndex.isInSourceContent(file.getVirtualFile())) {
          sortedDirectories.sort(CreateFileWithScopeFix::compareTargetsForProduction);
        }

        if (editor == null) {
          // run on first item of sorted list in batch mode
          invoke(myStartElement.getProject(), sortedDirectories.get(0).getDirectory());
        }
        else {
          showOptionsPopup(project, editor, sortedDirectories);
        }
      }
    }
  }

  private void showOptionsPopup(@NotNull Project project,
                                @NotNull Editor editor,
                                List<TargetDirectory> sortedDirectories) {
    List<TargetDirectoryListItem> items = getTargetDirectoryListItems(sortedDirectories);

    BaseListPopupStep<TargetDirectoryListItem> step =
      new BaseListPopupStep<TargetDirectoryListItem>(CodeInsightBundle.message("create.file.text", myNewFileName), items) {
        @Override
        public Icon getIconFor(TargetDirectoryListItem value) {
          JpsModuleSourceRootType type = value.getType();

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
            .run(() -> invoke(project, selectedValue.getDirectory()));

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
      PsiDirectory directory = targetDirectory.getDirectory();
      assert directory != null : "Invalid PsiDirectory instances found";

      VirtualFile f = directory.getVirtualFile();

      return new TargetDirectoryListItem(directory, targetDirectory.getSourceRootType(),
                                         getPresentableContentRootPath(f, directory.getProject()));
    });
  }

  private static int getTestsTargetOrdinal(TargetDirectory item) {
    JpsModuleSourceRootType type = item.getSourceRootType();

    if (isSourceItem(type)) return 4;
    if (isTestSourceItem(type)) return 3;
    if (isResourceItem(type)) return 2;
    if (isTestResourceItem(type)) return 1;

    return 0;
  }

  private static int getSourcesTargetOrdinal(TargetDirectory item) {
    JpsModuleSourceRootType type = item.getSourceRootType();

    if (isTestSourceItem(type)) return 4;
    if (isSourceItem(type)) return 3;
    if (isTestResourceItem(type)) return 2;
    if (isResourceItem(type)) return 1;

    return 0;
  }

  private static boolean isTestResourceItem(JpsModuleSourceRootType type) {
    return type == JavaResourceRootType.TEST_RESOURCE;
  }

  private static boolean isResourceItem(JpsModuleSourceRootType type) {
    return type == JavaResourceRootType.RESOURCE;
  }

  private static boolean isTestSourceItem(JpsModuleSourceRootType type) {
    return type == JavaSourceRootType.TEST_SOURCE;
  }

  private static boolean isSourceItem(JpsModuleSourceRootType type) {
    return type == JavaSourceRootType.SOURCE;
  }

  @NotNull
  private static String getPresentableContentRootPath(@NotNull VirtualFile f, @NotNull Project project) {
    return ProjectUtil.calcRelativeToProjectPath(f, project, true, false, true);
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

  private void invoke(@NotNull Project project, PsiDirectory myDirectory) throws IncorrectOperationException {
    myIsAvailableTimeStamp = 0; // to revalidate applicability

    try {
      createFile(project, myDirectory, myNewFileName);
    }
    catch (IncorrectOperationException e) {
      myIsAvailable = false;
    }
  }

  private static int compareTargetsForTests(@NotNull TargetDirectory d1, @NotNull TargetDirectory d2) {
    int o1 = getTestsTargetOrdinal(d1);
    int o2 = getTestsTargetOrdinal(d2);

    if (o1 > 0 && o2 > 0) {
      return Integer.compare(o1, o2);
    }

    return compareDirectoryPaths(d1, d2);
  }

  private static int compareTargetsForProduction(@NotNull TargetDirectory d1, @NotNull TargetDirectory d2) {
    int o1 = getSourcesTargetOrdinal(d1);
    int o2 = getSourcesTargetOrdinal(d2);

    if (o1 > 0 && o2 > 0) {
      return Integer.compare(o1, o2);
    }

    return compareDirectoryPaths(d1, d2);
  }

  private static int compareDirectoryPaths(@NotNull TargetDirectory d1, @NotNull TargetDirectory d2) {
    assert d1.getDirectory() != null : "Invalid PsiDirectory instances found";
    assert d2.getDirectory() != null : "Invalid PsiDirectory instances found";

    VirtualFile f1 = d1.getDirectory().getVirtualFile();
    VirtualFile f2 = d2.getDirectory().getVirtualFile();
    return f1.getPath().compareTo(f2.getPath());
  }

  private static class TargetDirectoryListItem {
    private final PsiDirectory myDirectory;
    @Nullable
    private final JpsModuleSourceRootType myType;
    private final String myPresentablePath;

    private TargetDirectoryListItem(PsiDirectory directory,
                                    @Nullable JpsModuleSourceRootType type, String presentablePath) {
      myDirectory = directory;
      myType = type;
      myPresentablePath = presentablePath;
    }

    @Nullable
    private JpsModuleSourceRootType getType() {
      return myType;
    }

    private String getPresentablePath() {
      return myPresentablePath;
    }

    private PsiDirectory getDirectory() {
      return myDirectory;
    }
  }

  public static class TargetDirectory {
    private final SmartPsiElementPointer<PsiDirectory> myDirectory;
    @Nullable
    private final SourceFolder mySourceFolder;

    public TargetDirectory(PsiDirectory directory) {
      this(directory, null);
    }

    public TargetDirectory(PsiDirectory directory, @Nullable SourceFolder sourceFolder) {
      myDirectory = SmartPointerManager.getInstance(directory.getProject()).createSmartPsiElementPointer(directory);
      mySourceFolder = sourceFolder;
    }

    @Nullable
    public PsiDirectory getDirectory() {
      return myDirectory.getElement();
    }

    @Nullable
    public JpsModuleSourceRootType getSourceRootType() {
      return mySourceFolder != null ? mySourceFolder.getRootType() : null;
    }

    @Nullable
    public SourceFolder getSourceFolder() {
      return mySourceFolder;
    }
  }
}
