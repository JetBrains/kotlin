// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.EmptyIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInsight.intention.impl.config.IntentionManagerSettings;
import com.intellij.codeInspection.SuppressIntentionActionFromFix;
import com.intellij.codeInspection.ex.QuickFixWrapper;
import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class CachedIntentions {
  private static final Logger LOG = Logger.getInstance(CachedIntentions.class);

  private final Set<IntentionActionWithTextCaching> myIntentions = ConcurrentCollectionFactory.createConcurrentSet(ACTION_TEXT_AND_CLASS_EQUALS);
  private final Set<IntentionActionWithTextCaching> myErrorFixes = ConcurrentCollectionFactory.createConcurrentSet(ACTION_TEXT_AND_CLASS_EQUALS);
  private final Set<IntentionActionWithTextCaching> myInspectionFixes = ConcurrentCollectionFactory.createConcurrentSet(ACTION_TEXT_AND_CLASS_EQUALS);
  private final Set<IntentionActionWithTextCaching> myGutters = ConcurrentCollectionFactory.createConcurrentSet(ACTION_TEXT_AND_CLASS_EQUALS);
  private final Set<IntentionActionWithTextCaching> myNotifications = ConcurrentCollectionFactory.createConcurrentSet(ACTION_TEXT_AND_CLASS_EQUALS);
  private int myOffset;
  @Nullable
  private final Editor myEditor;
  @NotNull
  private final PsiFile myFile;
  @NotNull
  private final Project myProject;

  public CachedIntentions(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor) {
    myProject = project;
    myFile = file;
    myEditor = editor;
  }

  @NotNull
  public Set<IntentionActionWithTextCaching> getIntentions() {
    return myIntentions;
  }

  @NotNull
  public Set<IntentionActionWithTextCaching> getErrorFixes() {
    return myErrorFixes;
  }

  @NotNull
  public Set<IntentionActionWithTextCaching> getInspectionFixes() {
    return myInspectionFixes;
  }

  @NotNull
  public Set<IntentionActionWithTextCaching> getGutters() {
    return myGutters;
  }

  @NotNull
  public Set<IntentionActionWithTextCaching> getNotifications() {
    return myNotifications;
  }

  @Nullable
  public Editor getEditor() {
    return myEditor;
  }

  @NotNull
  public PsiFile getFile() {
    return myFile;
  }

  @NotNull
  public Project getProject() {
    return myProject;
  }

  public int getOffset() {
    return myOffset;
  }

  @NotNull
  public static CachedIntentions create(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor, @NotNull ShowIntentionsPass.IntentionsInfo intentions) {
    CachedIntentions res = new CachedIntentions(project, file, editor);
    res.wrapAndUpdateActions(intentions, false);
    return res;
  }

  @NotNull
  public static CachedIntentions createAndUpdateActions(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor, @NotNull ShowIntentionsPass.IntentionsInfo intentions) {
    CachedIntentions res = new CachedIntentions(project, file, editor);
    res.wrapAndUpdateActions(intentions, true);
    return res;
  }

  private static final TObjectHashingStrategy<IntentionActionWithTextCaching> ACTION_TEXT_AND_CLASS_EQUALS = new TObjectHashingStrategy<IntentionActionWithTextCaching>() {
    @Override
    public int computeHashCode(final IntentionActionWithTextCaching object) {
      return object.getText().hashCode();
    }

    @Override
    public boolean equals(final IntentionActionWithTextCaching o1, final IntentionActionWithTextCaching o2) {
      return getActionClass(o1) == getActionClass(o2) && o1.getText().equals(o2.getText());
    }

    private Class<? extends IntentionAction> getActionClass(IntentionActionWithTextCaching o1) {
      return IntentionActionDelegate.unwrap(o1.getAction()).getClass();
    }
  };

  public boolean wrapAndUpdateActions(@NotNull ShowIntentionsPass.IntentionsInfo newInfo, boolean callUpdate) {
    myOffset = newInfo.getOffset();
    boolean changed = wrapActionsTo(newInfo.errorFixesToShow, myErrorFixes, callUpdate);
    changed |= wrapActionsTo(newInfo.inspectionFixesToShow, myInspectionFixes, callUpdate);
    changed |= wrapActionsTo(newInfo.intentionsToShow, myIntentions, callUpdate);
    changed |= wrapActionsTo(newInfo.guttersToShow, myGutters, callUpdate);
    changed |= wrapActionsTo(newInfo.notificationActionsToShow, myNotifications, callUpdate);
    return changed;
  }

  public boolean addActions(@NotNull ShowIntentionsPass.IntentionsInfo info) {
    boolean changed = addActionsTo(info.errorFixesToShow, myErrorFixes);
    changed |= addActionsTo(info.inspectionFixesToShow, myInspectionFixes);
    changed |= addActionsTo(info.intentionsToShow, myIntentions);
    changed |= addActionsTo(info.guttersToShow, myGutters);
    changed |= addActionsTo(info.notificationActionsToShow, myNotifications);
    return changed;
  }

  private boolean addActionsTo(@NotNull List<? extends HighlightInfo.IntentionActionDescriptor> newDescriptors,
                               @NotNull Set<? super IntentionActionWithTextCaching> cachedActions) {
    boolean changed = false;
    for (HighlightInfo.IntentionActionDescriptor descriptor : newDescriptors) {
      changed |= cachedActions.add(wrapAction(descriptor, myFile, myFile, myEditor));
    }
    return changed;
  }

  private boolean wrapActionsTo(@NotNull List<? extends HighlightInfo.IntentionActionDescriptor> newDescriptors,
                                @NotNull Set<IntentionActionWithTextCaching> cachedActions,
                                boolean callUpdate) {
    boolean changed = false;
    if (myEditor == null) {
      LOG.assertTrue(!callUpdate);
      for (HighlightInfo.IntentionActionDescriptor descriptor : newDescriptors) {
        changed |= cachedActions.add(wrapAction(descriptor, myFile, myFile, null));
      }
    } else {
      final int caretOffset = myEditor.getCaretModel().getOffset();
      final int fileOffset = caretOffset > 0 && caretOffset == myFile.getTextLength() ? caretOffset - 1 : caretOffset;
      PsiElement element;
      final PsiElement hostElement;
      if (myFile instanceof PsiCompiledElement) {
        hostElement = element = myFile;
      }
      else if (PsiDocumentManager.getInstance(myProject).isUncommited(myEditor.getDocument())) {
        //???
        FileViewProvider viewProvider = myFile.getViewProvider();
        hostElement = element = viewProvider.findElementAt(fileOffset, viewProvider.getBaseLanguage());
      }
      else {
        hostElement = myFile.getViewProvider().findElementAt(fileOffset, myFile.getLanguage());
        element = InjectedLanguageUtil.findElementAtNoCommit(myFile, fileOffset);
      }
      PsiFile injectedFile;
      Editor injectedEditor;
      if (element == null || element == hostElement) {
        injectedFile = myFile;
        injectedEditor = myEditor;
      }
      else {
        injectedFile = element.getContainingFile();
        injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(myEditor, injectedFile);
      }

      for (Iterator<IntentionActionWithTextCaching> iterator = cachedActions.iterator(); iterator.hasNext(); ) {
        IntentionActionWithTextCaching cachedAction = iterator.next();
        IntentionAction action = cachedAction.getAction();
        if (!ShowIntentionActionsHandler.availableFor(myFile, myEditor, action) &&
            (hostElement == element || element != null && !ShowIntentionActionsHandler.availableFor(injectedFile, injectedEditor, action))) {
          iterator.remove();
          changed = true;
        }
      }

      Set<IntentionActionWithTextCaching> wrappedNew =
        new THashSet<>(newDescriptors.size(), ACTION_TEXT_AND_CLASS_EQUALS);
      for (HighlightInfo.IntentionActionDescriptor descriptor : newDescriptors) {
        final IntentionAction action = descriptor.getAction();
        if (element != null &&
            element != hostElement &&
            (!callUpdate || ShowIntentionActionsHandler.availableFor(injectedFile, injectedEditor, action))) {
          IntentionActionWithTextCaching cachedAction = wrapAction(descriptor, element, injectedFile, injectedEditor);
          wrappedNew.add(cachedAction);
          changed |= cachedActions.add(cachedAction);
        }
        else if (hostElement != null && (!callUpdate || ShowIntentionActionsHandler.availableFor(myFile, myEditor, action))) {
          IntentionActionWithTextCaching cachedAction = wrapAction(descriptor, hostElement, myFile, myEditor);
          wrappedNew.add(cachedAction);
          changed |= cachedActions.add(cachedAction);
        }
      }
      for (Iterator<IntentionActionWithTextCaching> iterator = cachedActions.iterator(); iterator.hasNext(); ) {
        IntentionActionWithTextCaching cachedAction = iterator.next();
        if (!wrappedNew.contains(cachedAction)) {
          // action disappeared
          iterator.remove();
          changed = true;
        }
      }
    }
    return changed;
  }

  @NotNull
  IntentionActionWithTextCaching wrapAction(@NotNull HighlightInfo.IntentionActionDescriptor descriptor,
                                            @Nullable PsiElement element,
                                            @Nullable PsiFile containingFile,
                                            @Nullable Editor containingEditor) {
    IntentionActionWithTextCaching cachedAction = new IntentionActionWithTextCaching(descriptor);
    if (element == null) return cachedAction;
    final List<IntentionAction> options = descriptor.getOptions(element, containingEditor);
    if (options == null) return cachedAction;
    for (IntentionAction option : options) {
      if (containingFile != null && containingEditor != null && myEditor != null) {
        if (!ShowIntentionActionsHandler.availableFor(containingFile, containingEditor, option)) {
          //if option is not applicable in injected fragment, check in host file context
          if (containingEditor == myEditor || !ShowIntentionActionsHandler.availableFor(myFile, myEditor, option)) {
            continue;
          }
        }
      }
      else if (!option.isAvailable(myProject, containingEditor, containingFile)) {
        // if option is not applicable in injected fragment, check in host file context
        if (containingEditor == myEditor || !option.isAvailable(myProject, myEditor, myFile)) {
          continue;
        }
      }
      IntentionActionWithTextCaching textCaching = new IntentionActionWithTextCaching(option);
      boolean isErrorFix = myErrorFixes.contains(textCaching);
      if (isErrorFix) {
        cachedAction.addErrorFix(option);
      }
      boolean isInspectionFix = myInspectionFixes.contains(textCaching);
      if (isInspectionFix) {
        cachedAction.addInspectionFix(option);
      }
      else {
        cachedAction.addIntention(option);
      }
    }
    return cachedAction;
  }

  @NotNull
  public List<IntentionActionWithTextCaching> getAllActions() {
    List<IntentionActionWithTextCaching> result = new ArrayList<>(myErrorFixes);
    result.addAll(myInspectionFixes);
    result.addAll(myIntentions);
    result.addAll(myGutters);
    result.addAll(myNotifications);
    result = DumbService.getInstance(myProject).filterByDumbAwareness(result);
    Collections.sort(result, (o1, o2) -> {
      int weight1 = getWeight(o1);
      int weight2 = getWeight(o2);
      if (weight1 != weight2) {
        return weight2 - weight1;
      }
      return o1.compareTo(o2);
    });
    return result;
  }

  private int getWeight(@NotNull IntentionActionWithTextCaching action) {
    IntentionAction a = action.getAction();
    int group = getGroup(action).getPriority();
    while (a instanceof IntentionActionDelegate) {
      a = ((IntentionActionDelegate)a).getDelegate();
    }
    if (a instanceof PriorityAction) {
      return group + getPriorityWeight(((PriorityAction)a).getPriority());
    }
    if (a instanceof SuppressIntentionActionFromFix) {
      if (((SuppressIntentionActionFromFix)a).isShouldBeAppliedToInjectionHost() == ThreeState.NO) {
        return group - 1;
      }
    }
    return group;
  }

  private static int getPriorityWeight(PriorityAction.Priority priority) {
    switch (priority) {
      case TOP:
        return 20;
      case HIGH:
        return 3;
      case LOW:
        return -3;
      default:
        return 0;
    }
  }

  @NotNull
  public IntentionGroup getGroup(@NotNull IntentionActionWithTextCaching action) {
    if (myErrorFixes.contains(action)) {
      return IntentionGroup.ERROR;
    }
    if (myInspectionFixes.contains(action)) {
      return IntentionGroup.INSPECTION;
    }
    if (myNotifications.contains(action)) {
      return IntentionGroup.NOTIFICATION;
    }
    if (myGutters.contains(action)) {
      return IntentionGroup.GUTTER;
    }
    if (action.getAction() instanceof EmptyIntentionAction) {
      return IntentionGroup.EMPTY_ACTION;
    }
    return IntentionGroup.OTHER;
  }

  @NotNull
  public Icon getIcon(@NotNull IntentionActionWithTextCaching value) {
    if (value.getIcon() != null) {
      return value.getIcon();
    }

    IntentionAction action = value.getAction();

    while (action instanceof IntentionActionDelegate) {
      action = ((IntentionActionDelegate)action).getDelegate();
    }
    Object iconable = action;
    //custom icon
    if (action instanceof QuickFixWrapper) {
      iconable = ((QuickFixWrapper)action).getFix();
    }

    if (iconable instanceof Iconable) {
      final Icon icon = ((Iconable)iconable).getIcon(0);
      if (icon != null) {
        return icon;
      }
    }

    if (IntentionManagerSettings.getInstance().isShowLightBulb(action)) {
      return myErrorFixes.contains(value) ? AllIcons.Actions.QuickfixBulb
                                          : myInspectionFixes.contains(value) ? AllIcons.Actions.IntentionBulb :
                                            AllIcons.Actions.RealIntentionBulb;
    }
    else {
      if (myErrorFixes.contains(value)) return AllIcons.Actions.QuickfixOffBulb;
      Icon disabledIcon = IconLoader.getDisabledIcon(AllIcons.Actions.RealIntentionBulb);
      return disabledIcon != null ? disabledIcon : AllIcons.Actions.RealIntentionBulb;
    }
  }

  public boolean showBulb() {
    return ContainerUtil.exists(getAllActions(), info -> IntentionManagerSettings.getInstance().isShowLightBulb(info.getAction()));
  }

  @Override
  public String toString() {
    return "CachedIntentions{" +
           "myIntentions=" + myIntentions +
           ", myErrorFixes=" + myErrorFixes +
           ", myInspectionFixes=" + myInspectionFixes +
           ", myGutters=" + myGutters +
           ", myNotifications=" + myNotifications +
           '}';
  }
}
