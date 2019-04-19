/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.execution.ui.layout.impl;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutAttractionPolicy;
import com.intellij.execution.ui.layout.LayoutStateDefaults;
import com.intellij.execution.ui.layout.LayoutViewOptions;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithActions;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.ui.switcher.QuickActionProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RunnerLayoutUiImpl implements Disposable.Parent, RunnerLayoutUi, LayoutStateDefaults, LayoutViewOptions, DataProvider {
  private final RunnerLayout myLayout;
  private final RunnerContentUi myContentUI;

  private final ContentManager myViewsContentManager;
  public static final Key<String> CONTENT_TYPE = Key.create("ContentType");

  public RunnerLayoutUiImpl(@NotNull Project project,
                            @NotNull Disposable parent,
                            @NotNull String runnerId,
                            @NotNull String runnerTitle,
                            @NotNull String sessionName) {
    myLayout = RunnerLayoutSettings.getInstance().getLayout(runnerId);
    Disposer.register(parent, this);

    myContentUI = new RunnerContentUi(project, this, ActionManager.getInstance(), IdeFocusManager.getInstance(project), myLayout,
                                      runnerTitle + " - " + sessionName);
    Disposer.register(this, myContentUI);

    myViewsContentManager = getContentFactory().createContentManager(myContentUI.getContentUI(), true, project);
    myViewsContentManager.addDataProvider(this);
    Disposer.register(this, myViewsContentManager);
  }

  @Override
  @NotNull
  public LayoutViewOptions setTopToolbar(@NotNull ActionGroup actions, @NotNull String place) {
    myContentUI.setTopActions(actions, place);
    return this;
  }

  @NotNull
  @Override
  public LayoutStateDefaults initTabDefaults(int id, String text, Icon icon) {
    getLayout().setDefault(id, text, icon);
    return this;
  }

  @NotNull
  @Override
  public LayoutStateDefaults initContentAttraction(@NotNull String contentId, @NotNull String condition, @NotNull LayoutAttractionPolicy policy) {
    getLayout().setDefaultToFocus(contentId, condition, policy);
    return this;
  }

  @NotNull
  @Override
  public LayoutStateDefaults cancelContentAttraction(@NotNull String condition) {
    getLayout().cancelDefaultFocusBy(condition);
    return this;
  }

  @Override
  @NotNull
  public Content addContent(@NotNull Content content) {
    return addContent(content, false, -1, PlaceInGrid.center, false);
  }

  @Override
  @NotNull
  public Content addContent(@NotNull Content content, int defaultTabId, @NotNull PlaceInGrid defaultPlace, boolean defaultIsMinimized) {
    return addContent(content, true, defaultTabId, defaultPlace, defaultIsMinimized);
  }

  public Content addContent(@NotNull Content content, boolean applyDefaults, int defaultTabId, @NotNull PlaceInGrid defaultPlace, boolean defaultIsMinimized) {
    final String id = content.getUserData(CONTENT_TYPE);

    assert id != null : "Content id is missing, use RunnerLayoutUi to create content instances";

    if (applyDefaults) {
      getLayout().setDefault(id, defaultTabId, defaultPlace, defaultIsMinimized);
    }

    getContentManager().addContent(content);
    return content;
  }

  @Override
  @NotNull
  public Content createContent(@NotNull String id, @NotNull JComponent component, @NotNull String displayName, @Nullable Icon icon, @Nullable JComponent focusable) {
    return createContent(id, new ComponentWithActions.Impl(component), displayName, icon, focusable);
  }

  @Override
  @NotNull
  public Content createContent(@NotNull final String contentId, @NotNull final ComponentWithActions withActions, @NotNull final String displayName,
                               @Nullable final Icon icon,
                               @Nullable final JComponent toFocus) {
    final Content content = getContentFactory().createContent(withActions.getComponent(), displayName, false);
    content.putUserData(CONTENT_TYPE, contentId);
    content.putUserData(ViewImpl.ID, contentId);
    content.setIcon(icon);
    if (toFocus != null) {
      content.setPreferredFocusableComponent(toFocus);
    }

    if (!withActions.isContentBuiltIn()) {
      content.setSearchComponent(withActions.getSearchComponent());
      content.setActions(withActions.getToolbarActions(), withActions.getToolbarPlace(), withActions.getToolbarContextComponent());
    }

    return content;
  }

  @Override
  @NotNull
  public JComponent getComponent() {
    return myViewsContentManager.getComponent();
  }

  private static ContentFactory getContentFactory() {
    return ContentFactory.SERVICE.getInstance();
  }

  public RunnerLayout getLayout() {
    return myLayout;
  }

  @Override
  public void updateActionsNow() {
    myContentUI.updateActionsImmediately();
  }

  @Override
  public void beforeTreeDispose() {
    myContentUI.saveUiState();
  }

  @Override
  public void dispose() {
  }

  @Override
  @NotNull
  public ContentManager getContentManager() {
    return myViewsContentManager;
  }

  @NotNull
  @Override
  public ActionCallback selectAndFocus(@Nullable final Content content, boolean requestFocus, final boolean forced) {
    return selectAndFocus(content, requestFocus, forced, false);
  }

  @NotNull
  @Override
  public ActionCallback selectAndFocus(@Nullable final Content content, boolean requestFocus, final boolean forced, boolean implicit) {
    if (content == null) return ActionCallback.REJECTED;
    return getContentManager(content).setSelectedContent(content, requestFocus || shouldRequestFocus(), forced, implicit);
  }

  private ContentManager getContentManager(@NotNull Content content) {
    return myContentUI.getContentManager(content);
  }

  private boolean shouldRequestFocus() {
    final Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return focused != null && SwingUtilities.isDescendingFrom(focused, getContentManager().getComponent());
  }

  @Override
  public boolean removeContent(@Nullable Content content, final boolean dispose) {
    return content != null && getContentManager().removeContent(content, dispose);
  }

  @Override
  public boolean isToFocus(@NotNull final Content content, @NotNull final String condition) {
    final String id = content.getUserData(ViewImpl.ID);
    return getLayout().isToFocus(id, condition);
  }

  @NotNull
  @Override
  public LayoutViewOptions setToFocus(@Nullable final Content content, @NotNull final String condition) {
    getLayout().setToFocus(content != null ? content.getUserData(ViewImpl.ID) : null, condition);
    return this;
  }

  @Override
  public void attractBy(@NotNull final String condition) {
    myContentUI.attractByCondition(condition, true);
  }

  @Override
  public void clearAttractionBy(@NotNull final String condition) {
    myContentUI.clearAttractionByCondition(condition, true);
  }

  public void removeContent(@NotNull String id, final boolean dispose) {
    final Content content = findContent(id);
    if (content != null) {
      getContentManager().removeContent(content, dispose);
    }
  }

  @Override
  public AnAction getLayoutActions() {
    return myContentUI.getLayoutActions();
  }

  @NotNull
  @Override
  public AnAction[] getLayoutActionsList() {
    final ActionGroup group = (ActionGroup)getLayoutActions();
    return group.getChildren(null);
  }

  @NotNull
  @Override
  public LayoutViewOptions setTabPopupActions(@NotNull ActionGroup group) {
    myContentUI.setTabPopupActions(group);
    return this;
  }

  @NotNull
  @Override
  public LayoutViewOptions setLeftToolbar(@NotNull final ActionGroup leftToolbar, @NotNull final String place) {
    myContentUI.setLeftToolbar(leftToolbar, place);
    return this;
  }

  @Override
  @Nullable
  public Content findContent(@NotNull final String key) {
    return myContentUI.findContent(key);
  }

  @NotNull
  @Override
  public RunnerLayoutUi addListener(@NotNull final ContentManagerListener listener, @NotNull final Disposable parent) {
    final ContentManager mgr = getContentManager();
    mgr.addContentManagerListener(listener);
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        mgr.removeContentManagerListener(listener);
      }
    });
    return this;
  }

  @Override
  public void removeListener(@NotNull final ContentManagerListener listener) {
    getContentManager().removeContentManagerListener(listener);
  }

  @Override
  public void setBouncing(@NotNull final Content content, final boolean activate) {
    myContentUI.processBounce(content, activate);
  }


  @Override
  public boolean isDisposed() {
    return getContentManager().isDisposed();
  }

  @Override
  @NotNull
  public LayoutViewOptions setMinimizeActionEnabled(final boolean enabled) {
    myContentUI.setMinimizeActionEnabled(enabled);
    return this;
  }

  public LayoutViewOptions setToDisposeRemoveContent(boolean toDispose) {
    myContentUI.setToDisposeRemovedContent(toDispose);
    return this;
  }

  @Override
  @NotNull
  public LayoutViewOptions setMoveToGridActionEnabled(final boolean enabled) {
    myContentUI.setMovetoGridActionEnabled(enabled);
    return this;
  }

  @Override
  @NotNull
  public LayoutViewOptions setAttractionPolicy(@NotNull final String contentId, final LayoutAttractionPolicy policy) {
    myContentUI.setPolicy(contentId, policy);
    return this;
  }

  @NotNull
  @Override
  public LayoutViewOptions setConditionAttractionPolicy(@NotNull final String condition, final LayoutAttractionPolicy policy) {
    myContentUI.setConditionPolicy(condition, policy);
    return this;
  }

  @Override
  @NotNull
  public LayoutStateDefaults getDefaults() {
    return this;
  }

  @Override
  @NotNull
  public LayoutViewOptions getOptions() {
    return this;
  }

  @NotNull
  @Override
  public LayoutViewOptions setAdditionalFocusActions(@NotNull final ActionGroup group) {
    myContentUI.setAdditionalFocusActions(group);
    return this;
  }

  @Override
  public AnAction getSettingsActions() {
    return myContentUI.getSettingsActions();
  }

  @NotNull
  @Override
  public AnAction[] getSettingsActionsList() {
    final ActionGroup group = (ActionGroup)getSettingsActions();
    return group.getChildren(null);
  }

  @NotNull
  @Override
  public Content[] getContents() {
    Content[] contents = new Content[getContentManager().getContentCount()];
    for (int i = 0; i < contents.length; i++) {
      contents[i] = getContentManager().getContent(i);
    }
    return contents;
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (QuickActionProvider.KEY.is(dataId) || RunnerContentUi.KEY.is(dataId)) {
      return myContentUI;
    }
    return null;
  }

  public void setLeftToolbarVisible(boolean value) {
    myContentUI.setLeftToolbarVisible(value);
  }

  public void setContentToolbarBefore(boolean value) {
    myContentUI.setContentToolbarBefore(value);
  }

  public List<AnAction> getActions() {
    return myContentUI.getActions(true);
  }
}
