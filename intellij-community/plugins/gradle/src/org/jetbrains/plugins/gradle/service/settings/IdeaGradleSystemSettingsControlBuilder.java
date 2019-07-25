/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.gradle.initialization.BuildLayoutParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleBundle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.ui.Messages.getQuestionIcon;
import static org.jetbrains.plugins.gradle.service.settings.IdeaGradleProjectSettingsControlBuilder.getIDEName;

/**
 * @author Vladislav.Soroka
 */
public class IdeaGradleSystemSettingsControlBuilder implements GradleSystemSettingsControlBuilder {

  @NotNull
  private final GradleSettings myInitialSettings;

  // Used by reflection at showUi() and disposeUiResources()
  @SuppressWarnings("FieldCanBeLocal")
  @Nullable
  private JBLabel myServiceDirectoryLabel;
  private JBLabel myServiceDirectoryHint;
  @Nullable
  private TextFieldWithBrowseButton myServiceDirectoryPathField;
  private boolean dropServiceDirectory;

  @Nullable
  private JBTextField myGradleVmOptionsField;
  List<Component> myGradleVmOptionsComponents = new ArrayList<>();
  private boolean dropVmOptions;

  @Nullable
  private JBCheckBox myGenerateImlFilesCheckBox;
  private JBLabel myGenerateImlFilesHint;
  private boolean dropStoreExternallyCheckBox;

  public IdeaGradleSystemSettingsControlBuilder(@NotNull GradleSettings initialSettings) {
    myInitialSettings = initialSettings;
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {
    addServiceDirectoryControl(canvas, indentLevel);
    addVMOptionsControl(canvas, indentLevel);

    if (!dropStoreExternallyCheckBox) {
      myGenerateImlFilesCheckBox = new JBCheckBox(GradleBundle.message("gradle.settings.text.generate.iml.files"));
      canvas.add(myGenerateImlFilesCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

      myGenerateImlFilesHint = new JBLabel(
        XmlStringUtil.wrapInHtml(GradleBundle.message("gradle.settings.text.generate.iml.files.hint" , getIDEName())),
        UIUtil.ComponentStyle.SMALL);
      myGenerateImlFilesHint.setForeground(UIUtil.getLabelFontColor(UIUtil.FontColor.BRIGHTER));

      GridBag constraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel);
      constraints.insets.left += UIUtil.getCheckBoxTextHorizontalOffset(myGenerateImlFilesCheckBox);
      constraints.insets.top = 0;
      canvas.add(myGenerateImlFilesHint, constraints);
    }
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }

  @Override
  public void reset() {
    if (myServiceDirectoryPathField != null) {
      File gradleUserHomeDir = new BuildLayoutParameters().getGradleUserHomeDir();
      ((JBTextField)myServiceDirectoryPathField.getTextField()).getEmptyText().setText(gradleUserHomeDir.getPath());

      myServiceDirectoryPathField.setText(myInitialSettings.getServiceDirectoryPath());
    }

    if (myGradleVmOptionsField != null) {
      String vmOptions = trimIfPossible(myInitialSettings.getGradleVmOptions());
      myGradleVmOptionsField.setText(vmOptions);
      myGradleVmOptionsComponents.forEach(it -> {
        boolean showSetting = vmOptions != null || Registry.is("gradle.settings.showDeprecatedSettings", false);
        it.setVisible(showSetting);
      });
    }

    if (myGenerateImlFilesCheckBox != null) {
      myGenerateImlFilesCheckBox.setSelected(!myInitialSettings.getStoreProjectFilesExternally());
    }
  }

  @Override
  public boolean isModified() {
    if (myServiceDirectoryPathField != null &&
        !Comparing.equal(ExternalSystemApiUtil.normalizePath(myServiceDirectoryPathField.getText()),
                         ExternalSystemApiUtil.normalizePath(myInitialSettings.getServiceDirectoryPath()))) {
      return true;
    }

    if (myGradleVmOptionsField != null && !Comparing.equal(trimIfPossible(myGradleVmOptionsField.getText()), trimIfPossible(
      myInitialSettings.getGradleVmOptions()))) {
      return true;
    }

    if (myGenerateImlFilesCheckBox != null && myGenerateImlFilesCheckBox.isSelected() == myInitialSettings.getStoreProjectFilesExternally()) {
      return true;
    }

    return false;
  }

  @Override
  public void apply(@NotNull GradleSettings settings) {
    if (myServiceDirectoryPathField != null) {
      settings.setServiceDirectoryPath(ExternalSystemApiUtil.normalizePath(myServiceDirectoryPathField.getText()));
    }
    if (myGradleVmOptionsField != null) {
      settings.setGradleVmOptions(trimIfPossible(myGradleVmOptionsField.getText()));
    }
    if (myGenerateImlFilesCheckBox != null) {
      settings.setStoreProjectFilesExternally(!myGenerateImlFilesCheckBox.isSelected());
    }
  }

  @Override
  public boolean validate(@NotNull GradleSettings settings) {
    return true;
  }

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  @NotNull
  @Override
  public GradleSettings getInitialSettings() {
    return myInitialSettings;
  }

  public IdeaGradleSystemSettingsControlBuilder dropServiceDirectory() {
    dropServiceDirectory = true;
    return this;
  }

  public IdeaGradleSystemSettingsControlBuilder dropStoreExternallyCheckBox() {
    dropStoreExternallyCheckBox = true;
    return this;
  }

  public IdeaGradleSystemSettingsControlBuilder dropVmOptions() {
    dropVmOptions = true;
    return this;
  }

  @Deprecated
  public IdeaGradleSystemSettingsControlBuilder dropOfflineModeBox() {
    return this;
  }


  private void addServiceDirectoryControl(PaintAwarePanel canvas, int indentLevel) {
    if (dropServiceDirectory) return;

    myServiceDirectoryLabel = new JBLabel(GradleBundle.message("gradle.settings.text.user.home"));
    myServiceDirectoryHint = new JBLabel(XmlStringUtil.wrapInHtml(GradleBundle.message("gradle.settings.text.user.home.hint")),
                                         UIUtil.ComponentStyle.SMALL);
    myServiceDirectoryHint.setForeground(UIUtil.getLabelFontColor(UIUtil.FontColor.BRIGHTER));

    myServiceDirectoryPathField = new TextFieldWithBrowseButton(new JBTextField());
    myServiceDirectoryPathField.addBrowseFolderListener("", GradleBundle.message("gradle.settings.text.user.home.dialog.title"), null,
                                                        new FileChooserDescriptor(false, true, false, false, false, false),
                                                        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

    canvas.add(myServiceDirectoryLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
    canvas.add(myServiceDirectoryPathField, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

    canvas.add(Box.createGlue(), ExternalSystemUiUtil.getLabelConstraints(indentLevel));
    GridBag constraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel);
    constraints.insets.top = 0;
    canvas.add(myServiceDirectoryHint, constraints);

    myServiceDirectoryLabel.setLabelFor(myServiceDirectoryPathField);
  }

  private void addVMOptionsControl(@NotNull PaintAwarePanel canvas, int indentLevel) {
    if (!dropVmOptions) {
      JBLabel label = new JBLabel("Gradle VM options:");
      canvas.add(label, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
      myGradleVmOptionsComponents.add(label);

      myGradleVmOptionsField = new JBTextField();
      canvas.add(myGradleVmOptionsField, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
      myGradleVmOptionsComponents.add(myGradleVmOptionsField);

      label.setLabelFor(myGradleVmOptionsField);

      Component glue = Box.createGlue();
      canvas.add(glue, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
      myGradleVmOptionsComponents.add(glue);

      HyperlinkLabel fixLabel = new HyperlinkLabel();
      fixLabel.setFontSize(UIUtil.FontSize.SMALL);
      fixLabel.setForeground(UIUtil.getLabelFontColor(UIUtil.FontColor.BRIGHTER));
      fixLabel.setIcon(AllIcons.General.BalloonWarning12);
      label.setVerticalTextPosition(SwingConstants.TOP);
      GridBag constraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel);
      constraints.insets.top = 0;
      canvas.add(fixLabel, constraints);
      myGradleVmOptionsComponents.add(fixLabel);

      myGradleVmOptionsField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull DocumentEvent e) {
          boolean showMigration = e.getDocument().getLength() > 0;
          fixLabel.setHyperlinkText(
            "This setting is deprecated, please use 'org.gradle.jvmargs’ property in 'gradle.properties’ file instead ",
            showMigration ? "Migrate" : "  ", "");
        }
      });
      myGradleVmOptionsField.setText(" "); // trigger listener

      fixLabel.addHyperlinkListener(new HyperlinkAdapter() {
        @Override
        protected void hyperlinkActivated(HyperlinkEvent e) {
          String jvmArgs = myGradleVmOptionsField.getText().trim();
          if (jvmArgs.isEmpty()) return;

          if (moveVMOptionsToGradleProperties(jvmArgs, myInitialSettings)) {
            myGradleVmOptionsField.setText(null);
            myGradleVmOptionsField.getEmptyText().setText("VM options have been moved to gradle.properties");
          }
        }
      });
    }
  }

  @Nullable
  private static String trimIfPossible(@Nullable String s) {
    return StringUtil.nullize(StringUtil.trim(s));
  }

  private boolean moveVMOptionsToGradleProperties(@NotNull String vmOptions, @NotNull GradleSettings settings) {
    File gradleUserHomeDir = new BuildLayoutParameters().getGradleUserHomeDir();
    if (myServiceDirectoryPathField != null) {
      String fieldText = trimIfPossible(myServiceDirectoryPathField.getText());
      if (fieldText != null) gradleUserHomeDir = new File(fieldText);
    }

    int result = Messages.showYesNoDialog(
      settings.getProject(),
      "Would you like to move VM options to the '" + new File(gradleUserHomeDir, "gradle.properties") + "' file?\n" +
      "Note that the existing 'org.gradle.jvmargs' property will be overwritten.\n\n" +
      "You can do it manually any time later", "Gradle Settings",
      getQuestionIcon());
    if (result != Messages.YES) return false;

    try {
      // get or create project dir
      if (!gradleUserHomeDir.exists()) {
        if (!FileUtil.createDirectory(gradleUserHomeDir)) {
          throw new IOException("Cannot create " + gradleUserHomeDir);
        }
      }

      // get or create project's gradle.properties
      File props = new File(gradleUserHomeDir, "gradle.properties");
      if (props.isDirectory()) throw new IOException(props.getPath() + " is a directory");

      String original = props.exists() ? FileUtil.loadFile(props) : "";
      String updated = updateVMOptions(original, vmOptions);
      if (!original.equals(updated)) {
        FileUtil.writeToFile(props, updated);
      }
    }
    catch (IOException e) {
      Messages.showErrorDialog(settings.getProject(),
                               e.getMessage() + "\n\nPlease migrate settings manually",
                               "Migration Error");
      return false;
    }

    return true;
  }

  private static final Pattern VM_OPTIONS_REGEX = Pattern.compile("^(\\s*\"?org\\.gradle\\.jvmargs\"?\\s*[=:]).*?(?<!\\\\)($)",
                                                                  Pattern.MULTILINE | Pattern.DOTALL);

  @NotNull
  public static String updateVMOptions(@NotNull String originalText, @NotNull String vmOptions) {
    Matcher matcher = VM_OPTIONS_REGEX.matcher(originalText);

    StringBuffer result = new StringBuffer(originalText.length() + vmOptions.length());

    String escapedValue = StringUtil.escapeProperty(vmOptions, false);
    if (matcher.find()) {
      matcher.appendReplacement(result, "$1" + Matcher.quoteReplacement(escapedValue) + "$2");
      matcher.appendTail(result);
    }
    else {
      result.append(originalText);
      if (!originalText.isEmpty() && !originalText.endsWith("\n")) result.append("\n");
      result.append("org.gradle.jvmargs=").append(escapedValue).append("\n");
    }
    return result.toString();
  }
}
