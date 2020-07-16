// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionAction;
import com.intellij.ide.actions.SetShortcutAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.util.gotoByName.GotoActionItemProvider;
import com.intellij.ide.util.gotoByName.GotoActionModel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.ActionShortcutRestrictions;
import com.intellij.openapi.keymap.impl.ui.KeymapPanel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;
import static com.intellij.openapi.keymap.KeymapUtil.getFirstKeyboardShortcutText;

public class ActionSearchEverywhereContributor implements WeightedSearchEverywhereContributor<GotoActionModel.MatchedValue> {

  private static final Logger LOG = Logger.getInstance(ActionSearchEverywhereContributor.class);

  private final Project myProject;
  private final WeakReference<Component> myContextComponent;
  private final GotoActionModel myModel;
  private final GotoActionItemProvider myProvider;
  protected boolean myDisabledActions;

  public ActionSearchEverywhereContributor(Project project, Component contextComponent, Editor editor) {
    myProject = project;
    myContextComponent = new WeakReference<>(contextComponent);
    myModel = new GotoActionModel(project, contextComponent, editor);
    myProvider = new GotoActionItemProvider(myModel);
  }

  @NotNull
  @Override
  public String getGroupName() {
    return IdeBundle.message("search.everywhere.group.name.actions");
  }

  @NotNull
  @Override
  public String getAdvertisement() {
    ShortcutSet altEnterShortcutSet = getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
    String altEnter = getFirstKeyboardShortcutText(altEnterShortcutSet);
    return "Press " + altEnter + " to assign a shortcut";
  }

  public String includeNonProjectItemsText() {
    return IdeBundle.message("checkbox.disabled.included");
  }

  @Override
  public int getSortWeight() {
    return 400;
  }

  @Override
  public boolean isShownInSeparateTab() {
    return true;
  }

  @Override
  public void fetchWeightedElements(@NotNull String pattern,
                                    @NotNull ProgressIndicator progressIndicator,
                                    @NotNull Processor<? super FoundItemDescriptor<GotoActionModel.MatchedValue>> consumer) {

    if (StringUtil.isEmptyOrSpaces(pattern)) {
      return;
    }

    myProvider.filterElements(pattern, element -> {
      if (progressIndicator.isCanceled()) return false;

      if (!myDisabledActions &&
          element.value instanceof GotoActionModel.ActionWrapper &&
          !((GotoActionModel.ActionWrapper)element.value).isAvailable()) {
        return true;
      }

      if (element == null) {
        LOG.error("Null action has been returned from model");
        return true;
      }

      FoundItemDescriptor<GotoActionModel.MatchedValue> descriptor = new FoundItemDescriptor<>(element, element.getMatchingDegree());
      return consumer.process(descriptor);
    });
  }

  @NotNull
  @Override
  public List<AnAction> getActions(@NotNull Runnable onChanged) {
    return Collections.singletonList(new CheckBoxSearchEverywhereToggleAction(includeNonProjectItemsText()) {
      @Override
      public boolean isEverywhere() {
        return myDisabledActions;
      }

      @Override
      public void setEverywhere(boolean state) {
        myDisabledActions = state;
        onChanged.run();
      }
    });
  }

  @NotNull
  @Override
  public ListCellRenderer<? super GotoActionModel.MatchedValue> getElementsRenderer() {
    return new GotoActionModel.GotoActionListCellRenderer(myModel::getGroupName, true);
  }

  @Override
  public boolean showInFindResults() {
    return false;
  }

  @NotNull
  @Override
  public String getSearchProviderId() {
    return ActionSearchEverywhereContributor.class.getSimpleName();
  }

  @Override
  public Object getDataForItem(@NotNull GotoActionModel.MatchedValue element, @NotNull String dataId) {
    if (SetShortcutAction.SELECTED_ACTION.is(dataId)) {
      return getAction(element);
    }

    if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION.is(dataId)) {
      AnAction action = getAction(element);
      if (action != null) {
        String description = action.getTemplatePresentation().getDescription();
        if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
          String presentableId = StringUtil.notNullize(ActionManager.getInstance().getId(action), "class: " + action.getClass().getName());
          return String.format("[%s] %s", presentableId, StringUtil.notNullize(description));
        }
        return description;
      }
    }

    return null;
  }

  @Override
  public boolean processSelectedItem(@NotNull GotoActionModel.MatchedValue item, int modifiers, @NotNull String text) {
    if (modifiers == InputEvent.ALT_MASK) {
      showAssignShortcutDialog(item);
      return true;
    }

    Object selected = item.value;

    if (selected instanceof BooleanOptionDescription) {
      final BooleanOptionDescription option = (BooleanOptionDescription)selected;
      if (selected instanceof BooleanOptionDescription.RequiresRebuild) {
        myModel.clearActions();           // release references to plugin actions so that the plugin can be unloaded successfully
        myProvider.clearIntentions();
      }
      option.setOptionState(!option.isOptionEnabled());
      if (selected instanceof BooleanOptionDescription.RequiresRebuild) {
        myModel.rebuildActions();
      }
      return false;
    }

    GotoActionAction.openOptionOrPerformAction(selected, text, myProject, myContextComponent.get());
    boolean inplaceChange = selected instanceof GotoActionModel.ActionWrapper
                            && ((GotoActionModel.ActionWrapper)selected).getAction() instanceof ToggleAction;
    return !inplaceChange;
  }

  @Nullable
  private static AnAction getAction(@NotNull GotoActionModel.MatchedValue element) {
    Object value = element.value;
    if (value instanceof GotoActionModel.ActionWrapper) {
      value = ((GotoActionModel.ActionWrapper)value).getAction();
    }
    return value instanceof AnAction ? (AnAction)value : null;
  }

  private void showAssignShortcutDialog(@NotNull GotoActionModel.MatchedValue value) {
    AnAction action = getAction(value);
    if (action == null) return;

    String id = ActionManager.getInstance().getId(action);

    Keymap activeKeymap = Optional.ofNullable(KeymapManager.getInstance())
      .map(KeymapManager::getActiveKeymap)
      .orElse(null);
    if (activeKeymap == null) return;

    ApplicationManager.getApplication().invokeLater(() -> {
      Window window = myProject != null
                      ? WindowManager.getInstance().suggestParentWindow(myProject)
                      : KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
      if (window == null) return;

      KeymapPanel.addKeyboardShortcut(id, ActionShortcutRestrictions.getInstance().getForActionId(id), activeKeymap, window);
    });
  }

  public static class Factory implements SearchEverywhereContributorFactory<GotoActionModel.MatchedValue> {
    @NotNull
    @Override
    public SearchEverywhereContributor<GotoActionModel.MatchedValue> createContributor(@NotNull AnActionEvent initEvent) {
      return new ActionSearchEverywhereContributor(
        initEvent.getProject(),
        initEvent.getData(PlatformDataKeys.CONTEXT_COMPONENT),
        initEvent.getData(CommonDataKeys.EDITOR));
    }
  }
}
