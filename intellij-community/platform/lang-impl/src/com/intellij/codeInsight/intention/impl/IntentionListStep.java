// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.AbstractEmptyIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.codeInsight.intention.impl.preview.IntentionPreviewPopupUpdateProcessor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IntentionListStep implements ListPopupStep<IntentionActionWithTextCaching>, SpeedSearchFilter<IntentionActionWithTextCaching> {
  private static final Logger LOG = Logger.getInstance(IntentionListStep.class);

  private final CachedIntentions myCachedIntentions;
  @Nullable
  private final IntentionHintComponent myIntentionHintComponent;

  private Runnable myFinalRunnable;
  private final Project myProject;
  private final PsiFile myFile;
  @Nullable
  private final Editor myEditor;

  public IntentionListStep(@Nullable IntentionHintComponent intentionHintComponent,
                           @Nullable Editor editor,
                           @NotNull PsiFile file,
                           @NotNull Project project,
                           CachedIntentions intentions) {
    myIntentionHintComponent = intentionHintComponent;
    myProject = project;
    myFile = file;
    myEditor = editor;
    myCachedIntentions = intentions;
  }

  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public boolean isSelectable(final IntentionActionWithTextCaching action) {
    return true;
  }

  @Override
  public PopupStep<?> onChosen(IntentionActionWithTextCaching action, final boolean finalChoice) {
    IntentionAction a = IntentionActionDelegate.unwrap(action.getAction());

    if (finalChoice && !(a instanceof AbstractEmptyIntentionAction)) {
      applyAction(action);
      return FINAL_CHOICE;
    }

    if (hasSubstep(action)) {
      closeIntentionPreviewPopup();

      return getSubStep(action, action.getToolName());
    }

    return FINAL_CHOICE;
  }

  private static void closeIntentionPreviewPopup() {
    ApplicationManager.getApplication().invokeLater(() ->
       StackingPopupDispatcher.getInstance().getPopupStream()
         .filter(popup -> popup.getUserData(IntentionPreviewPopupUpdateProcessor.IntentionPreviewPopupKey.class) != null)
         .collect(Collectors.toList())
         .forEach(popup -> popup.cancel()));
  }

  @Override
  public Runnable getFinalRunnable() {
    return myFinalRunnable;
  }

  private void applyAction(@NotNull IntentionActionWithTextCaching cachedAction) {
    myFinalRunnable = () -> {
      HintManager.getInstance().hideAllHints();
      if (myProject.isDisposed()) return;
      if (myEditor != null && (myEditor.isDisposed() || (!myEditor.getComponent().isShowing() && !ApplicationManager.getApplication().isUnitTestMode()))) return;

      if (DumbService.isDumb(myProject) && !DumbService.isDumbAware(cachedAction)) {
        DumbService.getInstance(myProject).showDumbModeNotification(
          CodeInsightBundle.message("notification.0.is.not.available.during.indexing", cachedAction.getText()));
        return;
      }

      PsiDocumentManager.getInstance(myProject).commitAllDocuments();

      PsiFile file = myEditor != null ? PsiUtilBase.getPsiFileInEditor(myEditor, myProject) : myFile;
      if (file == null) {
        return;
      }

      ShowIntentionActionsHandler.chooseActionAndInvoke(file, myEditor, cachedAction.getAction(), cachedAction.getText(), myProject);
    };
  }


  @NotNull
  IntentionListStep getSubStep(@NotNull IntentionActionWithTextCaching action, final String title) {
    ShowIntentionsPass.IntentionsInfo intentions = new ShowIntentionsPass.IntentionsInfo();
    for (final IntentionAction optionIntention : action.getOptionIntentions()) {
      intentions.intentionsToShow.add(new HighlightInfo.IntentionActionDescriptor(optionIntention, getIcon(optionIntention)));
    }
    for (final IntentionAction optionFix : action.getOptionErrorFixes()) {
      intentions.errorFixesToShow.add(new HighlightInfo.IntentionActionDescriptor(optionFix, getIcon(optionFix)));
    }
    for (final IntentionAction optionFix : action.getOptionInspectionFixes()) {
      intentions.inspectionFixesToShow.add(new HighlightInfo.IntentionActionDescriptor(optionFix, getIcon(optionFix)));
    }

    return new IntentionListStep(myIntentionHintComponent, myEditor, myFile, myProject,
                                 CachedIntentions.create(myProject, myFile, myEditor, intentions)){
      @Override
      public String getTitle() {
        return title;
      }
    };
  }

  private static Icon getIcon(IntentionAction optionIntention) {
    return optionIntention instanceof Iconable ? ((Iconable)optionIntention).getIcon(0) : null;
  }

  @TestOnly
  public Map<IntentionAction, List<IntentionAction>> getActionsWithSubActions() {
    Map<IntentionAction, List<IntentionAction>> result = new LinkedHashMap<>();

    for (IntentionActionWithTextCaching cached : getValues()) {
      IntentionAction action = cached.getAction();
      if (ShowIntentionActionsHandler.chooseFileForAction(myFile, myEditor, action) == null) continue;

      List<IntentionActionWithTextCaching> subActions = getSubStep(cached, cached.getToolName()).getValues();
      List<IntentionAction> options = subActions.stream()
          .map(IntentionActionWithTextCaching::getAction)
          .filter(option -> ShowIntentionActionsHandler.chooseFileForAction(myFile, myEditor, option) != null)
          .collect(Collectors.toList());
      result.put(action, options);
    }
    return result;
  }

  @Override
  public boolean hasSubstep(final IntentionActionWithTextCaching action) {
    return action.getOptionIntentions().size() + action.getOptionErrorFixes().size() > 0;
  }

  @Override
  @NotNull
  public List<IntentionActionWithTextCaching> getValues() {
    return myCachedIntentions.getAllActions();
  }

  @Override
  @NotNull
  public String getTextFor(final IntentionActionWithTextCaching action) {
    final String text = action.getText();
    if (LOG.isDebugEnabled() && text.startsWith("<html>")) {
      LOG.info("IntentionAction.getText() returned HTML: action=" + action.getAction().getClass() + " text=" + text);
    }
    return text;
  }

  @Override
  public Icon getIconFor(final IntentionActionWithTextCaching value) {
    return myCachedIntentions.getIcon(value);
  }

  @Override
  public void canceled() {
    if (myIntentionHintComponent != null) {
      myIntentionHintComponent.canceled(this);
    }
  }

  @Override
  public int getDefaultOptionIndex() { return 0; }
  @Override
  public ListSeparator getSeparatorAbove(final IntentionActionWithTextCaching value) {
    List<IntentionActionWithTextCaching> values = getValues();
    int index = values.indexOf(value);
    if (index <= 0) return null;
    IntentionActionWithTextCaching prev = values.get(index - 1);

    if (myCachedIntentions.getGroup(value) != myCachedIntentions.getGroup(prev)) {
      return new ListSeparator();
    }
    return null;
  }
  @Override
  public boolean isMnemonicsNavigationEnabled() { return false; }
  @Override
  public MnemonicNavigationFilter<IntentionActionWithTextCaching> getMnemonicNavigationFilter() { return null; }
  @Override
  public boolean isSpeedSearchEnabled() { return true; }
  @Override
  public boolean isAutoSelectionEnabled() { return false; }
  @Override
  public SpeedSearchFilter<IntentionActionWithTextCaching> getSpeedSearchFilter() { return this; }

  //speed search filter
  @Override
  public String getIndexedString(final IntentionActionWithTextCaching value) { return getTextFor(value);}
}
