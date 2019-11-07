// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Arrays;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class ReformatCodeAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(ReformatCodeAction.class);

  protected static ReformatFilesOptions myTestOptions;

  public ReformatCodeAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    DataContext dataContext = event.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return;
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    final VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);

    PsiFile file = null;
    PsiDirectory dir;
    boolean hasSelection = false;

    if (editor != null){
      file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return;
      dir = file.getContainingDirectory();
      hasSelection = editor.getSelectionModel().hasSelection();
    }
    else if (containsOnlyFiles(files)) {
      final ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(Arrays.asList(files));
      if (!operationStatus.hasReadonlyFiles()) {
        ReformatFilesOptions selectedFlags = getReformatFilesOptions(project, files);
        if (selectedFlags == null)
          return;

        final boolean processOnlyChangedText = selectedFlags.getTextRangeType() == TextRangeType.VCS_CHANGED_TEXT;
        final boolean shouldOptimizeImports = selectedFlags.isOptimizeImports() && !DumbService.getInstance(project).isDumb();

        AbstractLayoutCodeProcessor processor = new ReformatCodeProcessor(project, convertToPsiFiles(files, project), null, processOnlyChangedText);
        if (shouldOptimizeImports) {
          processor = new OptimizeImportsProcessor(processor);
        }
        if (selectedFlags.isRearrangeCode()) {
          processor = new RearrangeCodeProcessor(processor);
        }
        if (selectedFlags.isCodeCleanup()) {
          processor = new CodeCleanupCodeProcessor(processor);
        }

        processor.run();
      }
      return;
    }
    else if (PlatformDataKeys.PROJECT_CONTEXT.getData(dataContext) != null || LangDataKeys.MODULE_CONTEXT.getData(dataContext) != null) {
      Module moduleContext = LangDataKeys.MODULE_CONTEXT.getData(dataContext);
      ReformatFilesOptions selectedFlags = getLayoutProjectOptions(project, moduleContext);
      if (selectedFlags != null) {
        reformatModule(project, moduleContext, selectedFlags);
      }
      return;
    }
    else {
      PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      if (element == null) return;
      if (element instanceof PsiDirectoryContainer) {
        dir = ArrayUtil.getFirstElement(((PsiDirectoryContainer)element).getDirectories());
      }
      else if (element instanceof PsiDirectory) {
        dir = (PsiDirectory)element;
      }
      else {
        file = element.getContainingFile();
        if (file == null) return;
        dir = file.getContainingDirectory();
      }
    }

    if (file == null && dir != null) {
      DirectoryFormattingOptions options = getDirectoryFormattingOptions(project, dir);
      if (options != null) {
        reformatDirectory(project, dir, options);
      }
      return;
    }

    if (file == null || editor == null) return;

    LastRunReformatCodeOptionsProvider provider = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());
    ReformatCodeRunOptions currentRunOptions = provider.getLastRunOptions(file);

    TextRangeType processingScope = currentRunOptions.getTextRangeType();
    if (hasSelection) {
      processingScope = TextRangeType.SELECTED_TEXT;
    }
    else if (processingScope == TextRangeType.VCS_CHANGED_TEXT) {
      if (FormatChangedTextUtil.getInstance().isChangeNotTrackedForFile(project, file)) {
        processingScope = TextRangeType.WHOLE_FILE;
      }
    }
    else {
      processingScope = TextRangeType.WHOLE_FILE;
    }

    currentRunOptions.setProcessingScope(processingScope);
    new FileInEditorProcessor(file, editor, currentRunOptions).processCode();
  }


  @Nullable
  private static DirectoryFormattingOptions getDirectoryFormattingOptions(@NotNull Project project, @NotNull PsiDirectory dir) {
    LayoutDirectoryDialog dialog = new LayoutDirectoryDialog(
      project,
      CodeInsightBundle.message("process.reformat.code"),
      CodeInsightBundle.message("process.scope.directory", dir.getVirtualFile().getPath()),
      FormatChangedTextUtil.hasChanges(dir)
    );

    boolean enableIncludeDirectoriesCb = dir.getSubdirectories().length > 0;
    dialog.setEnabledIncludeSubdirsCb(enableIncludeDirectoriesCb);
    dialog.setSelectedIncludeSubdirsCb(enableIncludeDirectoriesCb);

    if (dialog.showAndGet()) {
      return dialog;
    }
    return null;
  }

  public static void reformatDirectory(@NotNull Project project,
                                       @NotNull PsiDirectory dir,
                                       @NotNull DirectoryFormattingOptions options)
  {
    AbstractLayoutCodeProcessor processor = new ReformatCodeProcessor(
      project, dir, options.isIncludeSubdirectories(), options.getTextRangeType() == TextRangeType.VCS_CHANGED_TEXT
    );

    registerScopeFilter(processor, options.getSearchScope());
    registerFileMaskFilter(processor, options.getFileTypeMask());

    if (options.isOptimizeImports()) {
      processor = new OptimizeImportsProcessor(processor);
    }
    if (options.isRearrangeCode()) {
      processor = new RearrangeCodeProcessor(processor);
    }
    if (options.isCodeCleanup()) {
      processor = new CodeCleanupCodeProcessor(processor);
    }

    processor.run();
  }

  private static void reformatModule(@NotNull Project project,
                                     @Nullable Module moduleContext,
                                     @NotNull ReformatFilesOptions selectedFlags)
  {
    boolean shouldOptimizeImports = selectedFlags.isOptimizeImports() && !DumbService.getInstance(project).isDumb();
    boolean processOnlyChangedText = selectedFlags.getTextRangeType() == TextRangeType.VCS_CHANGED_TEXT;

    AbstractLayoutCodeProcessor processor;
    if (moduleContext != null)
      processor = new ReformatCodeProcessor(project, moduleContext, processOnlyChangedText);
    else
      processor = new ReformatCodeProcessor(project, processOnlyChangedText);

    registerScopeFilter(processor, selectedFlags.getSearchScope());
    registerFileMaskFilter(processor, selectedFlags.getFileTypeMask());

    if (shouldOptimizeImports) {
      processor = new OptimizeImportsProcessor(processor);
    }

    if (selectedFlags.isRearrangeCode()) {
      processor = new RearrangeCodeProcessor(processor);
    }

    processor.run();
  }

  public static void registerScopeFilter(@NotNull AbstractLayoutCodeProcessor processor, @Nullable final SearchScope scope) {
    if (scope == null) {
      return;
    }

    processor.addFileFilter(scope::contains);
  }

  public static void registerFileMaskFilter(@NotNull AbstractLayoutCodeProcessor processor, @Nullable String fileTypeMask) {
    if (fileTypeMask == null)
      return;

    final Condition<CharSequence> patternCondition = getFileTypeMaskPattern(fileTypeMask);
    processor.addFileFilter(file -> patternCondition.value(file.getNameSequence()));
  }

  private static Condition<CharSequence> getFileTypeMaskPattern(@Nullable String mask) {
    try {
      return FindInProjectUtil.createFileMaskCondition(mask);
    } catch (PatternSyntaxException e) {
      LOG.info("Error while processing file mask: ", e);
      return Conditions.alwaysTrue();
    }
  }

  public static PsiFile[] convertToPsiFiles(final VirtualFile[] files,Project project) {
    PsiManager psiManager = PsiManager.getInstance(project);
    List<PsiFile> list = PsiUtilCore.toPsiFiles(psiManager, Arrays.asList(files));
    return PsiUtilCore.toPsiFileArray(list);
  }



  @Override
  public void update(@NotNull AnActionEvent event){
    Presentation presentation = event.getPresentation();
    boolean available = isActionAvailable(event);
    if (event.isFromContextMenu()) {
      presentation.setEnabledAndVisible(available);
    }
    else {
      presentation.setEnabled(available);
    }
  }

  private static boolean isActionAvailable(AnActionEvent event) {
    DataContext dataContext = event.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null){
      return false;
    }

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

    final VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);

    if (editor != null){
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null || file.getVirtualFile() == null) {
        return false;
      }

      if (LanguageFormatting.INSTANCE.forContext(file) != null) {
        return true;
      }
    }
    else if (files!= null && containsOnlyFiles(files)) {
      boolean anyFormatters = false;
      for (VirtualFile virtualFile : files) {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
          return false;
        }
        final FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forContext(psiFile);
        if (builder != null) {
          anyFormatters = true;
          break;
        }
      }
      if (!anyFormatters) {
        return false;
      }
    }
    else if (files != null && files.length == 1) {
      // skip. Both directories and single files are supported.
    }
    else if (LangDataKeys.MODULE_CONTEXT.getData(dataContext) == null &&
             PlatformDataKeys.PROJECT_CONTEXT.getData(dataContext) == null) {
      PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      if (element == null) {
        return false;
      }
      if (!(element instanceof PsiDirectory)) {
        PsiFile file = element.getContainingFile();
        if (file == null || LanguageFormatting.INSTANCE.forContext(file) == null) {
          return false;
        }
      }
    }
    return true;
  }

  @Nullable
  private static ReformatFilesOptions getReformatFilesOptions(@NotNull Project project, @NotNull VirtualFile[] files) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return myTestOptions;
    }
    ReformatFilesDialog dialog = new ReformatFilesDialog(project, files);
    if (!dialog.showAndGet()) {
      return null;
    }
    return dialog;
  }

  @Nullable
  private static ReformatFilesOptions getLayoutProjectOptions(@NotNull Project project, @Nullable Module module) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return myTestOptions;
    }

    final String text = module != null ? CodeInsightBundle.message("process.scope.module", module.getModuleFilePath())
                                       : CodeInsightBundle.message("process.scope.project", project.getPresentableUrl());

    final boolean enableOnlyVCSChangedRegions = module != null ? FormatChangedTextUtil.hasChanges(module)
                                                               : FormatChangedTextUtil.hasChanges(project);

    LayoutProjectCodeDialog dialog =
      new LayoutProjectCodeDialog(project, CodeInsightBundle.message("process.reformat.code"), text, enableOnlyVCSChangedRegions);
    if (!dialog.showAndGet()) {
      return null;
    }
    return dialog;
  }

  @TestOnly
  public static void setTestOptions(ReformatFilesOptions options) {
    myTestOptions = options;
  }

  public static boolean containsOnlyFiles(final VirtualFile[] files) {
    if (files == null) return false;
    if (files.length < 1) return false;
    for (VirtualFile virtualFile : files) {
      if (virtualFile.isDirectory()) return false;
    }
    return true;
  }
}


