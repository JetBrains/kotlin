// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.searcheverywhere.ActionSearchEverywhereContributor;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.GotoActionItemProvider;
import com.intellij.ide.util.gotoByName.GotoActionModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.ActionMenu;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.impl.ActionShortcutRestrictions;
import com.intellij.openapi.keymap.impl.ui.KeymapPanel;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.annotations.JdkConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.Set;

import static com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

public class GotoActionAction extends GotoActionBase implements DumbAware {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (Registry.is("new.search.everywhere") && e.getProject() != null) {
      showInSearchEverywherePopup(ActionSearchEverywhereContributor.class.getSimpleName(), e, false, true);
    } else {
      super.actionPerformed(e);
    }
  }

  @Override
  public void gotoActionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    Editor editor = e.getData(CommonDataKeys.EDITOR);

    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.action");
    GotoActionModel model = new GotoActionModel(project, component, editor);
    GotoActionCallback<Object> callback = new GotoActionCallback<Object>() {
      @Override
      public void elementChosen(@NotNull ChooseByNamePopup popup, @NotNull Object element) {
        if (project != null) {
          // if the chosen action displays another popup, don't populate it automatically with the text from this popup
          project.putUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, null);
        }
        String enteredText = popup.getTrimmedText();
        int modifiers = popup.isClosedByShiftEnter() ? InputEvent.SHIFT_MASK : 0;
        openOptionOrPerformAction(((GotoActionModel.MatchedValue)element).value, enteredText, project, component, modifiers);
      }
    };

    Pair<String, Integer> start = getInitialText(false, e);
    showNavigationPopup(callback, null, createPopup(project, model, start.first, start.second, component, e), false);
  }

  @NotNull
  private static ChooseByNamePopup createPopup(@Nullable Project project,
                                               @NotNull GotoActionModel model,
                                               String initialText,
                                               int initialIndex,
                                               Component component,
                                               AnActionEvent event) {
    ChooseByNamePopup oldPopup = project == null ? null : project.getUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY);
    if (oldPopup != null) {
      oldPopup.close(false);
    }
    Disposable disposable = Disposer.newDisposable();
    ShortcutSet altEnterShortcutSet = getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
    KeymapManager km = KeymapManager.getInstance();
    Keymap activeKeymap = km != null ? km.getActiveKeymap() : null;
    ChooseByNamePopup popup = new ChooseByNamePopup(project, model, new GotoActionItemProvider(model), oldPopup, initialText, false, initialIndex) {
      @Override
      protected void initUI(Callback callback, ModalityState modalityState, boolean allowMultipleSelection) {
        super.initUI(callback, modalityState, allowMultipleSelection);
        myList.addListSelectionListener(new ListSelectionListener() {
          @Override
          public void valueChanged(ListSelectionEvent e) {
            Object value = myList.getSelectedValue();
            String text = getText(value);
            if (text != null && myDropdownPopup != null) {
              myDropdownPopup.setAdText(text, SwingConstants.LEFT);
            }

            String description = getValueDescription(value);
            ActionMenu.showDescriptionInStatusBar(true, myList, description);
          }

          @Nullable
          private String getText(@Nullable Object o) {
            if (o instanceof GotoActionModel.MatchedValue) {
              GotoActionModel.MatchedValue mv = (GotoActionModel.MatchedValue)o;

              if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
                if (mv.value instanceof GotoActionModel.ActionWrapper) {
                  AnAction action = ((GotoActionModel.ActionWrapper)mv.value).getAction();
                  String actionId = ActionManager.getInstance().getId(action);
                  return StringUtil.notNullize(actionId, "class: " + action.getClass().getName());
                }
              }

              if (mv.value instanceof BooleanOptionDescription ||
                  mv.value instanceof GotoActionModel.ActionWrapper && ((GotoActionModel.ActionWrapper)mv.value).getAction() instanceof ToggleAction) {
                return "Press " + KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)) + " to toggle option";
              }

              if (altEnterShortcutSet.getShortcuts().length > 0 && mv.value instanceof GotoActionModel.ActionWrapper && activeKeymap != null) {
                GotoActionModel.ActionWrapper aw = (GotoActionModel.ActionWrapper)mv.value;
                if (aw.isAvailable()) {
                  String actionId = ActionManager.getInstance().getId(aw.getAction());
                  boolean actionWithoutShortcuts = activeKeymap.getShortcuts(actionId).length == 0;
                  if (actionWithoutShortcuts && new Random().nextInt(2) > 0) {
                    String altEnter = KeymapUtil.getFirstKeyboardShortcutText(altEnterShortcutSet);
                    return "Press " + altEnter + " to assign a shortcut for the selected action";
                  }
                }
              }
            }
            return getAdText();
          }
        });
        myList.addMouseMotionListener(new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            int index = myList.locationToIndex(e.getPoint());
            if (index == -1) return;
            Object value = myList.getModel().getElementAt(index);
            String description = getValueDescription(value);
            ActionMenu.showDescriptionInStatusBar(true, myList, description);
          }
        });
      }

      @Nullable
      private String getValueDescription(@Nullable Object value) {
        if (value instanceof GotoActionModel.MatchedValue) {
          GotoActionModel.MatchedValue mv = (GotoActionModel.MatchedValue)value;
          if (mv.value instanceof GotoActionModel.ActionWrapper) {
            AnAction action = ((GotoActionModel.ActionWrapper)mv.value).getAction();
            return action.getTemplatePresentation().getDescription();
          }
        }
        return null;
      }

      @NotNull
      @Override
      protected Set<Object> filter(@NotNull Set<Object> elements) {
        return super.filter(model.sortItems(elements));
      }

      @Override
      protected boolean closeForbidden(boolean ok) {
        if (!ok) return false;
        Object element = getChosenElement();
        return element instanceof GotoActionModel.MatchedValue &&
               processOptionInplace(((GotoActionModel.MatchedValue)element).value, this, component, event) ||
               super.closeForbidden(true);
      }

      @Override
      public void setDisposed(boolean disposedFlag) {
        super.setDisposed(disposedFlag);
        Disposer.dispose(disposable);

        ActionMenu.showDescriptionInStatusBar(true, myList, null);

        for (ListSelectionListener listener : myList.getListSelectionListeners()) {
          myList.removeListSelectionListener(listener);
        }
        UIUtil.dispose(myList);
      }
    };

    ApplicationManager.getApplication().getMessageBus().connect(disposable).subscribe(ProgressWindow.TOPIC, pw -> Disposer.register(pw, new Disposable() {
      @Override
      public void dispose() {
        if (!popup.checkDisposed()) {
          popup.repaintList();
        }
      }
    }));

    if (project != null) {
      project.putUserData(ChooseByNamePopup.CHOOSE_BY_NAME_POPUP_IN_PROJECT_KEY, popup);
    }

    popup.addMouseClickListener(new MouseAdapter() {
      @Override
      public void mouseClicked(@NotNull MouseEvent me) {
        Object element = popup.getSelectionByPoint(me.getPoint());
        if (element instanceof GotoActionModel.MatchedValue &&
            processOptionInplace(((GotoActionModel.MatchedValue)element).value, popup, component, event)) {
          me.consume();
        }
      }
    });

    DumbAwareAction.create(e -> {
      Object o = popup.getChosenElement();
      if (o instanceof GotoActionModel.MatchedValue && activeKeymap != null) {
        Object value = ((GotoActionModel.MatchedValue)o).value;
        if (value instanceof GotoActionModel.ActionWrapper) {
          GotoActionModel.ActionWrapper aw = (GotoActionModel.ActionWrapper)value;
          if (aw.isAvailable()) {
            String id = ActionManager.getInstance().getId(aw.getAction());
            KeymapPanel.addKeyboardShortcut(id, ActionShortcutRestrictions.getInstance().getForActionId(id), activeKeymap, component);
          }
        }
      }
    }).registerCustomShortcutSet(altEnterShortcutSet, popup.getTextField(), disposable);

    return popup;
  }

  private static boolean processOptionInplace(Object value, ChooseByNamePopup popup, Component component, AnActionEvent e) {
    if (value instanceof BooleanOptionDescription) {
      BooleanOptionDescription option = (BooleanOptionDescription)value;
      option.setOptionState(!option.isOptionEnabled());
      repaint(popup);
      return true;
    }
    else if (value instanceof GotoActionModel.ActionWrapper) {
      AnAction action = ((GotoActionModel.ActionWrapper)value).getAction();
      if (action instanceof ToggleAction) {
        performAction(action, component, e, 0, () -> repaint(popup));
        return true;
      }
    }
    return false;
  }

  private static void repaint(@Nullable ChooseByNamePopup popup) {
    if (popup != null) {
      popup.repaintListImmediate();
    }
  }

  public static void openOptionOrPerformAction(@NotNull Object element, String enteredText, @Nullable Project project, Component component) {
    openOptionOrPerformAction(element, enteredText, project, component, 0);
  }

  private static void openOptionOrPerformAction(Object element,
                                                String enteredText,
                                                @Nullable Project project,
                                                Component component,
                                                @JdkConstants.InputEventMask int modifiers) {
    if (element instanceof OptionDescription) {
      OptionDescription optionDescription = (OptionDescription)element;
      String configurableId = optionDescription.getConfigurableId();
      Disposable disposable = project != null ? project : ApplicationManager.getApplication();
      TransactionGuard guard = TransactionGuard.getInstance();
      if (optionDescription.hasExternalEditor()) {
        guard.submitTransactionLater(disposable, () -> optionDescription.invokeInternalEditor());
      }
      else {
        guard.submitTransactionLater(disposable, () -> ShowSettingsUtilImpl.showSettingsDialog(project, configurableId, enteredText));
      }
    }
    else {
      ApplicationManager.getApplication().invokeLater(
        () -> IdeFocusManager.getInstance(project).doWhenFocusSettlesDown(
          () -> performAction(element, component, null, modifiers, null)));
    }
  }

  public static void performAction(@NotNull Object element, @Nullable Component component, @Nullable AnActionEvent e) {
    performAction(element, component, e, 0, null);
  }

  private static void performAction(Object element,
                                    @Nullable Component component,
                                    @Nullable AnActionEvent e,
                                    @JdkConstants.InputEventMask int modifiers,
                                    @Nullable Runnable callback) {
    // element could be AnAction (SearchEverywhere)
    if (component == null) return;
    AnAction action = element instanceof AnAction ? (AnAction)element : ((GotoActionModel.ActionWrapper)element).getAction();
    TransactionGuard.getInstance().submitTransactionLater(ApplicationManager.getApplication(), () -> {
        DataManager instance = DataManager.getInstance();
        DataContext context = instance != null ? instance.getDataContext(component) : DataContext.EMPTY_CONTEXT;
        InputEvent inputEvent = e != null ? e.getInputEvent() : null;
        AnActionEvent event = AnActionEvent.createFromAnAction(action, inputEvent, ActionPlaces.ACTION_SEARCH, context);
        if (inputEvent == null && modifiers != 0) {
          event = new AnActionEvent(null, event.getDataContext(), event.getPlace(), event.getPresentation(), event.getActionManager(), modifiers);
        }

        if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
          if (action instanceof ActionGroup && !((ActionGroup)action).canBePerformed(context)) {
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
              event.getPresentation().getText(), (ActionGroup)action, context, false, callback, -1);
            Window window = SwingUtilities.getWindowAncestor(component);
            if (window != null) {
              popup.showInCenterOf(window);
            }
            else {
              popup.showInFocusCenter();
            }
          }
          else {
            ActionManagerEx manager = ActionManagerEx.getInstanceEx();
            manager.fireBeforeActionPerformed(action, context, event);
            ActionUtil.performActionDumbAware(action, event);
            if (callback != null) callback.run();
            manager.fireAfterActionPerformed(action, context, event);
          }
        }
    });
  }

  @Override
  protected boolean requiresProject() {
    return false;
  }
}