// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.components.ServiceManager;
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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.util.Alarm;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ThreeState;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import one.util.streamex.StreamEx;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.GradleInstallationManager;
import org.jetbrains.plugins.gradle.settings.DefaultGradleProjectSettings;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK;
import static com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil.INSETS;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("FieldCanBeLocal") // Used implicitly by reflection at disposeUIResources() and showUi()
public class IdeaGradleProjectSettingsControlBuilder implements GradleProjectSettingsControlBuilder {

  private static final long BALLOON_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
  @NotNull
  private final GradleInstallationManager myInstallationManager;
  @NotNull
  private final GradleProjectSettings myInitialSettings;
  @NotNull
  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  @NotNull
  private LocationSettingType myGradleHomeSettingType = LocationSettingType.UNKNOWN;
  private boolean myShowBalloonIfNecessary;
  private final ActionListener myActionListener;

  private boolean dropUseAutoImportBox;
  private boolean dropCreateEmptyContentRootDirectoriesBox;
  private boolean dropModulesGroupingOptionPanel;

  @Nullable
  private JLabel myGradleHomeLabel;
  @Nullable
  private TextFieldWithBrowseButton myGradleHomePathField;
  private boolean dropGradleHomePathComponents;

  @Nullable
  private JLabel myGradleJdkLabel;
  @Nullable
  protected ExternalSystemJdkComboBox myGradleJdkComboBox;
  @Nullable protected FixedSizeButton myGradleJdkSetUpButton;
  private boolean dropGradleJdkComponents;

  @Nullable
  private JBRadioButton myUseWrapperButton;
  private boolean dropUseWrapperButton;

  @Nullable
  private JBRadioButton myUseWrapperWithVerificationButton;
  @Nullable
  private JBLabel myUseWrapperVerificationLabel;
  private boolean dropCustomizableWrapperButton;

  @Nullable
  private JBRadioButton myUseLocalDistributionButton;
  private boolean dropUseLocalDistributionButton;

  @Nullable
  private JBRadioButton myUseBundledDistributionButton;
  private boolean dropUseBundledDistributionButton;

  @Nullable
  private JBCheckBox myResolveModulePerSourceSetCheckBox;
  private boolean dropResolveModulePerSourceSetCheckBox;

  @Nullable
  private JBCheckBox myResolveExternalAnnotationsCheckBox;
  private boolean dropResolveExternalAnnotationsCheckBox = !Registry.is("external.system.import.resolve.annotations", false);

  @Nullable
  private JBCheckBox myStoreExternallyCheckBox;
  private boolean dropStoreExternallyCheckBox;

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

    myActionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myGradleHomePathField == null) return;

        boolean localDistributionEnabled = myUseLocalDistributionButton != null && myUseLocalDistributionButton.isSelected();
        myGradleHomePathField.setEnabled(localDistributionEnabled);
        if (localDistributionEnabled) {
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
    };
  }

  public IdeaGradleProjectSettingsControlBuilder dropGradleJdkComponents() {
    dropGradleJdkComponents = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropUseWrapperButton() {
    dropUseWrapperButton = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropGradleHomePathComponents() {
    dropGradleHomePathComponents = true;
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

  public IdeaGradleProjectSettingsControlBuilder dropCreateEmptyContentRootDirectoriesBox() {
    dropCreateEmptyContentRootDirectoriesBox = true;
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

  public IdeaGradleProjectSettingsControlBuilder dropStoreExternallyCheckBox() {
    dropStoreExternallyCheckBox = true;
    return this;
  }

  public IdeaGradleProjectSettingsControlBuilder dropModulesGroupingOptionPanel() {
    dropModulesGroupingOptionPanel = true;
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
  }

  @Override
  @NotNull
  public GradleProjectSettings getInitialSettings() {
    return myInitialSettings;
  }

  @Override
  public ExternalSystemSettingsControlCustomizer getExternalSystemSettingsControlCustomizer() {
    return new ExternalSystemSettingsControlCustomizer(
      dropUseAutoImportBox, dropCreateEmptyContentRootDirectoriesBox, dropModulesGroupingOptionPanel);
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

    if (!dropResolveModulePerSourceSetCheckBox) {
      myResolveModulePerSourceSetCheckBox = new JBCheckBox(GradleBundle.message("gradle.settings.text.create.module.per.sourceset"));
      content.add(myResolveModulePerSourceSetCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    if (!dropResolveExternalAnnotationsCheckBox) {
      myResolveExternalAnnotationsCheckBox = new JBCheckBox(GradleBundle.message("gradle.settings.text.resolve.external.annotations"));
      content.add(myResolveExternalAnnotationsCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    if (!dropStoreExternallyCheckBox && myInitialSettings.getStoreProjectFilesExternally() != ThreeState.UNSURE) {
      myStoreExternallyCheckBox = new JBCheckBox("Store generated project files externally");
      content.add(myStoreExternallyCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    addGradleChooserComponents(content, indentLevel);
    addGradleHomeComponents(content, indentLevel);
    addGradleJdkComponents(content, indentLevel);
    addGradleDelegationComponents(content, indentLevel);
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

  @Override
  public IdeaGradleProjectSettingsControlBuilder addGradleJdkComponents(PaintAwarePanel content, int indentLevel) {
    if(!dropGradleJdkComponents) {
      myGradleJdkLabel = new JBLabel(GradleBundle.message("gradle.settings.text.jvm.path"));
      myGradleJdkComboBox = new ExternalSystemJdkComboBox();
      Sdk internalJdk = ExternalSystemJdkUtil.getJdk(null, ExternalSystemJdkUtil.USE_INTERNAL_JAVA);
      if (internalJdk == null || !ExternalSystemJdkUtil.isValidJdk(internalJdk.getHomePath())) {
        myGradleJdkComboBox.withoutJre();
      }

      content.add(myGradleJdkLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel + 1));
      myGradleJdkPanel = new JPanel(new BorderLayout(SystemInfo.isMac ? 0 : 2, 0));
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
  public IdeaGradleProjectSettingsControlBuilder addGradleChooserComponents(PaintAwarePanel content, int indentLevel) {
    ButtonGroup buttonGroup = new ButtonGroup();

    if(!dropUseWrapperButton) {
      myUseWrapperButton = new JBRadioButton(GradleBundle.message("gradle.settings.text.use.default_wrapper.configured"));
      myUseWrapperButton.addActionListener(myActionListener);
      buttonGroup.add(myUseWrapperButton);
      content.add(myUseWrapperButton, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    if(!dropCustomizableWrapperButton) {
      myUseWrapperWithVerificationButton = new JBRadioButton(GradleBundle.message("gradle.settings.text.use.customizable_wrapper"));
      myUseWrapperWithVerificationButton.addActionListener(myActionListener);
      myUseWrapperVerificationLabel = new JBLabel(GradleBundle.message("gradle.settings.text.wrapper.customization.compatibility"));
      myUseWrapperVerificationLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.MINI));
      myUseWrapperVerificationLabel.setIcon(UIUtil.getBalloonInformationIcon());
      buttonGroup.add(myUseWrapperWithVerificationButton);
      content.add(myUseWrapperWithVerificationButton, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
      content.add(myUseWrapperVerificationLabel, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    if(!dropUseLocalDistributionButton) {
      myUseLocalDistributionButton = new JBRadioButton(GradleBundle.message("gradle.settings.text.use.local.distribution"));
      myUseLocalDistributionButton.addActionListener(myActionListener);
      buttonGroup.add(myUseLocalDistributionButton);
      content.add(myUseLocalDistributionButton, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    if(!dropUseBundledDistributionButton) {
      myUseBundledDistributionButton = new JBRadioButton(
        GradleBundle.message("gradle.settings.text.use.bundled.distribution", GradleVersion.current().getVersion()));
      myUseBundledDistributionButton.addActionListener(myActionListener);
      buttonGroup.add(myUseBundledDistributionButton);
      //content.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
      content.add(myUseBundledDistributionButton, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    return this;
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
    if (myGradleHomePathField == null) return true;

    String gradleHomePath = FileUtil.toCanonicalPath(myGradleHomePathField.getText());
    if (myUseLocalDistributionButton != null && myUseLocalDistributionButton.isSelected()) {
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

    if (myStoreExternallyCheckBox != null) {
      settings.setStoreProjectFilesExternally(ThreeState.fromBoolean(myStoreExternallyCheckBox.isSelected()));
    }

    if (myUseLocalDistributionButton != null && myUseLocalDistributionButton.isSelected()) {
      settings.setDistributionType(DistributionType.LOCAL);
    }
    else if (myUseWrapperButton != null && myUseWrapperButton.isSelected()) {
      settings.setDistributionType(DistributionType.DEFAULT_WRAPPED);
    }
    else if ((myUseWrapperWithVerificationButton != null && myUseWrapperWithVerificationButton.isSelected()) ||
             (myUseBundledDistributionButton != null && myUseBundledDistributionButton.isSelected())) {
      settings.setDistributionType(DistributionType.WRAPPED);
    }

    if (myDelegateBuildCombobox != null) {
      Object delegateBuildSelectedItem = myDelegateBuildCombobox.getSelectedItem();
      if (delegateBuildSelectedItem instanceof BuildRunItem) {
        settings.setDelegatedBuild(ObjectUtils.notNull(((BuildRunItem)delegateBuildSelectedItem).value, ThreeState.UNSURE));
      }
    }
    if (myTestRunnerCombobox != null) {
      Object testRunnerSelectedItem = myTestRunnerCombobox.getSelectedItem();
      if (testRunnerSelectedItem instanceof TestRunnerItem) {
        settings.setTestRunner(((TestRunnerItem)testRunnerSelectedItem).value);
      }
    }
  }

  @Override
  public boolean isModified() {
    DistributionType distributionType = myInitialSettings.getDistributionType();
    if (myUseBundledDistributionButton != null &&
        myUseBundledDistributionButton.isSelected() &&
        distributionType != DistributionType.BUNDLED) {
      return true;
    }

    if (myUseWrapperButton != null && myUseWrapperButton.isSelected() && distributionType != DistributionType.DEFAULT_WRAPPED) {
      return true;
    }

    if (myUseWrapperWithVerificationButton != null &&
        myUseWrapperWithVerificationButton.isSelected() &&
        distributionType != DistributionType.WRAPPED) {
      return true;
    }

    if (myUseLocalDistributionButton != null && myUseLocalDistributionButton.isSelected() && distributionType != DistributionType.LOCAL) {
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

    if (myStoreExternallyCheckBox != null && ThreeState.fromBoolean(myStoreExternallyCheckBox.isSelected()) != myInitialSettings.getStoreProjectFilesExternally()) {
      return true;
    }

    if (myDelegateBuildCombobox != null && myDelegateBuildCombobox.getSelectedItem() instanceof MyItem
        && ((MyItem)myDelegateBuildCombobox.getSelectedItem()).value != myInitialSettings.getDelegatedBuild()) {
      return true;
    }

    if (myTestRunnerCombobox != null && myTestRunnerCombobox.getSelectedItem() instanceof MyItem
        && ((MyItem)myTestRunnerCombobox.getSelectedItem()).value != myInitialSettings.getTestRunner()) {
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
    if (myResolveModulePerSourceSetCheckBox != null) {
      myResolveModulePerSourceSetCheckBox.setSelected(settings.isResolveModulePerSourceSet());
    }
    if (myResolveExternalAnnotationsCheckBox != null) {
      myResolveExternalAnnotationsCheckBox.setSelected(settings.isResolveExternalAnnotations());
    }
    if (myStoreExternallyCheckBox != null) {
      myStoreExternallyCheckBox.setSelected(settings.getStoreProjectFilesExternally() == ThreeState.YES);
    }

    resetGradleJdkComboBox(project, settings, wizardContext);
    resetWrapperControls(settings.getExternalProjectPath(), settings, isDefaultModuleCreation);
    resetGradleDelegationControls(wizardContext);

    if (myUseLocalDistributionButton != null && !myUseLocalDistributionButton.isSelected()) {
      myGradleHomePathField.setEnabled(false);
      return;
    }

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
  }

  @Override
  public void update(String linkedProjectPath, GradleProjectSettings settings, boolean isDefaultModuleCreation) {
    resetWrapperControls(linkedProjectPath, settings, isDefaultModuleCreation);
    if (myResolveModulePerSourceSetCheckBox != null) {
      myResolveModulePerSourceSetCheckBox.setSelected(settings.isResolveModulePerSourceSet());
    }
    if (myResolveExternalAnnotationsCheckBox != null) {
      myResolveExternalAnnotationsCheckBox.setSelected(settings.isResolveExternalAnnotations());
    }
  }

  @Override
  public IdeaGradleProjectSettingsControlBuilder addGradleHomeComponents(PaintAwarePanel content, int indentLevel) {
    if(dropGradleHomePathComponents) return this;

    myGradleHomeLabel = new JBLabel(GradleBundle.message("gradle.settings.text.home.path"));
    myGradleHomePathField = new TextFieldWithBrowseButton();

    myGradleHomePathField.addBrowseFolderListener("", GradleBundle.message("gradle.settings.text.home.path"), null,
                                                  GradleUtil.getGradleHomeFileChooserDescriptor(),
                                                  TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
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

    content.add(myGradleHomeLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
    content.add(myGradleHomePathField, ExternalSystemUiUtil.getFillLineConstraints(0));

    return this;
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
    if (isDefaultModuleCreation) {
      JComponent[] toRemove = new JComponent[]{myUseWrapperWithVerificationButton, myUseWrapperVerificationLabel};
      for (JComponent component : toRemove) {
        if (component != null) {
          Container parent = component.getParent();
          if (parent != null) {
            parent.remove(component);
          }
        }
      }
      myUseWrapperWithVerificationButton = null;
      myUseWrapperVerificationLabel = null;
    }

    if (StringUtil.isEmpty(linkedProjectPath) && !isDefaultModuleCreation) {
      if (myUseLocalDistributionButton != null) {
        myUseLocalDistributionButton.setSelected(true);
      }
      if (myGradleHomePathField != null) {
        myGradleHomePathField.setEnabled(true);
      }
      return;
    }

    final boolean isGradleDefaultWrapperFilesExist = GradleUtil.isGradleDefaultWrapperFilesExist(linkedProjectPath);
    if (myUseWrapperButton != null && (isGradleDefaultWrapperFilesExist || isDefaultModuleCreation)) {
      myUseWrapperButton.setEnabled(true);
      myUseWrapperButton.setSelected(true);
      if (myGradleHomePathField != null) {
        myGradleHomePathField.setEnabled(false);
      }
      myUseWrapperButton.setText(GradleBundle.message("gradle.settings.text.use.default_wrapper.configured"));
    }
    else {
      if (myUseWrapperButton != null) {
        myUseWrapperButton.setEnabled(false);
        myUseWrapperButton.setText(GradleBundle.message("gradle.settings.text.use.default_wrapper.not_configured"));
      }
      if (myUseLocalDistributionButton != null) {
        myUseLocalDistributionButton.setSelected(true);
      }
      if (myGradleHomePathField != null) {
        myGradleHomePathField.setEnabled(true);
      }
    }

    if (settings.getDistributionType() == null) {
      return;
    }

    switch (settings.getDistributionType()) {
      case LOCAL:
        if (myGradleHomePathField != null) {
          myGradleHomePathField.setEnabled(true);
        }
        if (myUseLocalDistributionButton != null) {
          myUseLocalDistributionButton.setSelected(true);
        }
        break;
      case DEFAULT_WRAPPED:
        if (isGradleDefaultWrapperFilesExist) {
          if (myGradleHomePathField != null) {
            myGradleHomePathField.setEnabled(false);
          }
          if (myUseWrapperButton != null) {
            myUseWrapperButton.setSelected(true);
            myUseWrapperButton.setEnabled(true);
          }
        }
        break;
      case WRAPPED:
        if (myGradleHomePathField != null) {
          myGradleHomePathField.setEnabled(false);
        }
        if (myUseWrapperWithVerificationButton != null) {
          myUseWrapperWithVerificationButton.setSelected(true);
        }
        break;
      case BUNDLED:
        if (myGradleHomePathField != null) {
          myGradleHomePathField.setEnabled(false);
        }
        if (myUseBundledDistributionButton != null) {
          myUseBundledDistributionButton.setSelected(true);
        }
        break;
    }
  }

  private void addGradleDelegationComponents(PaintAwarePanel content, int indentLevel) {
    if (dropDelegateBuildCombobox && dropTestRunnerCombobox) return;
    myDelegatePanel = new JPanel(new GridBagLayout());
    String title = GradleBundle.message("gradle.settings.text.delegate.panel.title");
    myDelegatePanel.setBorder(IdeBorderFactory.createTitledBorder(title, false, JBUI.insetsTop(3)));
    content.add(myDelegatePanel, ExternalSystemUiUtil.getFillLineConstraints(indentLevel + 1));
    int labelLevel = indentLevel + 1;
    if (!dropDelegateBuildCombobox) {
      BuildRunItem[] states = StreamEx.of(ThreeState.values()).map(BuildRunItem::new).toArray(BuildRunItem[]::new);
      myDelegateBuildCombobox = new ComboBox<>(states);
      myDelegateBuildCombobox.setRenderer(new MyItemCellRenderer<>());
      myDelegateBuildCombobox.setSelectedItem(new BuildRunItem(myInitialSettings.getDelegatedBuild()));

      myDelegateBuildLabel = new JBLabel(GradleBundle.message("gradle.settings.text.delegate.buildRun"));
      myDelegatePanel.add(myDelegateBuildLabel, getLabelConstraints(labelLevel));
      myDelegatePanel.add(myDelegateBuildCombobox);
    }
    if (!dropTestRunnerCombobox) {
      TestRunnerItem[] testRunners = StreamEx.of(TestRunner.values())
        .append((TestRunner)null)
        .map(TestRunnerItem::new)
        .toArray(TestRunnerItem[]::new);
      myTestRunnerCombobox = new ComboBox<>(testRunners);
      myTestRunnerCombobox.setRenderer(new MyItemCellRenderer<>());
      myTestRunnerCombobox.setSelectedItem(new TestRunnerItem(myInitialSettings.getTestRunner()));

      myTestRunnerLabel = new JBLabel(GradleBundle.message("gradle.settings.text.delegate.testRunner"));
      myDelegatePanel.add(myTestRunnerLabel, getLabelConstraints(labelLevel));
      myDelegatePanel.add(myTestRunnerCombobox);
      myDelegatePanel.add(Box.createGlue(), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }
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

  private class BuildRunItem extends MyItem<ThreeState> {

    private BuildRunItem(@Nullable ThreeState value) {
      super(value);
    }

    @Override
    protected String getText() {
      return getText(value);
    }

    @Override
    protected String getComment() {
      if (value != ThreeState.UNSURE) return null;
      ThreeState defaultDelegationOption =
        myProjectRef.isNull() ? null :
        ThreeState.fromBoolean(DefaultGradleProjectSettings.getInstance(myProjectRef.get()).isDelegatedBuild());
      return getText(defaultDelegationOption);
    }

    @NotNull
    private String getText(@Nullable ThreeState state) {
      if (state == ThreeState.NO) {
        return ApplicationNamesInfo.getInstance().getFullProductName();
      }
      if (state == ThreeState.YES) {
        return "Gradle";
      }
      return GradleBundle.message("gradle.settings.text.default");
    }
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
      if (value != null && !myProjectRef.isNull()) return null;
      TestRunner defaultRunner =
        myProjectRef.isNull() ? null : DefaultGradleProjectSettings.getInstance(myProjectRef.get()).getTestRunner();
      return getText(defaultRunner);
    }

    @NotNull
    private String getText(@Nullable TestRunner runner) {
      if (runner == TestRunner.PLATFORM) {
        return ApplicationNamesInfo.getInstance().getFullProductName();
      }
      if (runner == TestRunner.GRADLE) {
        return "Gradle";
      }
      if (runner == TestRunner.CHOOSE_PER_TEST) {
        return GradleBundle.message("gradle.preferred_test_runner.CHOOSE_PER_TEST");
      }
      return GradleBundle.message("gradle.settings.text.default");
    }
  }
}
