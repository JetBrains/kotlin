// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.settings.LocationSettingType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemSettingsControlCustomizer;
import com.intellij.openapi.externalSystem.service.ui.ExternalSystemJdkComboBox;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.Alarm;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import com.intellij.xml.util.XmlStringUtil;
import one.util.streamex.StreamEx;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.GradleInstallationManager;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.TestRunner;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK;
import static com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil.INSETS;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("FieldCanBeLocal") // Used implicitly by reflection at disposeUIResources() and showUi()
public class IdeaGradleProjectSettingsControlBuilder implements GradleProjectSettingsControlBuilder {
  private static final Logger LOG = Logger.getInstance("#" + IdeaGradleProjectSettingsControlBuilder.class.getPackage().getName());

  private static final long BALLOON_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
  private static final String HIDDEN_KEY = "hidden";
  @NotNull
  private final GradleInstallationManager myInstallationManager;
  @NotNull
  private final GradleProjectSettings myInitialSettings;
  @NotNull
  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  @NotNull
  private LocationSettingType myGradleHomeSettingType = LocationSettingType.UNKNOWN;
  private boolean myShowBalloonIfNecessary;

  private boolean dropUseAutoImportBox;

  @Nullable
  private TextFieldWithBrowseButton myGradleHomePathField;

  private JPanel myGradlePanel;
  @Nullable
  private JLabel myGradleJdkLabel;
  @Nullable
  protected ExternalSystemJdkComboBox myGradleJdkComboBox;
  @Nullable protected FixedSizeButton myGradleJdkSetUpButton;
  private boolean dropGradleJdkComponents;

  @Nullable JComboBox<DistributionTypeItem> myGradleDistributionComboBox;
  @Nullable JBLabel myGradleDistributionHint;

  private boolean dropUseWrapperButton;
  private boolean dropCustomizableWrapperButton;
  private boolean dropUseLocalDistributionButton;
  private boolean dropUseBundledDistributionButton;

  private JPanel myImportPanel;

  private JPanel myModulePerSourceSetPanel;
  @Nullable
  private JBCheckBox myResolveModulePerSourceSetCheckBox;
  private boolean dropResolveModulePerSourceSetCheckBox;

  @Nullable
  private JBCheckBox myResolveExternalAnnotationsCheckBox;
  private boolean dropResolveExternalAnnotationsCheckBox = !Registry.is("external.system.import.resolve.annotations", false);

  @Nullable
  private JLabel myDelegateBuildLabel;
  @Nullable
  private ComboBox<BuildRunItem> myDelegateBuildCombobox;
  private boolean dropDelegateBuildCombobox;

  @Nullable
  private JLabel myTestRunnerLabel;
  @Nullable
  private ComboBox<TestRunnerItem> myTestRunnerCombobox;
  private boolean dropTestRunnerCombobox;
  private JPanel myDelegatePanel;

  @Nullable
  private JPanel myGradleJdkPanel;

  /**
   * The target {@link Project} reference of the UI control.
   * It can be the current project of the settings UI configurable (see {@org.jetbrains.plugins.gradle.service.settings.GradleConfigurable}),
   * or the target project from the wizard context.
   */
  @NotNull
  private final Ref<Project> myProjectRef = Ref.create();
  @NotNull
  private final Disposable myProjectRefDisposable = () -> myProjectRef.set(null);

  public IdeaGradleProjectSettingsControlBuilder(@NotNull GradleProjectSettings initialSettings) {
    myInstallationManager = ServiceManager.getService(GradleInstallationManager.class);
    myInitialSettings = initialSettings;
  }

  public IdeaGradleProjectSettingsControlBuilder dropGradleJdkComponents() {
    dropGradleJdkComponents = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropUseWrapperButton() {
    dropUseWrapperButton = true;
    return this;
  }

  /**
   * @deprecated Use {@link #dropUseLocalDistributionButton()} instead
   */
  @Deprecated
  public IdeaGradleProjectSettingsControlBuilder dropGradleHomePathComponents() {
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropCustomizableWrapperButton() {
    dropCustomizableWrapperButton = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropUseLocalDistributionButton() {
    dropUseLocalDistributionButton = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropUseBundledDistributionButton() {
    dropUseBundledDistributionButton = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropUseAutoImportBox() {
    dropUseAutoImportBox = true;
    return this;
  }

  @Deprecated
  public IdeaGradleProjectSettingsControlBuilder dropCreateEmptyContentRootDirectoriesBox() {
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropResolveModulePerSourceSetCheckBox() {
    dropResolveModulePerSourceSetCheckBox = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropResolveExternalAnnotationsCheckBox() {
    dropResolveExternalAnnotationsCheckBox = true;
    return this;
  }

  /**
   *
   * @deprecated Use {@link IdeaGradleSystemSettingsControlBuilder#dropStoreExternallyCheckBox}
   */
  @Deprecated
  public IdeaGradleProjectSettingsControlBuilder dropStoreExternallyCheckBox() {
    return this;
  }

  @Deprecated
  public IdeaGradleProjectSettingsControlBuilder dropModulesGroupingOptionPanel() {
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropDelegateBuildCombobox() {
    dropDelegateBuildCombobox = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropTestRunnerCombobox() {
    dropTestRunnerCombobox = true;
    return this;
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);

    if (show) {
      // some controls need to remain hidden depending on the selection
      // also error notifications should be shown
      updateDistributionComponents();
      updateDeprecatedControls();
    }
  }

  @Override
  @NotNull
  public GradleProjectSettings getInitialSettings() {
    return myInitialSettings;
  }

  @Override
  public ExternalSystemSettingsControlCustomizer getExternalSystemSettingsControlCustomizer() {
    return new ExternalSystemSettingsControlCustomizer(dropUseAutoImportBox);
  }

  @Override
  public void createAndFillControls(PaintAwarePanel content, int indentLevel) {
    content.setPaintCallback(graphics -> showBalloonIfNecessary());

    content.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (!"ancestor".equals(evt.getPropertyName())) {
          return;
        }

        // Configure the balloon to show on initial configurable drawing.
        myShowBalloonIfNecessary = evt.getNewValue() != null && evt.getOldValue() == null;

        if (evt.getNewValue() == null && evt.getOldValue() != null) {
          // Cancel delayed balloons when the configurable is hidden.
          myAlarm.cancelAllRequests();
        }
      }
    });

    addImportComponents(content, indentLevel);
    addDelegationComponents(content, indentLevel);
    addGradleComponents(content, indentLevel);
  }

  private static JPanel addComponentsGroup(@Nullable String title,
                                           PaintAwarePanel content,
                                           int indentLevel,
                                           @NotNull Consumer<JPanel> configuration) {
    JPanel result = new JPanel(new GridBagLayout());
    if (title != null) {
      GridBag constraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel);
      constraints.insets.top = UIUtil.LARGE_VGAP;
      result.add(new TitledSeparator(title), constraints);
    }
    int count = result.getComponentCount();

    configuration.consume(result);

    if (result.getComponentCount() > count) {
      content.add(result, ExternalSystemUiUtil.getFillLineConstraints(0).insets(0, 0, 0, 0));
    }
    return result;
  }

  private void addImportComponents(PaintAwarePanel content, int indentLevel) {
    myImportPanel = addComponentsGroup(null, content, indentLevel, panel -> {
      if (!dropResolveModulePerSourceSetCheckBox) {
        myModulePerSourceSetPanel = new JPanel(new GridBagLayout());
        panel.add(myModulePerSourceSetPanel, ExternalSystemUiUtil.getFillLineConstraints(0).insets(0, 0, 0, 0));

        myModulePerSourceSetPanel.add(
          myResolveModulePerSourceSetCheckBox = new JBCheckBox(GradleBundle.message("gradle.settings.text.module.per.source.set",
                                                                                    getIDEName())),
          ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

        JBLabel myResolveModulePerSourceSetHintLabel = new JBLabel(
          XmlStringUtil.wrapInHtml(GradleBundle.message("gradle.settings.text.module.per.source.set.hint")),
          UIUtil.ComponentStyle.SMALL);
        myResolveModulePerSourceSetHintLabel.setIcon(AllIcons.General.BalloonWarning12);
        myResolveModulePerSourceSetHintLabel.setVerticalTextPosition(SwingConstants.TOP);
        myResolveModulePerSourceSetHintLabel.setForeground(UIUtil.getLabelFontColor(UIUtil.FontColor.BRIGHTER));

        GridBag constraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel);
        constraints.insets.top = 0;
        constraints.insets.left += UIUtil.getCheckBoxTextHorizontalOffset(myResolveModulePerSourceSetCheckBox);
        myModulePerSourceSetPanel.add(myResolveModulePerSourceSetHintLabel, constraints);
      }

      if (!dropResolveExternalAnnotationsCheckBox) {
        panel.add(
          myResolveExternalAnnotationsCheckBox = new JBCheckBox(GradleBundle.message("gradle.settings.text.download.annotations")),
          ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
      }
    });
  }

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  /**
   * Updates GUI of the gradle configurable in order to show deduced path to gradle (if possible).
   */
  private void deduceGradleHomeIfPossible() {
    if (myGradleHomePathField == null) return;

    File gradleHome = myInstallationManager.getAutodetectedGradleHome();
    if (gradleHome == null) {
      new DelayedBalloonInfo(MessageType.WARNING, LocationSettingType.UNKNOWN, BALLOON_DELAY_MILLIS).run();
      return;
    }
    myGradleHomeSettingType = LocationSettingType.DEDUCED;
    new DelayedBalloonInfo(MessageType.INFO, LocationSettingType.DEDUCED, BALLOON_DELAY_MILLIS).run();
    myGradleHomePathField.setText(gradleHome.getPath());
    myGradleHomePathField.getTextField().setForeground(LocationSettingType.DEDUCED.getColor());
  }

  private void addGradleComponents(PaintAwarePanel content, int indentLevel) {
    myGradlePanel = addComponentsGroup("Gradle", content, indentLevel, panel -> {
      addGradleChooserComponents(panel, indentLevel + 1);
      addGradleJdkComponents(panel, indentLevel + 1);
    });
  }

  @Override
  public IdeaGradleProjectSettingsControlBuilder addGradleJdkComponents(JPanel content, int indentLevel) {
    if(!dropGradleJdkComponents) {
      myGradleJdkLabel = new JBLabel(GradleBundle.message("gradle.settings.text.jvm.path"));
      myGradleJdkComboBox = new ExternalSystemJdkComboBox();
      Sdk internalJdk = ExternalSystemJdkUtil.getJdk(null, ExternalSystemJdkUtil.USE_INTERNAL_JAVA);
      if (internalJdk == null || !ExternalSystemJdkUtil.isValidJdk(internalJdk.getHomePath())) {
        myGradleJdkComboBox.withoutJre();
      }

      myGradleJdkLabel.setLabelFor(myGradleJdkComboBox);

      content.add(myGradleJdkLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
      myGradleJdkPanel = new JPanel(new BorderLayout(SystemInfo.isMac ? 0 : 2, indentLevel));
      myGradleJdkPanel.setFocusable(false);
      myGradleJdkPanel.add(myGradleJdkComboBox, BorderLayout.CENTER);
      myGradleJdkSetUpButton = new FixedSizeButton(myGradleJdkComboBox);
      myGradleJdkSetUpButton.setToolTipText(UIBundle.message("component.with.browse.button.browse.button.tooltip.text"));
      // FixedSizeButton isn't focusable but it should be selectable via keyboard.
      DumbAwareAction.create(event -> {
        for (ActionListener listener : myGradleJdkSetUpButton.getActionListeners()) {
          listener.actionPerformed(new ActionEvent(myGradleJdkComboBox, ActionEvent.ACTION_PERFORMED, "action"));
        }
      }).registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)),
                                   myGradleJdkComboBox);

      if (ScreenReader.isActive()) {
        myGradleJdkSetUpButton.setFocusable(true);
        myGradleJdkSetUpButton.getAccessibleContext().setAccessibleName(ApplicationBundle.message("button.new"));
      }
      myGradleJdkPanel.add(myGradleJdkSetUpButton, BorderLayout.EAST);
      content.add(myGradleJdkPanel, ExternalSystemUiUtil.getFillLineConstraints(0));
    }
    return this;
  }

  @Override
  public IdeaGradleProjectSettingsControlBuilder addGradleChooserComponents(JPanel content, int indentLevel) {
    ArrayList<DistributionTypeItem> availableDistributions = new ArrayList<>();

    if (!dropUseWrapperButton) availableDistributions.add(new DistributionTypeItem(DistributionType.DEFAULT_WRAPPED));
    if (!dropCustomizableWrapperButton) availableDistributions.add(new DistributionTypeItem(DistributionType.WRAPPED));
    if (!dropUseLocalDistributionButton) availableDistributions.add(new DistributionTypeItem(DistributionType.LOCAL));
    if (!dropUseBundledDistributionButton) availableDistributions.add(new DistributionTypeItem(DistributionType.BUNDLED));

    myGradleDistributionComboBox = new ComboBox<>();
    myGradleDistributionComboBox.setRenderer(new MyItemCellRenderer<>());

    myGradleDistributionHint = new JBLabel();

    myGradleHomePathField = new TextFieldWithBrowseButton();
    myGradleHomePathField.addBrowseFolderListener("", GradleBundle.message("gradle.settings.text.home.path"), null,
                                                  GradleUtil.getGradleHomeFileChooserDescriptor(),
                                                  TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

    myGradleDistributionHint.setLabelFor(myGradleHomePathField);

    myGradleHomePathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        myGradleHomePathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        myGradleHomePathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
      }
    });

    myGradleDistributionComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateDistributionComponents();
      }
    });

    myGradleDistributionComboBox.setModel(new CollectionComboBoxModel<>(availableDistributions));
    if (!availableDistributions.isEmpty()) {
      content.add(new JBLabel(GradleBundle.message("gradle.settings.text.distribution")), ExternalSystemUiUtil.getLabelConstraints(indentLevel));
      content.add(myGradleDistributionComboBox, ExternalSystemUiUtil.getLabelConstraints(0));

      JPanel additionalControlsPanel = new JPanel(new GridBagLayout());
      additionalControlsPanel.add(myGradleDistributionHint);
      additionalControlsPanel.add(myGradleHomePathField, ExternalSystemUiUtil.getFillLineConstraints(0));
      content.add(additionalControlsPanel, ExternalSystemUiUtil.getFillLineConstraints(0).insets(0, 0, 0, 0));

      // adjust the combobox height to match the height of the editor and Gradle GDK combobox.
      // - without setting the prefered size, it's resized when path component is shown/hidden
      // - without adjusting the height the combobox is a little smaller then the next combobox (Gradle JVM)
      boolean macTheme = UIUtil.isUnderDefaultMacTheme();
      myGradleDistributionComboBox.setPreferredSize(new Dimension(myGradleDistributionComboBox.getPreferredSize().width,
                                                                  myGradleHomePathField.getPreferredSize().height + (macTheme ? 3 : 0)));
    }

    return this;
  }

  private void updateDistributionComponents() {
    if (myGradleDistributionComboBox == null) return;
    if (myGradleHomePathField == null) return;

    boolean localEnabled = getSelectedGradleDistribution() == DistributionType.LOCAL;
    boolean wrapperSelected = getSelectedGradleDistribution() == DistributionType.DEFAULT_WRAPPED;

    myGradleHomePathField.setEnabled(localEnabled);
    myGradleHomePathField.setVisible(localEnabled);

    if (myGradleDistributionHint != null) {
      myGradleDistributionHint.setEnabled(wrapperSelected);
      myGradleDistributionHint.setVisible(wrapperSelected);
    }

    if (localEnabled) {
      if (myGradleHomePathField.getText().isEmpty()) {
        deduceGradleHomeIfPossible();
      }
      else {
        if (myInstallationManager.isGradleSdkHome(myGradleHomePathField.getText())) {
          myGradleHomeSettingType = LocationSettingType.EXPLICIT_CORRECT;
        }
        else {
          myGradleHomeSettingType = LocationSettingType.EXPLICIT_INCORRECT;
          myShowBalloonIfNecessary = true;
        }
      }
      showBalloonIfNecessary();
    }
    else {
      myAlarm.cancelAllRequests();
    }
  }

  @Nullable
  private DistributionType getSelectedGradleDistribution() {
    if (myGradleDistributionComboBox == null) return null;
    Object selection = myGradleDistributionComboBox.getSelectedItem();
    return selection == null ? null : ((DistributionTypeItem)selection).value;
  }

  @Override
  public boolean validate(GradleProjectSettings settings) throws ConfigurationException {
    if(myGradleJdkComboBox != null && !ApplicationManager.getApplication().isUnitTestMode()) {
      Sdk selectedJdk = myGradleJdkComboBox.getSelectedJdk();
      if(selectedJdk == null) {
        throw new ConfigurationException(GradleBundle.message("gradle.jvm.undefined"));
      }
      String homePath = selectedJdk.getHomePath();
      if(!ExternalSystemJdkUtil.isValidJdk(homePath)) {
        throw new ConfigurationException(GradleBundle.message("gradle.jvm.incorrect", homePath));
      }
    }

    if (myGradleHomePathField != null && getSelectedGradleDistribution() == DistributionType.LOCAL) {
      String gradleHomePath = FileUtil.toCanonicalPath(myGradleHomePathField.getText());
      if (StringUtil.isEmpty(gradleHomePath)) {
        myGradleHomeSettingType = LocationSettingType.UNKNOWN;
        throw new ConfigurationException(GradleBundle.message("gradle.home.setting.type.explicit.empty", gradleHomePath));
      }
      else if (!myInstallationManager.isGradleSdkHome(new File(gradleHomePath))) {
        myGradleHomeSettingType = LocationSettingType.EXPLICIT_INCORRECT;
        new DelayedBalloonInfo(MessageType.ERROR, myGradleHomeSettingType, 0).run();
        throw new ConfigurationException(GradleBundle.message("gradle.home.setting.type.explicit.incorrect", gradleHomePath));
      }
    }
    return true;
  }

  @Override
  public void apply(GradleProjectSettings settings) {
    settings.setCompositeBuild(myInitialSettings.getCompositeBuild());
    if (myGradleHomePathField != null) {
      String gradleHomePath = FileUtil.toCanonicalPath(myGradleHomePathField.getText());
      if (StringUtil.isEmpty(gradleHomePath)) {
        settings.setGradleHome(null);
      }
      else {
        settings.setGradleHome(gradleHomePath);
        GradleUtil.storeLastUsedGradleHome(gradleHomePath);
      }
    }

    if (myGradleJdkComboBox != null) {
      final String gradleJvm = FileUtil.toCanonicalPath(myGradleJdkComboBox.getSelectedValue());
      settings.setGradleJvm(StringUtil.isEmpty(gradleJvm) ? null : gradleJvm);
    }

    if (myResolveModulePerSourceSetCheckBox != null) {
      settings.setResolveModulePerSourceSet(myResolveModulePerSourceSetCheckBox.isSelected());
    }

    if (myResolveExternalAnnotationsCheckBox != null) {
      settings.setResolveExternalAnnotations(myResolveExternalAnnotationsCheckBox.isSelected());
    }

    if (myGradleDistributionComboBox != null) {
      Object selected = myGradleDistributionComboBox.getSelectedItem();
      if (selected instanceof DistributionTypeItem) {
        settings.setDistributionType(((DistributionTypeItem)selected).value);
      }
    }

    if (myDelegateBuildCombobox != null) {
      Object delegateBuildSelectedItem = myDelegateBuildCombobox.getSelectedItem();
      if (delegateBuildSelectedItem instanceof BuildRunItem) {
        settings.setDelegatedBuild(ObjectUtils.notNull(((BuildRunItem)delegateBuildSelectedItem).value,
                                                       GradleProjectSettings.DEFAULT_DELEGATE));
      }
    }
    if (myTestRunnerCombobox != null) {
      Object testRunnerSelectedItem = myTestRunnerCombobox.getSelectedItem();
      if (testRunnerSelectedItem instanceof TestRunnerItem) {
        settings.setTestRunner(ObjectUtils.notNull(((TestRunnerItem)testRunnerSelectedItem).value,
                                                   GradleProjectSettings.DEFAULT_TEST_RUNNER));
      }
    }
  }

  @Override
  public boolean isModified() {
    if (myGradleDistributionComboBox != null && myGradleDistributionComboBox.getSelectedItem() instanceof DistributionTypeItem
        && ((DistributionTypeItem)myGradleDistributionComboBox.getSelectedItem()).value != myInitialSettings.getDistributionType()) {
      return true;
    }

    if (myResolveModulePerSourceSetCheckBox != null &&
        (myResolveModulePerSourceSetCheckBox.isSelected() != myInitialSettings.isResolveModulePerSourceSet())) {
      return true;
    }

    if (myResolveExternalAnnotationsCheckBox != null &&
        (myResolveExternalAnnotationsCheckBox.isSelected() != myInitialSettings.isResolveExternalAnnotations())) {
      return true;
    }

    if (myDelegateBuildCombobox != null && myDelegateBuildCombobox.getSelectedItem() instanceof MyItem
        && !Objects.equals(((MyItem)myDelegateBuildCombobox.getSelectedItem()).value, myInitialSettings.getDelegatedBuild())) {
      return true;
    }

    if (myTestRunnerCombobox != null && myTestRunnerCombobox.getSelectedItem() instanceof MyItem
        && !Objects.equals(((MyItem)myTestRunnerCombobox.getSelectedItem()).value, myInitialSettings.getTestRunner())) {
      return true;
    }

    if (myGradleJdkComboBox != null && !StringUtil.equals(myGradleJdkComboBox.getSelectedValue(), myInitialSettings.getGradleJvm())) {
      return true;
    }

    if (myGradleHomePathField == null) return false;
    String gradleHome = FileUtil.toCanonicalPath(myGradleHomePathField.getText());
    if (StringUtil.isEmpty(gradleHome)) {
      return !StringUtil.isEmpty(myInitialSettings.getGradleHome());
    }
    else {
      return !gradleHome.equals(myInitialSettings.getGradleHome());
    }
  }

  @Override
  public void reset(@Nullable Project project, GradleProjectSettings settings, boolean isDefaultModuleCreation) {
    reset(project, settings, isDefaultModuleCreation, null);
  }

  @Override
  public void reset(@Nullable Project project,
                    GradleProjectSettings settings,
                    boolean isDefaultModuleCreation,
                    @Nullable WizardContext wizardContext) {
    updateProjectRef(project, wizardContext);

    String gradleHome = settings.getGradleHome();
    if (myGradleHomePathField != null) {
      myGradleHomePathField.setText(gradleHome == null ? "" : gradleHome);
      myGradleHomePathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
    }
    resetImportControls(settings);

    resetGradleJdkComboBox(project, settings, wizardContext);
    resetWrapperControls(settings.getExternalProjectPath(), settings, isDefaultModuleCreation);
    resetGradleDelegationControls(wizardContext);

    if (StringUtil.isEmpty(gradleHome)) {
      myGradleHomeSettingType = LocationSettingType.UNKNOWN;
      deduceGradleHomeIfPossible();
    }
    else {
      myGradleHomeSettingType = myInstallationManager.isGradleSdkHome(new File(gradleHome)) ?
                                LocationSettingType.EXPLICIT_CORRECT :
                                LocationSettingType.EXPLICIT_INCORRECT;
      myAlarm.cancelAllRequests();
      if (myGradleHomeSettingType == LocationSettingType.EXPLICIT_INCORRECT &&
          settings.getDistributionType() == DistributionType.LOCAL) {
        new DelayedBalloonInfo(MessageType.ERROR, myGradleHomeSettingType, 0).run();
      }
    }
    updateDeprecatedControls();
  }

  @Override
  public void update(String linkedProjectPath, GradleProjectSettings settings, boolean isDefaultModuleCreation) {
    resetWrapperControls(linkedProjectPath, settings, isDefaultModuleCreation);
    resetImportControls(settings);
    updateDeprecatedControls();
  }

  private void resetImportControls(GradleProjectSettings settings) {
    if (myResolveModulePerSourceSetCheckBox != null) {
      myResolveModulePerSourceSetCheckBox.setSelected(settings.isResolveModulePerSourceSet());
      boolean showSetting = !settings.isResolveModulePerSourceSet()
                            || Registry.is("gradle.settings.showDeprecatedSettings", false);
      myModulePerSourceSetPanel.putClientProperty(HIDDEN_KEY, showSetting);
    }
    if (myResolveExternalAnnotationsCheckBox != null) {
      myResolveExternalAnnotationsCheckBox.setSelected(settings.isResolveExternalAnnotations());
    }
  }

  private void updateDeprecatedControls() {
    if (myModulePerSourceSetPanel != null) {
      myModulePerSourceSetPanel.setVisible(myModulePerSourceSetPanel.getClientProperty(HIDDEN_KEY) == Boolean.TRUE);
    }
  }

  protected void resetGradleJdkComboBox(@Nullable final Project project,
                                      GradleProjectSettings settings,
                                      @Nullable WizardContext wizardContext) {
    if (myGradleJdkComboBox == null) return;

    final String gradleJvm = settings.getGradleJvm();
    myGradleJdkComboBox.setProject(project);
    myGradleJdkComboBox.setProjectJdk(null);

    Sdk projectJdk = wizardContext != null ? wizardContext.getProjectJdk() : null;
    final String sdkItem = ObjectUtils.nullizeByCondition(gradleJvm, s ->
      (projectJdk == null && project == null && StringUtil.equals(USE_PROJECT_JDK, s)) || StringUtil.isEmpty(s));

    myGradleJdkComboBox.refreshData(sdkItem, projectJdk);
    if (myGradleJdkSetUpButton != null) {
      ProjectSdksModel sdksModel = new ProjectSdksModel();
      myGradleJdkComboBox.setSetupButton(myGradleJdkSetUpButton, sdksModel, null, JavaSdkType.class::isInstance);
    }
  }

  private void resetWrapperControls(String linkedProjectPath, @NotNull GradleProjectSettings settings, boolean isDefaultModuleCreation) {
    if (myGradleDistributionComboBox == null) return;

    if (isDefaultModuleCreation) {
      DistributionTypeItem toRemove = new DistributionTypeItem(DistributionType.WRAPPED);
      ((CollectionComboBoxModel<DistributionTypeItem>)myGradleDistributionComboBox.getModel()).remove(toRemove);
    }

    if (StringUtil.isEmpty(linkedProjectPath) && !isDefaultModuleCreation) {
      myGradleDistributionComboBox.setSelectedItem(new DistributionTypeItem(DistributionType.LOCAL));
      return;
    }

    if (myGradleDistributionHint != null && !dropUseWrapperButton) {
      final boolean isGradleDefaultWrapperFilesExist = GradleUtil.isGradleDefaultWrapperFilesExist(linkedProjectPath);
      boolean showError = !isGradleDefaultWrapperFilesExist && !isDefaultModuleCreation;
      myGradleDistributionHint.setText(showError ? "'gradle-wrapper.properties' not found" : null);
      myGradleDistributionHint.setIcon(showError ? AllIcons.General.Error : null);
    }

    if (settings.getDistributionType() == null) {
      if (myGradleDistributionComboBox.getItemCount() > 0) {
        myGradleDistributionComboBox.setSelectedIndex(0);
      }
    }
    else {
      myGradleDistributionComboBox.setSelectedItem(new DistributionTypeItem(settings.getDistributionType()));
    }
  }

  private void addDelegationComponents(PaintAwarePanel content, int indentLevel) {
    myDelegatePanel = addComponentsGroup(GradleBundle.message("gradle.settings.text.build.run.title"), content, indentLevel, panel -> {
      if (dropDelegateBuildCombobox && dropTestRunnerCombobox) return;

      JBLabel label = new JBLabel(
        XmlStringUtil.wrapInHtml(GradleBundle.message("gradle.settings.text.build.run.hint", getIDEName())),
        UIUtil.ComponentStyle.SMALL);
      label.setForeground(UIUtil.getLabelFontColor(UIUtil.FontColor.BRIGHTER));

      GridBag constraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel + 1);
      constraints.insets.bottom = UIUtil.LARGE_VGAP;
      panel.add(label, constraints);

      if (!dropDelegateBuildCombobox) {
        BuildRunItem[] states = new BuildRunItem[]{new BuildRunItem(Boolean.TRUE), new BuildRunItem(Boolean.FALSE)};
        myDelegateBuildCombobox = new ComboBox<>(states);
        myDelegateBuildCombobox.setRenderer(new MyItemCellRenderer<>());
        myDelegateBuildCombobox.setSelectedItem(new BuildRunItem(myInitialSettings.getDelegatedBuild()));

        myDelegateBuildLabel = new JBLabel(GradleBundle.message("gradle.settings.text.build.run"));
        panel.add(myDelegateBuildLabel, getLabelConstraints(indentLevel + 1));
        panel.add(myDelegateBuildCombobox);
        panel.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel + 1));

        myDelegateBuildLabel.setLabelFor(myDelegateBuildCombobox);
      }
      if (!dropTestRunnerCombobox) {
        TestRunnerItem[] testRunners = StreamEx.of(TestRunner.values()).map(TestRunnerItem::new).toArray(TestRunnerItem[]::new);
        myTestRunnerCombobox = new ComboBox<>(testRunners);
        myTestRunnerCombobox.setRenderer(new MyItemCellRenderer<>());
        myTestRunnerCombobox.setSelectedItem(new TestRunnerItem(myInitialSettings.getTestRunner()));

        myTestRunnerLabel = new JBLabel(GradleBundle.message("gradle.settings.text.run.tests"));
        panel.add(myTestRunnerLabel, getLabelConstraints(indentLevel + 1));
        panel.add(myTestRunnerCombobox);
        panel.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel + 1));

        myTestRunnerLabel.setLabelFor(myTestRunnerCombobox);
      }
    });
  }

  private void resetGradleDelegationControls(@Nullable WizardContext wizardContext) {
    if (wizardContext != null) {
      dropTestRunnerCombobox();
      dropDelegateBuildCombobox();
      if (myDelegatePanel != null) {
        Container parent = myDelegatePanel.getParent();
        if (parent != null) {
          parent.remove(myDelegatePanel);
        }
        myDelegatePanel = null;
        myDelegateBuildCombobox = null;
        myTestRunnerCombobox = null;
      }
      return;
    }
    if (myDelegateBuildCombobox != null) {
      myDelegateBuildCombobox.setSelectedItem(new BuildRunItem(myInitialSettings.getDelegatedBuild()));
    }
    if (myTestRunnerCombobox != null) {
      myTestRunnerCombobox.setSelectedItem(new TestRunnerItem(myInitialSettings.getTestRunner()));
    }
  }

  @NotNull
  private static GridBag getLabelConstraints(int indentLevel) {
    Insets insets = JBUI.insets(0, INSETS + INSETS * indentLevel, 0, INSETS);
    return new GridBag().anchor(GridBagConstraints.WEST).weightx(0).insets(insets);
  }

  void showBalloonIfNecessary() {
    if (!myShowBalloonIfNecessary || (myGradleHomePathField != null && !myGradleHomePathField.isEnabled())) {
      return;
    }
    myShowBalloonIfNecessary = false;
    MessageType messageType = null;
    switch (myGradleHomeSettingType) {
      case DEDUCED:
        messageType = MessageType.INFO;
        break;
      case EXPLICIT_INCORRECT:
      case UNKNOWN:
        messageType = MessageType.ERROR;
        break;
      default:
    }
    if (messageType != null) {
      new DelayedBalloonInfo(messageType, myGradleHomeSettingType, BALLOON_DELAY_MILLIS).run();
    }
  }

  private void updateProjectRef(@Nullable Project project, @Nullable WizardContext wizardContext) {
    if (wizardContext != null && wizardContext.getProject() != null) {
      project = wizardContext.getProject();
    }
    if (project != null && project != myProjectRef.get()
        && Disposer.findRegisteredObject(project, myProjectRefDisposable) == null) {
      Disposer.register(project, myProjectRefDisposable);
    }
    myProjectRef.set(project);
  }

  private class DelayedBalloonInfo implements Runnable {
    private final MessageType myMessageType;
    private final String myText;
    private final long myTriggerTime;

    DelayedBalloonInfo(@NotNull MessageType messageType, @NotNull LocationSettingType settingType, long delayMillis) {
      myMessageType = messageType;
      myText = settingType.getDescription(GradleConstants.SYSTEM_ID);
      myTriggerTime = System.currentTimeMillis() + delayMillis;
    }

    @Override
    public void run() {
      long diff = myTriggerTime - System.currentTimeMillis();
      if (diff > 0) {
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(this, diff);
        return;
      }
      if (myGradleHomePathField == null || !myGradleHomePathField.isShowing()) {
        // Don't schedule the balloon if the configurable is hidden.
        return;
      }
      ExternalSystemUiUtil.showBalloon(myGradleHomePathField, myMessageType, myText);
    }
  }

  private static class MyItemCellRenderer<T> extends ColoredListCellRenderer<MyItem<T>> {

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends MyItem<T>> list,
                                         MyItem<T> value,
                                         int index,
                                         boolean selected,
                                         boolean hasFocus) {
      if (value == null) return;
      CompositeAppearance.DequeEnd ending = new CompositeAppearance().getEnding();
      ending.addText(value.getText(), getTextAttributes(selected));
      if (value.getComment() != null) {
        SimpleTextAttributes commentAttributes = getCommentAttributes(selected);
        ending.addComment(value.getComment(), commentAttributes);
      }
      ending.getAppearance().customize(this);
    }

    @NotNull
    private static SimpleTextAttributes getTextAttributes(boolean selected) {
      return selected && !(SystemInfo.isWinVistaOrNewer && UIManager.getLookAndFeel().getName().contains("Windows"))
             ? SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES
             : SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES;
    }

    @NotNull
    private static SimpleTextAttributes getCommentAttributes(boolean selected) {
      return SystemInfo.isMac && selected
             ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.WHITE)
             : SimpleTextAttributes.GRAY_ATTRIBUTES;
    }
  }

  private static abstract class MyItem<T> {
    @Nullable
    protected final T value;

    private MyItem(@Nullable T value) {
      this.value = value;
    }

    protected abstract String getText();

    protected abstract String getComment();

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MyItem)) return false;
      MyItem item = (MyItem)o;
      return Objects.equals(value, item.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }

  private class BuildRunItem extends MyItem<Boolean> {

    private BuildRunItem(@Nullable Boolean value) {
      super(value);
    }

    @Override
    protected String getText() {
      return getText(value);
    }

    @Override
    protected String getComment() {
      return Comparing.equal(value, GradleProjectSettings.DEFAULT_DELEGATE) ? "Default" : null;
    }

    @NotNull
    private String getText(@Nullable Boolean state) {
      if (state == Boolean.TRUE) {
        return "Gradle";
      }
      if (state == Boolean.FALSE) {
        return getIDEName();
      }
      LOG.error("Unexpected: " + state);
      return "Unexpected: " + state;
    }
  }

  static String getIDEName() {
    return ApplicationNamesInfo.getInstance().getFullProductName();
  }

  private class TestRunnerItem extends MyItem<TestRunner> {

    private TestRunnerItem(@Nullable TestRunner value) {
      super(value);
    }

    @Override
    protected String getText() {
      return getText(value);
    }

    @Override
    protected String getComment() {
      return Comparing.equal(value, GradleProjectSettings.DEFAULT_TEST_RUNNER) ? "Default" : null;
    }

    @NotNull
    private String getText(@Nullable TestRunner runner) {
      if (runner == TestRunner.GRADLE) {
        return "Gradle";
      }
      if (runner == TestRunner.PLATFORM) {
        return getIDEName();
      }
      if (runner == TestRunner.CHOOSE_PER_TEST) {
        return GradleBundle.message("gradle.settings.text.build.run.per.test");
      }
      LOG.error("Unexpected: " + runner);
      return "Unexpected: " + runner;
    }
  }

  private class DistributionTypeItem extends MyItem<DistributionType> {

    private DistributionTypeItem(@Nullable DistributionType value) {
      super(value);
    }

    @Override
    protected String getText() {
      return getText(value);
    }

    @Override
    protected String getComment() {
      return null;
    }

    @NotNull
    private String getText(@Nullable DistributionType value) {
      if (value != null) {
        switch (value) {
          case BUNDLED:
            return GradleBundle.message("gradle.settings.text.distribution.bundled", GradleVersion.current().getVersion());
          case DEFAULT_WRAPPED:
            return GradleBundle.message("gradle.settings.text.distribution.wrapper");
          case WRAPPED:
            return GradleBundle.message("gradle.settings.text.distribution.wrapper.task");
          case LOCAL:
            return GradleBundle.message("gradle.settings.text.distribution.location");
        }
      }
      LOG.error("Unexpected: " + value);
      return "Unexpected: " + value;
    }
  }
}
