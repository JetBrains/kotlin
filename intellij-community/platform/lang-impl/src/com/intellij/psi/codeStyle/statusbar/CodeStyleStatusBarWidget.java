// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.statusbar;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.*;
import com.intellij.psi.codeStyle.modifier.CodeStyleSettingsModifier;
import com.intellij.psi.codeStyle.modifier.CodeStyleStatusBarUIContributor;
import com.intellij.psi.codeStyle.modifier.TransientCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;

public class CodeStyleStatusBarWidget extends EditorBasedStatusBarPopup implements CodeStyleSettingsListener {
  public static final String WIDGET_ID = CodeStyleStatusBarWidget.class.getName();

  public CodeStyleStatusBarWidget(@NotNull Project project) {
    super(project);
  }

  @NotNull
  @Override
  protected WidgetState getWidgetState(@Nullable VirtualFile file) {
    if (file == null) return WidgetState.HIDDEN;
    PsiFile psiFile = getPsiFile();
    if (psiFile == null || !psiFile.isWritable()) return WidgetState.HIDDEN;
    CodeStyleSettings settings = CodeStyle.getSettings(psiFile);
    IndentOptions indentOptions = CodeStyle.getIndentOptions(psiFile);
    if (settings instanceof TransientCodeStyleSettings) {
      return createWidgetState(psiFile, indentOptions, getUiContributor((TransientCodeStyleSettings)settings));
    }
    else {
      return createWidgetState(psiFile, indentOptions, getUiContributor(file, indentOptions));
    }
  }


  @Nullable
  private static CodeStyleStatusBarUIContributor getUiContributor(@NotNull TransientCodeStyleSettings settings) {
    final CodeStyleSettingsModifier modifier = settings.getModifier();
    return modifier != null ? modifier.getStatusBarUiContributor(settings) : null;
  }


  @Nullable
  private static IndentStatusBarUIContributor getUiContributor(@NotNull VirtualFile file, @NotNull IndentOptions indentOptions) {
    FileIndentOptionsProvider provider = findProvider(file, indentOptions);
    if (provider != null) {
      return provider.getIndentStatusBarUiContributor(indentOptions);
    }
    return null;
  }

  @Nullable
  private static FileIndentOptionsProvider findProvider(@NotNull VirtualFile file, @NotNull IndentOptions indentOptions) {
    FileIndentOptionsProvider optionsProvider = indentOptions.getFileIndentOptionsProvider();
    if (optionsProvider != null) return optionsProvider;
    for (FileIndentOptionsProvider provider : FileIndentOptionsProvider.EP_NAME.getExtensions()) {
      IndentStatusBarUIContributor uiContributor = provider.getIndentStatusBarUiContributor(indentOptions);
      if (uiContributor != null && uiContributor.areActionsAvailable(file)) {
        return provider;
      }
    }
    return null;
  }

  private static WidgetState createWidgetState(@NotNull PsiFile psiFile,
                                               @NotNull final IndentOptions indentOptions,
                                               @Nullable CodeStyleStatusBarUIContributor uiContributor) {
    if (uiContributor != null) {
      return new MyWidgetState(uiContributor.getTooltip(), uiContributor.getStatusText(psiFile), psiFile, indentOptions, uiContributor);
    }
    else {
      String indentInfo = IndentStatusBarUIContributor.getIndentInfo(indentOptions);
      String tooltip = IndentStatusBarUIContributor.createTooltip(indentInfo, null);
      return new MyWidgetState(tooltip, indentInfo, psiFile, indentOptions, null);
    }
  }


  @Nullable
  private PsiFile getPsiFile()  {
    Editor editor = getEditor();
    Project project = getProject();
    if (editor != null && project != null) {
      Document document = editor.getDocument();
      return PsiDocumentManager.getInstance(project).getPsiFile(document);
    }
    return null;
  }

  @Nullable
  @Override
  protected ListPopup createPopup(DataContext context) {
    WidgetState state = getWidgetState(context.getData(CommonDataKeys.VIRTUAL_FILE));
    Editor editor = getEditor();
    PsiFile psiFile = getPsiFile();
    if (state instanceof MyWidgetState && editor != null && psiFile != null) {
      AnAction[] actions = getActions(((MyWidgetState)state).getContributor(), psiFile);
      ActionGroup actionGroup = new ActionGroup() {
        @NotNull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
          return actions;
        }
      };
      return JBPopupFactory.getInstance().createActionGroupPopup(
        "Code Style", actionGroup, context,
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }
    return null;
  }

  @NotNull
  private static AnAction[] getActions(@Nullable final CodeStyleStatusBarUIContributor uiContributor, @NotNull PsiFile psiFile) {
    List<AnAction> allActions = new ArrayList<>();
    if (uiContributor != null) {
      AnAction[] actions = uiContributor.getActions(psiFile);
      if (actions != null) {
        allActions.addAll(Arrays.asList(actions));
      }
    }
    if (uiContributor == null ||
        (uiContributor instanceof IndentStatusBarUIContributor) &&
        ((IndentStatusBarUIContributor)uiContributor).isShowFileIndentOptionsEnabled()) {
      allActions.add(CodeStyleStatusBarWidgetProvider.createDefaultIndentConfigureAction(psiFile));
    }
    if (uiContributor != null) {
      AnAction disabledAction = uiContributor.createDisableAction(psiFile.getProject());
      if (disabledAction != null) {
        allActions.add(disabledAction);
      }
      AnAction showAllAction = uiContributor.createShowAllAction(psiFile.getProject());
      if (showAllAction != null) {
        allActions.add(showAllAction);
      }
    }
    return allActions.toArray(AnAction.EMPTY_ARRAY);
  }

  @Override
  protected void registerCustomListeners() {
    CodeStyleSettingsManager.getInstance(getProject()).addListener(this);
  }

  @Override
  public void codeStyleSettingsChanged(@NotNull CodeStyleSettingsChangeEvent event) {
    update();
  }

  @NotNull
  @Override
  protected StatusBarWidget createInstance(Project project) {
    return new CodeStyleStatusBarWidget(project);
  }


  @NotNull
  @Override
  public String ID() {
    return WIDGET_ID;
  }

  private static class MyWidgetState extends WidgetState {

    private final @NotNull IndentOptions myIndentOptions;
    private final @Nullable CodeStyleStatusBarUIContributor myContributor;
    private final @NotNull PsiFile myPsiFile;

    protected MyWidgetState(String toolTip,
                            String text,
                            @NotNull PsiFile psiFile,
                            @NotNull IndentOptions indentOptions,
                            @Nullable CodeStyleStatusBarUIContributor uiContributor) {
      super(toolTip, text, true);
      myIndentOptions = indentOptions;
      myContributor = uiContributor;
      myPsiFile = psiFile;
      if (uiContributor != null) {
        setIcon(uiContributor.getIcon());
      }
    }

    @Nullable
    public CodeStyleStatusBarUIContributor getContributor() {
      return myContributor;
    }

    @NotNull
    public IndentOptions getIndentOptions() {
      return myIndentOptions;
    }

    @NotNull
    public PsiFile getPsiFile() {
      return myPsiFile;
    }
  }

  @Override
  public void dispose() {
    CodeStyleSettingsManager.removeListener(myProject, this);
    super.dispose();
  }
}
