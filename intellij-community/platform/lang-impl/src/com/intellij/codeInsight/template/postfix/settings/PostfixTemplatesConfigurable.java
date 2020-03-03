// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.application.options.editor.EditorOptionsProvider;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.postfix.templates.LanguagePostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatesUtils;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.extensions.BaseExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.Alarm;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.List;
import java.util.*;

@SuppressWarnings("rawtypes")
public class PostfixTemplatesConfigurable implements SearchableConfigurable, EditorOptionsProvider, Configurable.NoScroll,
                                                     Configurable.WithEpDependencies {
  public static final Comparator<PostfixTemplate> TEMPLATE_COMPARATOR = Comparator.comparing(PostfixTemplate::getKey);

  @Nullable
  private PostfixTemplatesCheckboxTree myCheckboxTree;

  @NotNull
  private final PostfixTemplatesSettings myTemplatesSettings;

  @Nullable
  private PostfixDescriptionPanel myInnerPostfixDescriptionPanel;
  
  private JComponent myPanel;
  private JBCheckBox myCompletionEnabledCheckbox;
  private JBCheckBox myPostfixTemplatesEnabled;
  private JPanel myTemplatesTreeContainer;
  private ComboBox<String> myShortcutComboBox;
  private JPanel myDescriptionPanel;
  private final Alarm myUpdateDescriptionPanelAlarm = new Alarm();

  public PostfixTemplatesConfigurable() {
    myTemplatesSettings = PostfixTemplatesSettings.getInstance();

    myPostfixTemplatesEnabled.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        updateComponents();
      }
    });
    myShortcutComboBox.addItem(getTab());
    myShortcutComboBox.addItem(getSpace());
    myShortcutComboBox.addItem(getEnter());
    myDescriptionPanel.setLayout(new BorderLayout());
  }

  @NotNull
  @Override
  public Collection<BaseExtensionPointName<?>> getDependencies() {
    return Collections.singleton(LanguagePostfixTemplate.EP_NAME);
  }

  @NotNull
  private static List<PostfixTemplateProvider> getProviders() {
    List<LanguageExtensionPoint> list = LanguagePostfixTemplate.EP_NAME.getExtensionList();
    return ContainerUtil.map(list, el -> (PostfixTemplateProvider)el.getInstance());
  }

  private void createTree() {
    myCheckboxTree = new PostfixTemplatesCheckboxTree() {
      @Override
      protected void selectionChanged() {
        myUpdateDescriptionPanelAlarm.cancelAllRequests();
        myUpdateDescriptionPanelAlarm.addRequest(() -> resetDescriptionPanel(), 100);
      }
    };

    
    JPanel panel = new JPanel(new BorderLayout());
    boolean canAddTemplate = ContainerUtil.find(getProviders(), p -> StringUtil.isNotEmpty(p.getPresentableName())) != null;
    
    panel.add(ToolbarDecorator.createDecorator(myCheckboxTree)
                              .setAddActionUpdater(e -> canAddTemplate)
                              .setAddAction(button -> myCheckboxTree.addTemplate(button))
                              .setEditActionUpdater(e -> myCheckboxTree.canEditSelectedTemplate())
                              .setEditAction(button -> myCheckboxTree.editSelectedTemplate())
                              .setRemoveActionUpdater(e -> myCheckboxTree.canRemoveSelectedTemplates())
                              .setRemoveAction(button -> myCheckboxTree.removeSelectedTemplates())
                              .addExtraAction(duplicateAction())
                              .createPanel());

    myTemplatesTreeContainer.setLayout(new BorderLayout());
    myTemplatesTreeContainer.add(panel);
  }

  private AnActionButton duplicateAction() {
    AnActionButton button = new AnActionButton(CodeInsightBundle.messagePointer("action.AnActionButton.text.duplicate"), PlatformIcons.COPY_ICON) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        if (myCheckboxTree != null) {
          myCheckboxTree.duplicateSelectedTemplate();
        }
      }

      @Override
      public void updateButton(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(myCheckboxTree != null && myCheckboxTree.canDuplicateSelectedTemplate());
      }
    };
    button.registerCustomShortcutSet(CommonShortcuts.getDuplicate(), myCheckboxTree, myCheckboxTree);
    return button;
  }

  private void resetDescriptionPanel() {
    if (null != myCheckboxTree && null != myInnerPostfixDescriptionPanel) {
      myInnerPostfixDescriptionPanel.reset(PostfixTemplateMetaData.createMetaData(myCheckboxTree.getSelectedTemplate()));
      myInnerPostfixDescriptionPanel.resetHeights(myDescriptionPanel.getWidth());
    }
  }

  @NotNull
  @Override
  public String getId() {
    return "reference.settingsdialog.IDE.editor.postfix.templates";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return getId();
  }

  @Nls
  @Override
  public String getDisplayName() {
    return CodeInsightBundle.message("configurable.PostfixTemplatesConfigurable.display.name");
  }

  @Nullable
  public PostfixTemplatesCheckboxTree getTemplatesTree() {
    return myCheckboxTree;
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    GuiUtils.replaceJSplitPaneWithIDEASplitter(myPanel);
    if (null == myInnerPostfixDescriptionPanel) {
      myInnerPostfixDescriptionPanel = new PostfixDescriptionPanel();
      myDescriptionPanel.add(myInnerPostfixDescriptionPanel.getComponent());
    }
    if (null == myCheckboxTree) {
      createTree();
    }

    return myPanel;
  }

  @Override
  public void apply() {
    if (myCheckboxTree != null) {
      myTemplatesSettings.setProviderToDisabledTemplates(myCheckboxTree.getDisabledTemplatesState());
      myTemplatesSettings.setPostfixTemplatesEnabled(myPostfixTemplatesEnabled.isSelected());
      myTemplatesSettings.setTemplatesCompletionEnabled(myCompletionEnabledCheckbox.isSelected());
      myTemplatesSettings.setShortcut(stringToShortcut((String)myShortcutComboBox.getSelectedItem()));

      MultiMap<PostfixTemplateProvider, PostfixTemplate> state = myCheckboxTree.getEditableTemplates();
      for (PostfixTemplateProvider provider : getProviders()) {
        PostfixTemplateStorage.getInstance().setTemplates(provider, state.get(provider));
      }
    }
  }

  @Override
  public void reset() {
    if (myCheckboxTree != null) {
      MultiMap<PostfixTemplateProvider, PostfixTemplate> templatesMap = getProviderToTemplatesMap();

      myCheckboxTree.initTree(templatesMap);
      myCheckboxTree.setDisabledTemplatesState(myTemplatesSettings.getProviderToDisabledTemplates());
      myPostfixTemplatesEnabled.setSelected(myTemplatesSettings.isPostfixTemplatesEnabled());
      myCompletionEnabledCheckbox.setSelected(myTemplatesSettings.isTemplatesCompletionEnabled());
      myShortcutComboBox.setSelectedItem(shortcutToString((char)myTemplatesSettings.getShortcut()));
      resetDescriptionPanel();
      updateComponents();
    }
  }

  @NotNull
  private static MultiMap<PostfixTemplateProvider, PostfixTemplate> getProviderToTemplatesMap() {
    MultiMap<PostfixTemplateProvider, PostfixTemplate> templatesMap = MultiMap.create();

    for (LanguageExtensionPoint<?> extension : LanguagePostfixTemplate.EP_NAME.getExtensionList()) {
      PostfixTemplateProvider provider = (PostfixTemplateProvider)extension.getInstance();
      Set<PostfixTemplate> templates = PostfixTemplatesUtils.getAvailableTemplates(provider);
      if (!templates.isEmpty()) {
        templatesMap.putValues(provider, ContainerUtil.sorted(templates, TEMPLATE_COMPARATOR));
      }
    }
    return templatesMap;
  }

  @Override
  public boolean isModified() {
    if (myCheckboxTree == null) {
      return false;
    }
    if (myPostfixTemplatesEnabled.isSelected() != myTemplatesSettings.isPostfixTemplatesEnabled() ||
        myCompletionEnabledCheckbox.isSelected() != myTemplatesSettings.isTemplatesCompletionEnabled() ||
        stringToShortcut((String)myShortcutComboBox.getSelectedItem()) != myTemplatesSettings.getShortcut() ||
        !myCheckboxTree.getDisabledTemplatesState().equals(myTemplatesSettings.getProviderToDisabledTemplates())) {
      return true;
    }

    MultiMap<PostfixTemplateProvider, PostfixTemplate> state = myCheckboxTree.getEditableTemplates();
    for (PostfixTemplateProvider provider : getProviders()) {
      if (!PostfixTemplateStorage.getInstance().getTemplates(provider).equals(state.get(provider))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void disposeUIResources() {
    if (myInnerPostfixDescriptionPanel != null) {
      Disposer.dispose(myInnerPostfixDescriptionPanel);
    }
    if (myCheckboxTree != null) {
      Disposer.dispose(myCheckboxTree);
      myCheckboxTree = null;
    }
    Disposer.dispose(myUpdateDescriptionPanelAlarm);
  }

  private void updateComponents() {
    boolean pluginEnabled = myPostfixTemplatesEnabled.isSelected();
    myCompletionEnabledCheckbox.setVisible(!LiveTemplateCompletionContributor.shouldShowAllTemplates());
    myCompletionEnabledCheckbox.setEnabled(pluginEnabled);
    myShortcutComboBox.setEnabled(pluginEnabled);
    if (myCheckboxTree != null) {
      myCheckboxTree.setEnabled(pluginEnabled);
    }
  }

  private static char stringToShortcut(@Nullable String string) {
    if (getSpace().equals(string)) {
      return TemplateSettings.SPACE_CHAR;
    }
    else if (getEnter().equals(string)) {
      return TemplateSettings.ENTER_CHAR;
    }
    return TemplateSettings.TAB_CHAR;
  }

  private static String shortcutToString(char shortcut) {
    if (shortcut == TemplateSettings.SPACE_CHAR) {
      return getSpace();
    }
    if (shortcut == TemplateSettings.ENTER_CHAR) {
      return getEnter();
    }
    return getTab();
  }

  private static String getSpace() {
    return CodeInsightBundle.message("template.shortcut.space");
  }

  private static String getTab() {
    return CodeInsightBundle.message("template.shortcut.tab");
  }

  private static String getEnter() {
    return CodeInsightBundle.message("template.shortcut.enter");
  }
}
