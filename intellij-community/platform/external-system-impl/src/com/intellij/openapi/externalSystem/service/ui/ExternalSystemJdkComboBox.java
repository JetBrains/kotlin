// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkException;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Consumer;
import com.intellij.util.EnvironmentUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Sergey Evdokimov
 */
public class ExternalSystemJdkComboBox extends ComboBoxWithWidePopup<ExternalSystemJdkComboBox.JdkComboBoxItem> {

  private static final int MAX_PATH_LENGTH = 50;

  @Nullable
  private Project myProject;
  private @Nullable Sdk myProjectJdk;
  private boolean suggestJre = true;

  public ExternalSystemJdkComboBox() {
    this(null);
  }

  public ExternalSystemJdkComboBox(@Nullable Project project) {
    myProject = project;
    setRenderer(new ColoredListCellRenderer<JdkComboBoxItem>() {

      @Override
      protected void customizeCellRenderer(@NotNull JList list, JdkComboBoxItem value, int index, boolean selected, boolean hasFocus) {
        if (value == null) return;
        CompositeAppearance appearance = new CompositeAppearance();
        appearance.setIcon(AllIcons.Nodes.PpJdk);
        SimpleTextAttributes attributes = getTextAttributes(value.valid, selected);
        CompositeAppearance.DequeEnd ending = appearance.getEnding();

        ending.addText(value.label, attributes);
        if (value.comment != null && !value.comment.equals(value.jdkName)) {
          final SimpleTextAttributes textAttributes;
          if (!value.valid) {
            textAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES;
          }
          else {
            textAttributes = SystemInfo.isMac && selected
                             ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.WHITE)
                             : SimpleTextAttributes.GRAY_ATTRIBUTES;
          }

          ending.addComment(value.comment, textAttributes);
        }

        final CompositeAppearance compositeAppearance = ending.getAppearance();
        compositeAppearance.customize(this);
      }
    });
  }

  @Nullable
  public Project getProject() {
    return myProject;
  }

  public void setProject(@Nullable Project project) {
    myProject = project;
  }

  public void setProjectJdk(@Nullable Sdk projectJdk) {
    myProjectJdk = projectJdk;
  }

  public void setSetupButton(@NotNull JButton setUpButton,
                             @NotNull ProjectSdksModel jdksModel,
                             @Nullable String actionGroupTitle,
                             @Nullable Condition<? super SdkTypeId> creationFilter) {
    setSetupButton(setUpButton, jdksModel, actionGroupTitle, creationFilter, null);
  }

  public void setSetupButton(@NotNull JButton setUpButton,
                             @NotNull ProjectSdksModel jdksModel,
                             @Nullable String actionGroupTitle,
                             @Nullable Condition<? super SdkTypeId> creationFilter,
                             @Nullable WizardContext wizardContext) {
    Arrays.stream(setUpButton.getActionListeners()).forEach(setUpButton::removeActionListener);

    setUpButton.addActionListener(e -> {
      DefaultActionGroup group = new DefaultActionGroup();
      Sdk selectedJdk = getSelectedJdk();
      Consumer<Sdk> updateTree = jdk -> {
        Sdk existingJdk = Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
                                .filter(sdk -> StringUtil.equals(sdk.getHomePath(), jdk.getHomePath()))
                                .findFirst().orElse(null);

        String jdkName;
        if (existingJdk == null) {
          SdkConfigurationUtil.addSdk(jdk);
          jdkName = jdk.getName();
        }
        else {
          jdkName = existingJdk.getName();
        }
        refreshData(jdkName, wizardContext != null ? wizardContext.getProjectJdk() : null);
      };
      jdksModel.reset(getProject());
      jdksModel.createAddActions(group, this, selectedJdk, updateTree, creationFilter);

      if (group.getChildrenCount() == 0) {
        SimpleJavaSdkType javaSdkType = SimpleJavaSdkType.getInstance();
        final AnAction addAction = new DumbAwareAction(javaSdkType.getPresentableName(), null, javaSdkType.getIconForAddAction()) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            jdksModel.doAdd(ExternalSystemJdkComboBox.this, selectedJdk, javaSdkType, updateTree);
          }
        };
        group.add(addAction);
      }

      final DataContext dataContext = DataManager.getInstance().getDataContext(this);
      if (group.getChildrenCount() > 1) {
        JBPopupFactory.getInstance()
          .createActionGroupPopup(actionGroupTitle, group, dataContext, JBPopupFactory.ActionSelectionAid.MNEMONICS, false)
          .showUnderneathOf(setUpButton);
      }
      else if (group.getChildrenCount() == 1) {
        final AnActionEvent event =
          new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, new Presentation(""), ActionManager.getInstance(), 0);
        group.getChildren(event)[0].actionPerformed(event);
      }
    });
  }

  @Nullable
  public Sdk getSelectedJdk() {
    String jdkName = getSelectedValue();
    Sdk jdk = null;
    try {
      jdk = ExternalSystemJdkUtil.resolveJdkName(myProjectJdk, jdkName);
    }
    catch (ExternalSystemJdkException ignore) {
    }
    return jdk;
  }

  @NotNull
  public ExternalSystemJdkComboBox withoutJre() {
    suggestJre = false;
    return this;
  }

  public void refreshData(@Nullable String selectedValue) {
    refreshData(selectedValue, null);
  }

  public void refreshData(@Nullable String selectedValue, @Nullable Sdk projectJdk) {
    myProjectJdk = projectJdk;
    Map<String, JdkComboBoxItem> jdkMap = collectComboBoxItem();
    if (selectedValue != null && !jdkMap.containsKey(selectedValue)) {
      assert selectedValue.length() > 0;
      jdkMap.put(selectedValue, new JdkComboBoxItem(selectedValue, selectedValue, "", false));
    }

    removeAllItems();

    ComboBoxModel<JdkComboBoxItem> comboBoxModel = getModel();
    for (Map.Entry<String, JdkComboBoxItem> entry : jdkMap.entrySet()) {
      ((MutableComboBoxModel<JdkComboBoxItem>)comboBoxModel).addElement(entry.getValue());
    }

    select(comboBoxModel, selectedValue);
  }

  @ApiStatus.Experimental
  public static void select(@NotNull ComboBoxModel<? extends JdkComboBoxItem> model, @Nullable String value) {
    for (int i = 0; i < model.getSize(); i++) {
      JdkComboBoxItem item = model.getElementAt(i);
      if (item.jdkName.equals(value)) {
        model.setSelectedItem(item);
        return;
      }
    }
    if (model.getSize() != 0) {
      model.setSelectedItem(model.getElementAt(0));
    }
  }

  @Nullable
  public String getSelectedValue() {
    final DefaultComboBoxModel model = (DefaultComboBoxModel)getModel();
    final Object item = model.getSelectedItem();
    return item != null ? ((JdkComboBoxItem)item).jdkName : null;
  }

  private Map<String, JdkComboBoxItem> collectComboBoxItem() {
    Map<String, JdkComboBoxItem> result = new LinkedHashMap<>();
    for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
      SdkTypeId sdkType = sdk.getSdkType();
      if (!(sdkType instanceof JavaSdkType && sdkType instanceof SdkType)) continue;
      if (!((SdkType)sdkType).sdkHasValidPath(sdk)) {
        continue;
      }
      String name = sdk.getName();
      String comment = buildComment(sdk);
      result.put(name, new JdkComboBoxItem(name, name, comment, true));
    }

    if (suggestJre) {
      final Sdk internalJdk = ExternalSystemJdkUtil.getJdk(null, ExternalSystemJdkUtil.USE_INTERNAL_JAVA);
      assert internalJdk != null;
      assert internalJdk.getHomePath() != null;
      result.put(ExternalSystemJdkUtil.USE_INTERNAL_JAVA,
                 new JdkComboBoxItem(
                   ExternalSystemJdkUtil.USE_INTERNAL_JAVA,
                   ExternalSystemBundle.message("external.system.java.internal.jre"),
                   buildComment(internalJdk),
                   true
                 ));
    }

    if (myProjectJdk == null) {
      if (myProject != null && !myProject.isDisposed()) {
        myProjectJdk = ProjectRootManager.getInstance(myProject).getProjectSdk();
      }
    }

    if (myProjectJdk != null) {
      result.put(ExternalSystemJdkUtil.USE_PROJECT_JDK,
                 new JdkComboBoxItem(ExternalSystemJdkUtil.USE_PROJECT_JDK, "Use Project JDK", buildComment(myProjectJdk), true));
    }

    String javaHomePath = EnvironmentUtil.getEnvironmentMap().get("JAVA_HOME");
    if (ExternalSystemJdkUtil.isValidJdk(javaHomePath)) {
      result.put(ExternalSystemJdkUtil.USE_JAVA_HOME,
                 new JdkComboBoxItem(
                   ExternalSystemJdkUtil.USE_JAVA_HOME, ExternalSystemBundle.message("external.system.java.home.env"),
                   truncateLongPath(javaHomePath), true
                 ));
    }
    return result;
  }

  private static String buildComment(@NotNull Sdk sdk) {
    String versionString = sdk.getVersionString();
    String path = sdk.getHomePath();
    StringBuilder buf = new StringBuilder();
    if (versionString != null) {
      buf.append(versionString);
    }
    if (path != null) {
      buf.append(versionString != null ? ", " : "");
      buf.append("path: ").append(truncateLongPath(path));
    }

    return buf.toString();
  }

  @NotNull
  private static String truncateLongPath(@NotNull String path) {
    if (path.length() > MAX_PATH_LENGTH) {
      return path.substring(0, MAX_PATH_LENGTH / 2) + "..." + path.substring(path.length() - MAX_PATH_LENGTH / 2 - 3);
    }

    return path;
  }

  private static SimpleTextAttributes getTextAttributes(final boolean valid, final boolean selected) {
    if (!valid) {
      return SimpleTextAttributes.ERROR_ATTRIBUTES;
    }
    else if (selected && !(SystemInfo.isWinVistaOrNewer && UIManager.getLookAndFeel().getName().contains("Windows"))) {
      return SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES;
    }
    else {
      return SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES;
    }
  }

  static class JdkComboBoxItem {
    private final String jdkName;
    private final String label;
    private final String comment;
    private final boolean valid;

    JdkComboBoxItem(String jdkName, String label, String comment, boolean valid) {
      this.jdkName = jdkName;
      this.label = label;
      this.comment = comment;
      this.valid = valid;
    }
  }
}
