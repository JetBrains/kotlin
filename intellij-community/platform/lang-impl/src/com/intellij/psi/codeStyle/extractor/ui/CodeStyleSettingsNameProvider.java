// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.extractor.ui;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider.SettingsType;
import com.intellij.psi.codeStyle.extractor.values.Value;
import com.intellij.psi.codeStyle.presentation.CodeStyleSelectSettingPresentation;
import com.intellij.psi.codeStyle.presentation.CodeStyleSettingPresentation;
import com.intellij.psi.codeStyle.presentation.CodeStyleSettingPresentation.SettingsGroup;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Roman.Shein
 */
public class CodeStyleSettingsNameProvider implements CodeStyleSettingsCustomizable {

  protected Map<SettingsType, Map<SettingsGroup, List<CodeStyleSettingPresentation>>> mySettings =
    new HashMap<>();
  private final Map<SettingsType, Map<SettingsGroup, List<CodeStyleSettingPresentation>>> standardSettings =
    new HashMap<>();

  public CodeStyleSettingsNameProvider() {
    for (SettingsType settingsType : SettingsType.values()) {
      standardSettings.put(settingsType, CodeStyleSettingPresentation.getStandardSettings(settingsType));
    }
  }

  protected void addSetting(@NotNull SettingsGroup group, @NotNull CodeStyleSettingPresentation setting, @Nullable OptionAnchor anchor,
                            @Nullable String anchorFieldName) {
    for (Map.Entry<SettingsType, Map<SettingsGroup, List<CodeStyleSettingPresentation>>> entry: mySettings.entrySet()) {
      if (entry.getValue().containsKey(group)) {
        addSetting(entry.getKey(), group, setting, anchor, anchorFieldName);
        return;
      }
    }
    addSetting(SettingsType.LANGUAGE_SPECIFIC, group, setting, anchor, anchorFieldName);
  }

  protected void addSetting(@NotNull SettingsType settingsType, @NotNull SettingsGroup group, @NotNull CodeStyleSettingPresentation setting,
                            @Nullable OptionAnchor anchor, @Nullable String anchorFieldName) {
    Map<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>> groups = mySettings.get(settingsType);
    if (groups == null) {
      groups = new LinkedHashMap<>();
    }
    List<CodeStyleSettingPresentation> settingsList = groups.get(group);
    if (settingsList == null) {
      settingsList = new LinkedList<>();
    }
    if (settingsList.contains(setting)) return;
    if (anchor != null && anchorFieldName != null) {
      CodeStyleSettingPresentation anchorSettingRepresentation = new CodeStyleSettingPresentation(anchorFieldName, anchorFieldName);
      int insertIndex = settingsList.indexOf(anchorSettingRepresentation);
      if (insertIndex < 0) {
        insertIndex = settingsList.size();
      } else {
        switch (anchor) {
          case BEFORE:
            break;
          case AFTER:
            insertIndex++;
            break;
          case NONE:
            insertIndex = settingsList.size();
        }
      }
      settingsList.add(insertIndex, setting);
    } else {
      settingsList.add(setting);
    }
    groups.put(group, settingsList);
  }

  @Override
  public void showAllStandardOptions() {
    for (SettingsType settingsType : SettingsType.values()) {
      Map<SettingsGroup, List<CodeStyleSettingPresentation>> standardGroups = standardSettings.get(settingsType);
      for (Map.Entry<SettingsGroup, List<CodeStyleSettingPresentation>> entry : standardGroups.entrySet()) {
        for (CodeStyleSettingPresentation setting: entry.getValue()) {
          addSetting(settingsType, entry.getKey(), setting, null, null);
        }
      }
    }
  }

  @Override
  public void showStandardOptions(String... optionNames) {
    List<String> options = Arrays.asList(optionNames);
    for (SettingsType settingsType : SettingsType.values()) {
      Map<SettingsGroup, List<CodeStyleSettingPresentation>> standardGroups = standardSettings.get(settingsType);
      for (Map.Entry<SettingsGroup, List<CodeStyleSettingPresentation>> entry : standardGroups.entrySet()) {
        for (CodeStyleSettingPresentation setting: entry.getValue()) {
          if (options.contains(setting.getFieldName())) {
            addSetting(settingsType, entry.getKey(), setting, null, null);
          }
        }
      }
    }
  }

  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass, @NotNull String fieldName, @NotNull String title, @Nullable String groupName, Object... options) {
    showCustomOption(settingsClass, fieldName, title, groupName, null, null, options);
  }

  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass, @NotNull String fieldName, @NotNull String title,
                               @Nullable String groupName, @Nullable OptionAnchor anchor, @Nullable String anchorFieldName, Object... options) {
    if (options.length == 2) {
      addSetting(new SettingsGroup(groupName), new CodeStyleSelectSettingPresentation(fieldName, title, (int[])options[1],
                                                                                        (String[])options[0]), anchor, anchorFieldName);
    } else {
      addSetting(new SettingsGroup(groupName), new CodeStyleSettingPresentation(fieldName, title), anchor, anchorFieldName);
    }
  }

  @Override
  public void renameStandardOption(String fieldName, String newTitle) {
    for (SettingsType settingsType : SettingsType.values()) {
      Map<SettingsGroup, List<CodeStyleSettingPresentation>> standardGroups = mySettings.get(settingsType);
      if (standardGroups == null) {
        continue;
      }
      for (Map.Entry<SettingsGroup, List<CodeStyleSettingPresentation>> entry : standardGroups.entrySet()) {
        for (CodeStyleSettingPresentation setting: entry.getValue()) {
          if (setting.getFieldName().equals(fieldName)) {
            setting.setUiName(newTitle);
            return;
          }
        }
      }
    }
  }

  @Override
  public void moveStandardOption(String fieldName, String newGroup) {
    for (SettingsType settingsType : SettingsType.values()) {
      Map<SettingsGroup, List<CodeStyleSettingPresentation>> standardGroups = mySettings.get(settingsType);
      if (standardGroups == null) {
        standardGroups = new LinkedHashMap<>();
        mySettings.put(settingsType, standardGroups);
      }
      for (Map.Entry<SettingsGroup, List<CodeStyleSettingPresentation>> entry : standardGroups.entrySet()) {
        CodeStyleSettingPresentation moveSetting = null;
        for (CodeStyleSettingPresentation setting: entry.getValue()) {
          if (setting.getFieldName().equals(fieldName)) {
            moveSetting = setting;
            break;
          }
        }
        if (moveSetting != null) {
          entry.getValue().remove(moveSetting);
          addSetting(new SettingsGroup(newGroup), moveSetting, null, null);
        }
      }
    }
  }

  public static String getSettingsTypeName(LanguageCodeStyleSettingsProvider.SettingsType settingsType) {
    switch (settingsType) {
      case BLANK_LINES_SETTINGS: return ApplicationBundle.message("title.blank.lines");
      case SPACING_SETTINGS: return ApplicationBundle.message("title.spaces");
      case WRAPPING_AND_BRACES_SETTINGS: return ApplicationBundle.message("wrapping.and.braces");
      case INDENT_SETTINGS: return ApplicationBundle.message("title.tabs.and.indents");
      case LANGUAGE_SPECIFIC: return "Language-specific"; //TODO should load from ApplciationBundle here
      default: throw new IllegalArgumentException("Unknown settings type: " + settingsType);
    }
  }

  public void addSettings(LanguageCodeStyleSettingsProvider provider) {
    for (SettingsType settingsType : LanguageCodeStyleSettingsProvider.SettingsType.values()) {
      provider.customizeSettings(this, settingsType);
    }
  }

  public static Value getValue(final CodeStyleSettingPresentation representation, List<? extends Value> values) {
    Value myValue = ContainerUtil.find(values, value -> {
      return value.state == Value.STATE.SELECTED && value.name.equals(representation.getFieldName());
      //return value.name.equals(representation.getFieldName()); //TODO this is here only to test the UI!!
    });
    return myValue;
  }

  public String getSettings(List<? extends Value> values) {
    StringBuilder builder = new StringBuilder();
    for (SettingsType settingsType : LanguageCodeStyleSettingsProvider.SettingsType.values()) {
      builder.append("<br><b><u>").append(getSettingsTypeName(settingsType)).append("</u></b>");
      Map<SettingsGroup, List<CodeStyleSettingPresentation>> groups = mySettings.get(settingsType);
      if (groups != null) {
        for (Map.Entry<SettingsGroup, List<CodeStyleSettingPresentation>> entry : groups.entrySet()) {
          boolean firstSettingGroupTop = entry.getKey().isNull();
          boolean groupReported = false;
          for (final CodeStyleSettingPresentation setting : entry.getValue()) {
            Value myValue = ContainerUtil.find(values,
                                               value -> value.state == Value.STATE.SELECTED && value.name.equals(setting.getFieldName()));
            if (myValue == null) {
              continue;
            }
            if (!groupReported) {
              if (firstSettingGroupTop) {
                builder.append("<b>");
              } else {
                builder.append("<br><b>").append(entry.getKey().name).append("</b>");
              }
            }
            builder.append("<br>");
            String postNameSign = setting.getUiName().endsWith(":") ?  " " : ": ";
            builder.append(setting.getUiName()).append(postNameSign).append(setting.getValueUiName(myValue.value));
            if (!groupReported) {
              if (firstSettingGroupTop) {
                builder.append("</b>");
              }
            }
            groupReported = true;
          }
        }
      }
    }
    return builder.toString();
  }
}
