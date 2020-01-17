// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.actionSystem.impl.Utils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.MnemonicNavigationFilter;
import com.intellij.psi.PsiElement;
import com.intellij.ui.ErrorLabel;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.PopupListElementRenderer;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

import static com.intellij.util.ui.UIUtil.DEFAULT_HGAP;

public class CopyReferencePopup extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(CopyReferencePopup.class);
  private static final String COPY_REFERENCE_POPUP_PLACE = "CopyReferencePopupPlace";
  private static final int DEFAULT_WIDTH = JBUIScale.scale(500);
  private static final PresentationFactory PRESENTATION_FACTORY = new PresentationFactory();

  @Override
  public void update(@NotNull AnActionEvent e) {
    ActionGroup actionGroup = getCopyReferenceActionGroup();
    if (actionGroup == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    List<AnAction> actions = Utils.expandActionGroup(false, actionGroup, PRESENTATION_FACTORY, e.getDataContext(), e.getPlace());
    e.getPresentation().setEnabledAndVisible(CopyPathsAction.isCopyReferencePopupAvailable() && !actions.isEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ActionGroup actionGroup = getCopyReferenceActionGroup();
    if (actionGroup == null) return;

    DataContext dataContext = cloneDataContext(e);
    ListPopup popup =
      new PopupFactoryImpl.ActionGroupPopup("Copy", actionGroup, e.getDataContext(), true, true, false, true, null, -1, null,
                                            COPY_REFERENCE_POPUP_PLACE) {
      @Override
      protected ListCellRenderer<PopupFactoryImpl.ActionItem> getListElementRenderer() {
        return new PopupListElementRenderer<PopupFactoryImpl.ActionItem>(this) {
          private JLabel myInfoLabel;
          private JLabel myShortcutLabel;

          @Override
          protected JComponent createItemComponent() {
            myTextLabel = new ErrorLabel();
            myTextLabel.setBorder(JBUI.Borders.empty(1));

            myInfoLabel = new JLabel();
            myInfoLabel.setBorder(JBUI.Borders.empty(1, DEFAULT_HGAP, 1, 1));

            myShortcutLabel = new JLabel();
            myShortcutLabel.setBorder(JBUI.Borders.emptyLeft(DEFAULT_HGAP));
            myShortcutLabel.setForeground(UIUtil.getContextHelpForeground());

            JPanel textPanel = new JPanel(new BorderLayout());
            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.add(myTextLabel, BorderLayout.WEST);
            titlePanel.add(myShortcutLabel, BorderLayout.CENTER);

            textPanel.add(titlePanel, BorderLayout.WEST);
            textPanel.add(myInfoLabel, BorderLayout.CENTER);
            return layoutComponent(textPanel);
          }

          @Override
          protected void customizeComponent(@NotNull JList<? extends PopupFactoryImpl.ActionItem> list,
                                            @NotNull PopupFactoryImpl.ActionItem actionItem,
                                            boolean isSelected) {
            AnAction action = actionItem.getAction();
            AnActionEvent event = new AnActionEvent(e.getInputEvent(),
                                                    dataContext,
                                                    COPY_REFERENCE_POPUP_PLACE,
                                                    action.getTemplatePresentation().clone(),
                                                    e.getActionManager(),
                                                    e.getModifiers());

            ActionUtil.performDumbAwareUpdate(true, action, event, false);

            Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
            java.util.List<PsiElement> elements = CopyReferenceUtil.getElementsToCopy(editor, dataContext);
            String qualifiedName = null;
            if (action instanceof CopyPathProvider) {
              qualifiedName = ((CopyPathProvider)action).getQualifiedName(getProject(), elements, editor, dataContext);
            }

            if (qualifiedName != null) {
              myInfoLabel.setText(qualifiedName);
            }
            Color foreground = isSelected ? UIUtil.getListSelectionForeground(true) : UIUtil.getInactiveTextColor();
            myInfoLabel.setForeground(foreground);
            myShortcutLabel.setForeground(foreground);

            MnemonicNavigationFilter<Object> filter = myStep.getMnemonicNavigationFilter();
            int pos = filter == null ? -1 : filter.getMnemonicPos(actionItem);
            if (pos != -1) {
              String text = myTextLabel.getText();
              text = text.substring(0, pos) + text.substring(pos + 1);
              myTextLabel.setText(text);
              myTextLabel.setDisplayedMnemonicIndex(pos);
            }

            if (action instanceof CopyPathProvider) {
              Shortcut shortcut = ArrayUtil.getFirstElement(action.getShortcutSet().getShortcuts());
              myShortcutLabel.setText(shortcut != null ? KeymapUtil.getShortcutText(shortcut) : null);
            }
          }
        };
      }

      @Override
      protected boolean isResizable() {
        return true;
      }
    };

    updatePopupSize(popup);

    popup.showInBestPositionFor(e.getDataContext());
  }

  @Nullable
  public ActionGroup getCopyReferenceActionGroup() {
    AnAction popupGroup = ActionManager.getInstance().getAction("CopyReferencePopupGroup");
    if (!(popupGroup instanceof DefaultActionGroup)) {
      LOG.warn("Cannot find 'CopyReferencePopup' action to show popup");
      return null;
    }
    return (ActionGroup)popupGroup;
  }

  private static void updatePopupSize(@NotNull ListPopup popup) {
    ApplicationManager.getApplication().invokeLater(() -> {
      popup.getContent().setPreferredSize(new Dimension(DEFAULT_WIDTH, popup.getContent().getPreferredSize().height));
      popup.getContent().setSize(new Dimension(DEFAULT_WIDTH, popup.getContent().getPreferredSize().height));
      popup.setSize(popup.getContent().getPreferredSize());
    });
  }

  @NotNull
  private static DataContext cloneDataContext(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    HashMap<String, Object> map = new HashMap<>();
    map.put(CommonDataKeys.PSI_ELEMENT.getName(), CommonDataKeys.PSI_ELEMENT.getData(dataContext));
    map.put(CommonDataKeys.PROJECT.getName(), CommonDataKeys.PROJECT.getData(dataContext));
    map.put(LangDataKeys.PSI_ELEMENT_ARRAY.getName(), LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext));
    map.put(CommonDataKeys.VIRTUAL_FILE_ARRAY.getName(), CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext));
    map.put(CommonDataKeys.EDITOR.getName(), CommonDataKeys.EDITOR.getData(dataContext));

    return SimpleDataContext.getSimpleContext(map, DataContext.EMPTY_CONTEXT);
  }
}