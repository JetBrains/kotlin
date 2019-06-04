// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.options;

import com.intellij.codeInsight.NullableNotNullDialog;
import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.compiler.MalformedPatternException;
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfiguration;
import com.intellij.compiler.server.BuildManager;
import com.intellij.ide.PowerSaveMode;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.List;
import java.util.*;

import static com.intellij.compiler.options.CompilerOptionsFilter.Setting;

public class CompilerUIConfigurable implements SearchableConfigurable, Configurable.NoScroll {
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.options.CompilerUIConfigurable");

  private static final Set<Setting> EXTERNAL_BUILD_SETTINGS = EnumSet.of(
    Setting.EXTERNAL_BUILD, Setting.AUTO_MAKE, Setting.PARALLEL_COMPILATION, Setting.REBUILD_MODULE_ON_DEPENDENCY_CHANGE,
    Setting.HEAP_SIZE, Setting.COMPILER_VM_OPTIONS
  );

  private final Set<Setting> myDisabledSettings = EnumSet.noneOf(Setting.class);

  private       JPanel  myPanel;
  private final Project myProject;

  private RawCommandLineEditor myResourcePatternsField;
  private JCheckBox            myCbClearOutputDirectory;
  private JCheckBox            myCbAssertNotNull;
  private JBLabel              myPatternLegendLabel;
  private JCheckBox            myCbAutoShowFirstError;
  private JCheckBox            myCbDisplayNotificationPopup;
  private JCheckBox            myCbEnableAutomake;
  private JCheckBox            myCbParallelCompilation;
  private JTextField           myHeapSizeField;
  private JTextField           mySharedVMOptionsField;
  private JTextField           myVMOptionsField;
  private JLabel               myHeapSizeLabel;
  private JLabel               mySharedVMOptionsLabel;
  private JLabel               myVMOptionsLabel;
  private JCheckBox            myCbRebuildOnDependencyChange;
  private JLabel               myResourcePatternsLabel;
  private JLabel               myEnableAutomakeLegendLabel;
  private JLabel               myParallelCompilationLegendLabel;
  private JButton              myConfigureAnnotations;
  private JLabel myWarningLabel;
  private JPanel myAssertNotNullPanel;

  public CompilerUIConfigurable(@NotNull final Project project) {
    myProject = project;

    myPatternLegendLabel.setText(XmlStringUtil.wrapInHtml(
                                   "Use <b>;</b> to separate patterns and <b>!</b> to negate a pattern. " +
                                   "Accepted wildcards: <b>?</b> &mdash; exactly one symbol; <b>*</b> &mdash; zero or more symbols; " +
                                   "<b>/</b> &mdash; path separator; <b>/**/</b> &mdash; any number of directories; " +
                                   "<i>&lt;dir_name&gt;</i>:<i>&lt;pattern&gt;</i> &mdash; restrict to source roots with the specified name"
    ));
    myWarningLabel.setText("<html>WARNING!<br>" +
                                              /*"All source files located in the generated sources output directory WILL BE EXCLUDED from annotation processing. " +*/
                                              "If option 'Clear output directory on rebuild' is enabled, " +
                                              "the entire contents of directories where generated sources are stored WILL BE CLEARED on rebuild.</html>");
    myWarningLabel.setFont(myWarningLabel.getFont().deriveFont(Font.BOLD));

    myPatternLegendLabel.setForeground(new JBColor(Gray._50, Gray._130));
    tweakControls(project);
    myVMOptionsField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        mySharedVMOptionsField.setEnabled(e.getDocument().getLength() == 0);
        myHeapSizeField.setEnabled(ContainerUtil.find(ParametersListUtil.parse(myVMOptionsField.getText()),
                                                      s -> StringUtil.startsWithIgnoreCase(s, "-Xmx")) == null);
      }
    });
    myConfigureAnnotations.addActionListener(e -> {
      NullableNotNullDialog.showDialogWithInstrumentationOptions(myPanel);
      myCbAssertNotNull.setSelected(!NullableNotNullManager.getInstance(myProject).getInstrumentedNotNulls().isEmpty());
    });
  }

  private void tweakControls(@NotNull Project project) {
    CompilerOptionsFilter[] managers = CompilerOptionsFilter.EP_NAME.getExtensions();
    boolean showExternalBuildSetting = true;
    for (CompilerOptionsFilter manager : managers) {
      showExternalBuildSetting = manager.isAvailable(Setting.EXTERNAL_BUILD, project);
      if (!showExternalBuildSetting) {
        myDisabledSettings.add(Setting.EXTERNAL_BUILD);
        break;
      }
    }

    for (Setting setting : Setting.values()) {
      if (!showExternalBuildSetting && EXTERNAL_BUILD_SETTINGS.contains(setting)) {
        // Disable all nested external compiler settings if 'use external build' is unavailable.
        myDisabledSettings.add(setting);
      }
      else {
        for (CompilerOptionsFilter manager : managers) {
          if (!manager.isAvailable(setting, project)) {
            myDisabledSettings.add(setting);
            break;
          }
        }
      }
    }

    Map<Setting, Collection<JComponent>> controls = new HashMap<>();
    controls.put(Setting.RESOURCE_PATTERNS, ContainerUtilRt.newArrayList(myResourcePatternsLabel, myResourcePatternsField, myPatternLegendLabel));
    controls.put(Setting.CLEAR_OUTPUT_DIR_ON_REBUILD, Collections.singleton(myCbClearOutputDirectory));
    controls.put(Setting.ADD_NOT_NULL_ASSERTIONS, Collections.singleton(myAssertNotNullPanel));
    controls.put(Setting.AUTO_SHOW_FIRST_ERROR_IN_EDITOR, Collections.singleton(myCbAutoShowFirstError));
    controls.put(Setting.DISPLAY_NOTIFICATION_POPUP, Collections.singleton(myCbDisplayNotificationPopup));
    controls.put(Setting.AUTO_MAKE, ContainerUtilRt.newArrayList(myCbEnableAutomake, myEnableAutomakeLegendLabel));
    controls.put(Setting.PARALLEL_COMPILATION, ContainerUtilRt.newArrayList(myCbParallelCompilation, myParallelCompilationLegendLabel));
    controls.put(Setting.REBUILD_MODULE_ON_DEPENDENCY_CHANGE, ContainerUtilRt.newArrayList(myCbRebuildOnDependencyChange));
    controls.put(Setting.HEAP_SIZE, ContainerUtilRt.newArrayList(myHeapSizeLabel, myHeapSizeField));
    controls.put(Setting.COMPILER_VM_OPTIONS, ContainerUtilRt.newArrayList(myVMOptionsLabel, myVMOptionsField, mySharedVMOptionsLabel, mySharedVMOptionsField));

    for (Setting setting : myDisabledSettings) {
      Collection<JComponent> components = controls.get(setting);
      if (components != null) {
        for (JComponent component : components) {
          component.setVisible(false);
        }
      }
    }
  }

  @Override
  public void reset() {

    final CompilerConfigurationImpl configuration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    final CompilerWorkspaceConfiguration workspaceConfiguration = CompilerWorkspaceConfiguration.getInstance(myProject);
    myCbAutoShowFirstError.setSelected(workspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
    myCbDisplayNotificationPopup.setSelected(workspaceConfiguration.DISPLAY_NOTIFICATION_POPUP);
    myCbClearOutputDirectory.setSelected(workspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
    myCbAssertNotNull.setSelected(configuration.isAddNotNullAssertions());
    myCbEnableAutomake.setSelected(workspaceConfiguration.MAKE_PROJECT_ON_SAVE);
    myCbParallelCompilation.setSelected(workspaceConfiguration.PARALLEL_COMPILATION);
    myCbRebuildOnDependencyChange.setSelected(workspaceConfiguration.REBUILD_ON_DEPENDENCY_CHANGE);
    final int javacPreferred = JavacConfiguration.getOptions(myProject, JavacConfiguration.class).MAXIMUM_HEAP_SIZE; // for compatibility with older projects
    myHeapSizeField.setText(String.valueOf(configuration.getBuildProcessHeapSize(javacPreferred)));
    final String options = workspaceConfiguration.COMPILER_PROCESS_ADDITIONAL_VM_OPTIONS;
    myVMOptionsField.setText(options == null ? "" : options.trim());
    mySharedVMOptionsField.setText(configuration.getBuildProcessVMOptions());

    configuration.convertPatterns();

    myResourcePatternsField.setText(patternsToString(configuration.getResourceFilePatterns()));

    if (PowerSaveMode.isEnabled()) {
      myEnableAutomakeLegendLabel.setText("(disabled in Power Save mode)");
      myEnableAutomakeLegendLabel.setFont(myEnableAutomakeLegendLabel.getFont().deriveFont(Font.BOLD));
    }
    else {
      myEnableAutomakeLegendLabel.setText("(only works while not running / debugging)");
      myEnableAutomakeLegendLabel.setFont(myEnableAutomakeLegendLabel.getFont().deriveFont(Font.PLAIN));
    }
  }

  private static String patternsToString(final String[] patterns) {
    final StringBuilder extensionsString = new StringBuilder();
    for (int idx = 0; idx < patterns.length; idx++) {
      if (idx > 0) {
        extensionsString.append(";");
      }
      extensionsString.append(patterns[idx]);
    }
    return extensionsString.toString();
  }

  @Override
  public void apply() throws ConfigurationException {

    CompilerConfigurationImpl configuration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    final CompilerWorkspaceConfiguration workspaceConfiguration = CompilerWorkspaceConfiguration.getInstance(myProject);
    if (!myDisabledSettings.contains(Setting.AUTO_SHOW_FIRST_ERROR_IN_EDITOR)) {
      workspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR = myCbAutoShowFirstError.isSelected();
    }
    if (!myDisabledSettings.contains(Setting.DISPLAY_NOTIFICATION_POPUP)) {
      workspaceConfiguration.DISPLAY_NOTIFICATION_POPUP = myCbDisplayNotificationPopup.isSelected();
    }
    if (!myDisabledSettings.contains(Setting.CLEAR_OUTPUT_DIR_ON_REBUILD)) {
      workspaceConfiguration.CLEAR_OUTPUT_DIRECTORY = myCbClearOutputDirectory.isSelected();
    }
    if (!myDisabledSettings.contains(Setting.EXTERNAL_BUILD)) {
      if (!myDisabledSettings.contains(Setting.AUTO_MAKE)) {
        workspaceConfiguration.MAKE_PROJECT_ON_SAVE = myCbEnableAutomake.isSelected();
      }
      if (!myDisabledSettings.contains(Setting.PARALLEL_COMPILATION)) {
        workspaceConfiguration.PARALLEL_COMPILATION = myCbParallelCompilation.isSelected();
      }
      if (!myDisabledSettings.contains(Setting.REBUILD_MODULE_ON_DEPENDENCY_CHANGE)) {
        workspaceConfiguration.REBUILD_ON_DEPENDENCY_CHANGE = myCbRebuildOnDependencyChange.isSelected();
      }
      if (!myDisabledSettings.contains(Setting.HEAP_SIZE)) {
        try {
          configuration.setBuildProcessHeapSize(Integer.parseInt(myHeapSizeField.getText().trim()));
        }
        catch (NumberFormatException ignored) {
          LOG.info(ignored);
        }
      }
      if (!myDisabledSettings.contains(Setting.COMPILER_VM_OPTIONS)) {
        workspaceConfiguration.COMPILER_PROCESS_ADDITIONAL_VM_OPTIONS = myVMOptionsField.getText().trim();
        configuration.setBuildProcessVMOptions(mySharedVMOptionsField.getText().trim());
      }
    }

    if (!myDisabledSettings.contains(Setting.ADD_NOT_NULL_ASSERTIONS)) {
      configuration.setAddNotNullAssertions(myCbAssertNotNull.isSelected());
    }
    if (!myDisabledSettings.contains(Setting.RESOURCE_PATTERNS)) {
      configuration.removeResourceFilePatterns();
      String extensionString = myResourcePatternsField.getText().trim();
      applyResourcePatterns(extensionString, configuration);
    }

    BuildManager.getInstance().clearState(myProject);
  }

  public static void applyResourcePatterns(String extensionString, final CompilerConfigurationImpl configuration)
    throws ConfigurationException {
    StringTokenizer tokenizer = new StringTokenizer(extensionString, ";", false);
    List<String[]> errors = new ArrayList<>();

    while (tokenizer.hasMoreTokens()) {
      String namePattern = tokenizer.nextToken();
      try {
        configuration.addResourceFilePattern(namePattern);
      }
      catch (MalformedPatternException e) {
        errors.add(new String[]{namePattern, e.getLocalizedMessage()});
      }
    }

    if (errors.size() > 0) {
      final StringBuilder pattersnsWithErrors = new StringBuilder();
      for (final Object error : errors) {
        String[] pair = (String[])error;
        pattersnsWithErrors.append("\n");
        pattersnsWithErrors.append(pair[0]);
        pattersnsWithErrors.append(": ");
        pattersnsWithErrors.append(pair[1]);
      }

      throw new ConfigurationException(
        CompilerBundle.message("error.compiler.configurable.malformed.patterns", pattersnsWithErrors.toString()), CompilerBundle.message("bad.resource.patterns.dialog.title")
      );
    }
  }

  @Override
  public boolean isModified() {
    final CompilerWorkspaceConfiguration workspaceConfiguration = CompilerWorkspaceConfiguration.getInstance(myProject);
    boolean isModified = !myDisabledSettings.contains(Setting.AUTO_SHOW_FIRST_ERROR_IN_EDITOR)
                         && ComparingUtils.isModified(myCbAutoShowFirstError, workspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
    isModified |= !myDisabledSettings.contains(Setting.DISPLAY_NOTIFICATION_POPUP)
                  && ComparingUtils.isModified(myCbDisplayNotificationPopup, workspaceConfiguration.DISPLAY_NOTIFICATION_POPUP);
    isModified |= !myDisabledSettings.contains(Setting.AUTO_MAKE)
                  && ComparingUtils.isModified(myCbEnableAutomake, workspaceConfiguration.MAKE_PROJECT_ON_SAVE);
    isModified |= !myDisabledSettings.contains(Setting.PARALLEL_COMPILATION)
                  && ComparingUtils.isModified(myCbParallelCompilation, workspaceConfiguration.PARALLEL_COMPILATION);
    isModified |= !myDisabledSettings.contains(Setting.REBUILD_MODULE_ON_DEPENDENCY_CHANGE)
                  && ComparingUtils.isModified(myCbRebuildOnDependencyChange, workspaceConfiguration.REBUILD_ON_DEPENDENCY_CHANGE);
    isModified |= !myDisabledSettings.contains(Setting.COMPILER_VM_OPTIONS)
                  && ComparingUtils.isModified(myVMOptionsField, workspaceConfiguration.COMPILER_PROCESS_ADDITIONAL_VM_OPTIONS);

    final CompilerConfigurationImpl compilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    isModified |= !myDisabledSettings.contains(Setting.HEAP_SIZE)
                  && ComparingUtils.isModified(myHeapSizeField, compilerConfiguration.getBuildProcessHeapSize(0));
    isModified |= !myDisabledSettings.contains(Setting.COMPILER_VM_OPTIONS)
                  && ComparingUtils.isModified(mySharedVMOptionsField, compilerConfiguration.getBuildProcessVMOptions());
    isModified |= !myDisabledSettings.contains(Setting.ADD_NOT_NULL_ASSERTIONS)
                  && ComparingUtils.isModified(myCbAssertNotNull, compilerConfiguration.isAddNotNullAssertions());
    isModified |= !myDisabledSettings.contains(Setting.CLEAR_OUTPUT_DIR_ON_REBUILD)
                  && ComparingUtils.isModified(myCbClearOutputDirectory, workspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
    isModified |= !myDisabledSettings.contains(Setting.RESOURCE_PATTERNS)
                  && ComparingUtils.isModified(myResourcePatternsField, patternsToString(compilerConfiguration.getResourceFilePatterns()));

    return isModified;
  }

  @Override
  public String getDisplayName() {
    return "General";
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  @NotNull
  public String getId() {
    return "compiler.general";
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  private void createUIComponents() {
    myResourcePatternsField = new RawCommandLineEditor(ParametersListUtil.COLON_LINE_PARSER, ParametersListUtil.COLON_LINE_JOINER);
    myResourcePatternsField.setDialogCaption("Resource patterns");
    myHeapSizeField = new JTextField();
    myHeapSizeField.setPreferredSize(new Dimension(JBUIScale.scale(50), myHeapSizeField.getPreferredSize().height));
  }
}
