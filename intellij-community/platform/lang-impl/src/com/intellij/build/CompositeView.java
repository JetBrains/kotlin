// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.ComponentContainer;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class CompositeView<T extends ComponentContainer> extends JPanel implements ComponentContainer, DataProvider {
  private final Map<String, T> myViewMap = ContainerUtil.newConcurrentMap();
  private final String mySelectionStateKey;
  private final AtomicReference<String> myVisibleViewRef = new AtomicReference<>();
  @NotNull
  private final SwitchViewAction mySwitchViewAction;

  public CompositeView(String selectionStateKey) {
    super(new CardLayout());
    mySelectionStateKey = selectionStateKey;
    mySwitchViewAction = new SwitchViewAction();
  }

  public void addView(@NotNull T view, @NotNull String viewName) {
    T oldView = getView(viewName);
    if (oldView != null) {
      remove(oldView.getComponent());
      Disposer.dispose(oldView);
    }
    myViewMap.put(viewName, view);
    add(view.getComponent(), viewName);
    Disposer.register(this, view);
  }

  public void addViewAndShowIfNeeded(@NotNull T view, @NotNull String viewName, boolean showByDefault) {
    addView(view, viewName);
    String storedState = getStoredState();
    if (storedState != null && (storedState.equals(viewName)) ||
        storedState == null && showByDefault) {
      showView(viewName);
    }
  }

  public void showView(@NotNull String viewName) {
    showView(viewName, true);
    setStoredState(viewName);
  }

  public void showView(@NotNull String viewName, boolean requestFocus) {
    if (!StringUtil.equals(viewName, myVisibleViewRef.get())) {
      myVisibleViewRef.set(viewName);
      CardLayout cl = (CardLayout)(getLayout());
      cl.show(this, viewName);
    }
    if (requestFocus) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
        ComponentContainer view = getView(viewName);
        if (view != null) {
          IdeFocusManager.getGlobalInstance().requestFocus(view.getPreferredFocusableComponent(), true);
        }
      });
    }
  }

  public boolean isViewVisible(String viewName) {
    return StringUtil.equals(myVisibleViewRef.get(), viewName);
  }

  public T getView(@NotNull String viewName) {
    return myViewMap.get(viewName);
  }

  @Nullable
  public <U> U getView(@NotNull String viewName, @NotNull Class<U> viewClass) {
    T view = getView(viewName);
    return viewClass.isInstance(view) ? viewClass.cast(view) : null;
  }

  @NotNull
  public AnAction[] createConsoleActions() {
    return AnAction.EMPTY_ARRAY;
  }

  @NotNull
  public AnAction[] getSwitchActions() {
    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.addSeparator();
    actionGroup.add(mySwitchViewAction);
    return new AnAction[]{actionGroup};
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return this;
  }

  @Override
  public void dispose() {
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    String visibleViewName = myVisibleViewRef.get();
    if (visibleViewName != null) {
      T visibleView = getView(visibleViewName);
      if (visibleView instanceof DataProvider) {
        Object data = ((DataProvider)visibleView).getData(dataId);
        if (data != null) return data;
      }
    }
    return null;
  }

  private void setStoredState(String viewName) {
    if (mySelectionStateKey != null) {
      PropertiesComponent.getInstance().setValue(mySelectionStateKey, viewName);
    }
  }

  @Nullable
  private String getStoredState() {
    return mySelectionStateKey == null ? null : PropertiesComponent.getInstance().getValue(mySelectionStateKey);
  }

  private class SwitchViewAction extends ToggleAction implements DumbAware {
    SwitchViewAction() {
      super("Toggle view", null,
            AllIcons.Actions.ChangeView);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      if (myViewMap.size() <= 1) {
        presentation.setEnabledAndVisible(false);
      }
      else {
        presentation.setEnabledAndVisible(true);
        Toggleable.setSelected(presentation, isSelected(e));
      }
    }

    @Override
    public boolean isSelected(@NotNull final AnActionEvent event) {
      String visibleViewName = myVisibleViewRef.get();
      if (visibleViewName == null) return true;
      Set<String> viewNames = myViewMap.keySet();
      return viewNames.isEmpty() || visibleViewName.equals(viewNames.iterator().next());
    }

    @Override
    public void setSelected(@NotNull final AnActionEvent event, final boolean flag) {
      if (myViewMap.size() > 1) {
        List<String> names = new ArrayList<>(myViewMap.keySet());
        String viewName = flag ? names.get(0) : names.get(1);
        showView(viewName);
        ApplicationManager.getApplication().invokeLater(() -> update(event));
      }
    }
  }
}
