// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.CleanupOnScopeIntention;
import com.intellij.codeInsight.daemon.impl.EditCleanupProfileIntentionAction;
import com.intellij.codeInsight.intention.*;
import com.intellij.codeInspection.GlobalInspectionTool;
import com.intellij.codeInspection.GlobalSimpleInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.actions.CleanupAllIntention;
import com.intellij.codeInspection.actions.CleanupInspectionIntention;
import com.intellij.codeInspection.actions.RunInspectionIntention;
import com.intellij.codeInspection.ex.*;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class IntentionManagerImpl extends IntentionManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(IntentionManagerImpl.class);
  public static final ExtensionPointName<IntentionActionBean> EP_INTENTION_ACTIONS = new ExtensionPointName<>("com.intellij.intentionAction");

  private final List<IntentionAction> myActions;
  private final AtomicReference<ScheduledFuture<?>> myScheduledFuture = new AtomicReference<>();
  private boolean myIntentionsDisabled;

  public IntentionManagerImpl() {
    List<IntentionAction> actions = new ArrayList<>(EP_INTENTION_ACTIONS.getPoint().size() + 1);
    actions.add(new EditInspectionToolsSettingsInSuppressedPlaceIntention());
    EP_INTENTION_ACTIONS.forEachExtensionSafe(extension -> actions.add(new IntentionActionWrapper(extension)));
    myActions = ContainerUtil.createLockFreeCopyOnWriteList(actions);

    EP_INTENTION_ACTIONS.addExtensionPointListener(new ExtensionPointListener<IntentionActionBean>() {
      @Override
      public void extensionAdded(@NotNull IntentionActionBean extension, @NotNull PluginDescriptor pluginDescriptor) {
        myActions.add(new IntentionActionWrapper(extension));
      }

      @Override
      public void extensionRemoved(@NotNull IntentionActionBean extension, @NotNull PluginDescriptor pluginDescriptor) {
        myActions.removeIf((wrapper) -> {
          return wrapper instanceof IntentionActionWrapper &&
                 ((IntentionActionWrapper)wrapper).getImplementationClassName().equals(extension.className);
        });
      }
    }, this);
  }

  @Override
  public void registerIntentionAndMetaData(@NotNull IntentionAction action, String @NotNull ... category) {
    addAction(action);

    String descriptionDirectoryName = action instanceof IntentionActionWrapper
                                      ? ((IntentionActionWrapper)action).getDescriptionDirectoryName()
                                      : IntentionActionWrapper.getDescriptionDirectoryName(action.getClass().getName());
    IntentionManagerSettings settings = IntentionManagerSettings.getInstance();
    settings.registerIntentionMetaData(action, category, descriptionDirectoryName);
  }

  @Override
  public void unregisterIntention(@NotNull IntentionAction intentionAction) {
    myActions.remove(intentionAction);
    IntentionManagerSettings settings = IntentionManagerSettings.getInstance();
    settings.unregisterMetaData(intentionAction);
  }

  @Override
  @NotNull
  public List<IntentionAction> getStandardIntentionOptions(@NotNull HighlightDisplayKey displayKey,
                                                           @NotNull PsiElement context) {
    checkForDuplicates();
    List<IntentionAction> options = new ArrayList<>(9);
    options.add(new EditInspectionToolsSettingsAction(displayKey));
    options.add(new RunInspectionIntention(displayKey));
    options.add(new DisableInspectionToolAction(displayKey));
    return options;
  }

  @Nullable
  @Override
  public IntentionAction createFixAllIntention(@NotNull InspectionToolWrapper<?, ?> toolWrapper, @NotNull IntentionAction action) {
    checkForDuplicates();
    if (toolWrapper instanceof GlobalInspectionToolWrapper) {
      LocalInspectionToolWrapper localWrapper = ((GlobalInspectionToolWrapper)toolWrapper).getSharedLocalInspectionToolWrapper();
      if (localWrapper != null) {
        toolWrapper = localWrapper;
      }
    }

    if (toolWrapper instanceof LocalInspectionToolWrapper) {
      return createFixAllIntentionInternal(toolWrapper, action);
    }
    if (toolWrapper instanceof GlobalInspectionToolWrapper) {
      GlobalInspectionTool wrappedTool = ((GlobalInspectionToolWrapper)toolWrapper).getTool();
      if (wrappedTool instanceof GlobalSimpleInspectionTool && (action instanceof LocalQuickFix || action instanceof QuickFixWrapper)) {
        return createFixAllIntentionInternal(toolWrapper, action);
      }
    }
    else {
      throw new AssertionError("unknown tool: " + toolWrapper);
    }
    return null;
  }

  @Override
  public void dispose() {
  }

  private static IntentionAction createFixAllIntentionInternal(@NotNull InspectionToolWrapper<?, ?> toolWrapper,
                                                               @NotNull IntentionAction action) {
    PsiFile file = null;
    FileModifier fix = action;
    if (action instanceof QuickFixWrapper) {
      fix = ((QuickFixWrapper)action).getFix();
      file = ((QuickFixWrapper)action).getFile();
    }
    return new CleanupInspectionIntention(toolWrapper, fix, file, action.getText());
  }

  @NotNull
  @Override
  public IntentionAction createCleanupAllIntention() {
    return CleanupAllIntention.INSTANCE;
  }

  @NotNull
  @Override
  public List<IntentionAction> getCleanupIntentionOptions() {
    ArrayList<IntentionAction> options = new ArrayList<>();
    options.add(EditCleanupProfileIntentionAction.INSTANCE);
    options.add(CleanupOnScopeIntention.INSTANCE);
    return options;
  }

  @Override
  @NotNull
  public LocalQuickFix convertToFix(@NotNull IntentionAction action) {
    if (action instanceof LocalQuickFix) {
      return (LocalQuickFix)action;
    }
    return new LocalQuickFix() {
      @Override
      @NotNull
      public String getName() {
        return action.getText();
      }

      @Override
      @NotNull
      public String getFamilyName() {
        return action.getFamilyName();
      }

      @Override
      public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiFile psiFile = descriptor.getPsiElement().getContainingFile();
        try {
          action.invoke(project, new LazyEditor(psiFile), psiFile);
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    };
  }

  @Override
  public void addAction(@NotNull IntentionAction action) {
    myActions.add(action);
  }

  @Override
  public IntentionAction @NotNull [] getIntentionActions() {
    if (myIntentionsDisabled) {
      return IntentionAction.EMPTY_ARRAY;
    }
    return myActions.toArray(IntentionAction.EMPTY_ARRAY);
  }

  @Override
  public @NotNull List<IntentionAction> getAvailableIntentions() {
    if (myIntentionsDisabled) {
      return Collections.emptyList();
    }

    checkForDuplicates();
    List<IntentionAction> list = new ArrayList<>(myActions.size());
    IntentionManagerSettings settings = IntentionManagerSettings.getInstance();
    for (IntentionAction action : myActions) {
      if (settings.isEnabled(action)) {
        list.add(action);
      }
    }
    return list;
  }

  private boolean checkedForDuplicates; // benign data race
  // check that the intention of some class registered only once
  public void checkForDuplicates() {
    if (checkedForDuplicates) {
      return;
    }

    checkedForDuplicates = true;
    Map<String, List<IntentionAction>> map = new HashMap<>(myActions.size());
    for (IntentionAction action : myActions) {
      map.computeIfAbsent(action instanceof IntentionActionDelegate
                          ? ((IntentionActionDelegate)action).getImplementationClassName()
                          : action.getClass().getName(), k -> new SmartList<>()).add(action);
    }
    List<String> duplicates = new ArrayList<>();
    for (List<IntentionAction> list : map.values()) {
      if (list.size() > 1) {
        duplicates.add(list.size() + " intention duplicates found for " + IntentionActionDelegate.unwrap(list.get(0))
                       + " (" + list.get(0).getClass()
                       + "; plugin " + PluginManager.getInstance().getPluginOrPlatformByClassName(list.get(0).getClass().getName()) + ")");
      }
    }

    if (!duplicates.isEmpty()) {
      throw new IllegalStateException(duplicates.toString());
    }
  }

  public boolean hasActiveRequests() {
    return myScheduledFuture.get() != null;
  }

  @TestOnly
  public <T extends Throwable> void withDisabledIntentions(ThrowableRunnable<T> runnable) throws T {
    boolean oldIntentionsDisabled = myIntentionsDisabled;
    myIntentionsDisabled = true;
    try {
      runnable.run();
    }
    finally {
      myIntentionsDisabled = oldIntentionsDisabled;
    }
  }
}
