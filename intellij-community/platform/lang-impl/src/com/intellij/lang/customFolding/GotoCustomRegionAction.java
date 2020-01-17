// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.lang.customFolding;

import com.intellij.ide.IdeBundle;
import com.intellij.lang.Language;
import com.intellij.lang.folding.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Rustam Vishnyakov
 */
public class GotoCustomRegionAction extends AnAction implements DumbAware, PopupAction {
  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final Project project = e.getProject();
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (Boolean.TRUE.equals(e.getData(PlatformDataKeys.IS_MODAL_CONTEXT))) {
      return;
    }
    if (project != null && editor != null) {
      if (DumbService.getInstance(project).isDumb()) {
        DumbService.getInstance(project).showDumbModeNotification(IdeBundle.message("goto.custom.region.message.dumb.mode"));
        return;
      }
      CommandProcessor processor = CommandProcessor.getInstance();
      processor.executeCommand(
        project,
        () -> {
          Collection<FoldingDescriptor> foldingDescriptors = getCustomFoldingDescriptors(editor, project);
          if (foldingDescriptors.size() > 0) {
            CustomFoldingRegionsPopup.show(foldingDescriptors, editor, project);
          }
          else {
            notifyCustomRegionsUnavailable(editor, project);
          }
        },
        IdeBundle.message("goto.custom.region.command"),
        null);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setText(IdeBundle.message("goto.custom.region.menu.item"));
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    final Project project = e.getProject();
    boolean isAvailable = editor != null && project != null;
    presentation.setEnabledAndVisible(isAvailable);
  }

  @NotNull
  private static Collection<FoldingDescriptor> getCustomFoldingDescriptors(@NotNull Editor editor, @NotNull Project project) {
    Set<FoldingDescriptor> foldingDescriptors = new HashSet<>();
    final Document document = editor.getDocument();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    PsiFile file = documentManager != null ? documentManager.getPsiFile(document) : null;
    if (file != null) {
      final FileViewProvider viewProvider = file.getViewProvider();
      for (final Language language : viewProvider.getLanguages()) {
        final PsiFile psi = viewProvider.getPsi(language);
        final FoldingBuilder foldingBuilder = LanguageFolding.INSTANCE.forLanguage(language);
        if (psi != null) {
          for (FoldingDescriptor descriptor : LanguageFolding.buildFoldingDescriptors(foldingBuilder, psi, document, false)) {
            CustomFoldingBuilder customFoldingBuilder = getCustomFoldingBuilder(foldingBuilder, descriptor);
            if (customFoldingBuilder != null) {
              if (customFoldingBuilder.isCustomRegionStart(descriptor.getElement())) {
                foldingDescriptors.add(descriptor);
              }
            }
          }
        }
      }
    }
    return foldingDescriptors;
  }

  @Nullable
  private static CustomFoldingBuilder getCustomFoldingBuilder(FoldingBuilder builder, FoldingDescriptor descriptor) {
    if (builder instanceof CustomFoldingBuilder) return (CustomFoldingBuilder)builder;
    FoldingBuilder originalBuilder = CompositeFoldingBuilder.getOriginalBuilder(descriptor);
    if (originalBuilder instanceof CustomFoldingBuilder) return (CustomFoldingBuilder)originalBuilder;
    return null;
  }

  private static void notifyCustomRegionsUnavailable(@NotNull Editor editor, @NotNull Project project) {
    final JBPopupFactory popupFactory = JBPopupFactory.getInstance();
    Balloon balloon = popupFactory
      .createHtmlTextBalloonBuilder(IdeBundle.message("goto.custom.region.message.unavailable"), MessageType.INFO, null)
      .setFadeoutTime(2000)
      .setHideOnClickOutside(true)
      .setHideOnKeyOutside(true)
      .createBalloon();
    Disposer.register(project, balloon);
    balloon.show(popupFactory.guessBestPopupLocation(editor), Balloon.Position.below);
  }
}
