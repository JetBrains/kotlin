// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteActionAware;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class CreateFromTemplateAction<T extends PsiElement> extends AnAction implements WriteActionAware {
  protected static final Logger LOG = Logger.getInstance(CreateFromTemplateAction.class);

  public CreateFromTemplateAction(@NlsActions.ActionText String text,
                                  @NlsActions.ActionDescription String description, Icon icon) {
    super(text, description, icon);
  }

  public CreateFromTemplateAction(@NotNull Supplier<String> dynamicText, @NotNull Supplier<String> dynamicDescription, Icon icon) {
    super(dynamicText, dynamicDescription, icon);
  }

  @Override
  public final void actionPerformed(@NotNull final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();

    final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (view == null) {
      return;
    }

    final Project project = CommonDataKeys.PROJECT.getData(dataContext);

    final PsiDirectory dir = view.getOrChooseDirectory();
    if (dir == null || project == null) return;

    final CreateFileFromTemplateDialog.Builder builder = createDialogBuilder(project, dataContext);
    buildDialog(project, dir, builder);

    final Ref<String> selectedTemplateName = Ref.create(null);
    builder.show(getErrorTitle(), getDefaultTemplateName(dir),
                 new CreateFileFromTemplateDialog.FileCreator<T>() {

                   @Override
                   public T createFile(@NotNull String name, @NotNull String templateName) {
                     selectedTemplateName.set(templateName);
                     return CreateFromTemplateAction.this.createFile(name, templateName, dir);
                   }

                   @Override
                   public boolean startInWriteAction() {
                     return CreateFromTemplateAction.this.startInWriteAction();
                   }

                   @Override
                   @NotNull
                   public String getActionName(@NotNull String name, @NotNull String templateName) {
                     return CreateFromTemplateAction.this.getActionName(dir, name, templateName);
                   }
                 },
                 createdElement -> {
                   if (createdElement != null) {
                     Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                     int offset = getOffsetToPreserve(editor);
                     view.selectElement(createdElement);
                     if (offset != -1 && editor != null && !editor.isDisposed()) {
                       editor.getCaretModel().moveToOffset(offset);
                     }
                     postProcess(createdElement, selectedTemplateName.get(), builder.getCustomProperties());
                   }
                 });
  }

  @SuppressWarnings("TestOnlyProblems")
  private static CreateFileFromTemplateDialog.Builder createDialogBuilder(Project project, DataContext dataContext) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      TestDialogBuilder.TestAnswers answers = dataContext.getData(TestDialogBuilder.TestAnswers.KEY);
      if (answers != null) {
        return new TestDialogBuilder(answers);
      }
    }
    return CreateFileFromTemplateDialog.createDialog(project);
  }

  protected void postProcess(@NotNull T createdElement, String templateName, Map<String, String> customProperties) {
  }

  @Nullable
  protected abstract T createFile(String name, String templateName, PsiDirectory dir);

  protected abstract void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory,
                                      @NotNull CreateFileFromTemplateDialog.Builder builder);

  @NonNls
  @Nullable
  protected String getDefaultTemplateName(@NotNull PsiDirectory dir) {
    String property = getDefaultTemplateProperty();
    return property == null ? null : PropertiesComponent.getInstance(dir.getProject()).getValue(property);
  }

  @NonNls
  @Nullable
  protected String getDefaultTemplateProperty() {
    return null;
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Presentation presentation = e.getPresentation();

    final boolean enabled = isAvailable(dataContext);

    presentation.setEnabledAndVisible(enabled);
  }

  protected boolean isAvailable(DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    return project != null && view != null && view.getDirectories().length != 0;
  }

  @NlsContexts.Command
  protected abstract String getActionName(PsiDirectory directory, @NonNls @NotNull String newName, @NonNls String templateName);

  @Nls(capitalization = Nls.Capitalization.Title)
  @NotNull
  protected String getErrorTitle() {
    return CommonBundle.getErrorTitle();
  }

  private static Integer getOffsetToPreserve(Editor editor) {
    if (editor == null) return -1;
    int offset = editor.getCaretModel().getOffset();
    if (offset == 0) return -1;
    return offset;
  }

  //todo append $END variable to templates?
  public static void moveCaretAfterNameIdentifier(PsiNameIdentifierOwner createdElement) {
    final Project project = createdElement.getProject();
    final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null) {
      final VirtualFile virtualFile = createdElement.getContainingFile().getVirtualFile();
      if (virtualFile != null) {
        if (FileDocumentManager.getInstance().getDocument(virtualFile) == editor.getDocument()) {
          final PsiElement nameIdentifier = createdElement.getNameIdentifier();
          if (nameIdentifier != null) {
            editor.getCaretModel().moveToOffset(nameIdentifier.getTextRange().getEndOffset());
          }
        }
      }
    }
  }
}
