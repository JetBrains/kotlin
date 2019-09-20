// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tabs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.FileColorManager;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public final class FileColorManagerImpl extends FileColorManager {
  public static final String FC_ENABLED = "FileColorsEnabled";
  public static final String FC_TABS_ENABLED = "FileColorsForTabsEnabled";
  public static final String FC_PROJECT_VIEW_ENABLED = "FileColorsForProjectViewEnabled";
  private final Project myProject;
  private final FileColorsModel myModel;

  private final NotNullLazyValue<FileColorsModel> myInitializedModel;

  private static final Map<String, Color> ourDefaultColors = ContainerUtil.<String, Color>immutableMapBuilder()
    .put("Blue", JBColor.namedColor("FileColor.Blue", new JBColor(0xeaf6ff, 0x4f556b)))
    .put("Green", JBColor.namedColor("FileColor.Green", new JBColor(0xeffae7, 0x49544a)))
    .put("Orange", JBColor.namedColor("FileColor.Orange", new JBColor(0xf6e9dc, 0x806052)))
    .put("Rose", JBColor.namedColor("FileColor.Rose", new JBColor(0xf2dcda, 0x6e535b)))
    .put("Violet", JBColor.namedColor("FileColor.Violet", new JBColor(0xe6e0f1, 0x534a57)))
    .put("Yellow", JBColor.namedColor("FileColor.Yellow", new JBColor(0xffffe4, 0x4f4b41)))
    .build();

  public FileColorManagerImpl(@NotNull Project project) {
    myProject = project;
    myModel = new FileColorsModel(project);

    myInitializedModel = NotNullLazyValue.createValue(() -> {
      project.getService(PerTeamFileColorModelStorageManager.class);
      project.getService(PerUserFileColorModelStorageManager.class);
      return myModel;
    });
  }

  @Override
  public boolean isEnabled() {
    return _isEnabled();
  }

  public static boolean _isEnabled() {
    return PropertiesComponent.getInstance().getBoolean(FC_ENABLED, true);
  }

  @Override
  public void setEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(FC_ENABLED, enabled, true);
  }

  public void setEnabledForTabs(boolean enabled) {
    PropertiesComponent.getInstance().setValue(FC_TABS_ENABLED, Boolean.toString(enabled));
  }

  @Override
  public boolean isEnabledForTabs() {
    return _isEnabledForTabs();
  }

  public static boolean _isEnabledForTabs() {
    return PropertiesComponent.getInstance().getBoolean(FC_TABS_ENABLED, true);
  }

  @Override
  public boolean isEnabledForProjectView() {
    return _isEnabledForProjectView();
  }

  public static boolean _isEnabledForProjectView() {
    return PropertiesComponent.getInstance().getBoolean(FC_PROJECT_VIEW_ENABLED, true);
  }

  public static void setEnabledForProjectView(boolean enabled) {
    PropertiesComponent.getInstance().setValue(FC_PROJECT_VIEW_ENABLED, Boolean.toString(enabled));
  }

  @Override
  @Nullable
  public Color getColor(@NotNull String name) {
    Color color = ourDefaultColors.get(name);
    return color == null ? ColorUtil.fromHex(name, null) : color;
  }

  @Override
  public Collection<String> getColorNames() {
    List<String> sorted = new ArrayList<>(ourDefaultColors.keySet());
    Collections.sort(sorted);
    return sorted;
  }

  @Nullable
  @Override
  public Color getRendererBackground(VirtualFile vFile) {
    if (vFile == null) return null;

    if (isEnabled()) {
      final Color fileColor = getFileColor(vFile);
      if (fileColor != null) return fileColor;
    }

    //return FileEditorManager.getInstance(myProject).isFileOpen(vFile) && !UIUtil.isUnderDarcula() ? LightColors.SLIGHTLY_GREEN : null;
    return null;
  }

  @Nullable
  @Override
  public Color getRendererBackground(PsiFile file) {
    if (file == null) return null;

    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) return null;

    return getRendererBackground(vFile);
  }

  @Override
  public void addScopeColor(@NotNull String scopeName, @NotNull String colorName, boolean isProjectLevel) {
    myInitializedModel.getValue().add(scopeName, colorName, isProjectLevel);
  }

  @Override
  @Nullable
  public Color getFileColor(@NotNull VirtualFile file) {
    String colorName = myInitializedModel.getValue().getColor(file, getProject());
    return colorName == null ? null : getColor(colorName);
  }

  @Override
  @Nullable
  public Color getScopeColor(@NotNull String scopeName) {
    String colorName = myInitializedModel.getValue().getScopeColor(scopeName, getProject());
    return colorName == null ? null : getColor(colorName);
  }

  @Override
  public boolean isShared(@NotNull String scopeName) {
    return myInitializedModel.getValue().isProjectLevel(scopeName);
  }

  @NotNull
  FileColorsModel getModel() {
    return myInitializedModel.getValue();
  }

  @NotNull
  FileColorsModel getUninitializedModel() {
    return myModel;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @NotNull
  public List<FileColorConfiguration> getApplicationLevelConfigurations() {
    return myInitializedModel.getValue().getLocalConfigurations();
  }

  public List<FileColorConfiguration> getProjectLevelConfigurations() {
    return myInitializedModel.getValue().getProjectLevelConfigurations();
  }

  @Nullable
  public static String getColorName(@NotNull Color color) {
    for (String name : ourDefaultColors.keySet()) {
      if (color.equals(ourDefaultColors.get(name))) {
        return name;
      }
    }
    return null;
  }

  static String getAlias(String text) {
    return StartupUiUtil.isUnderDarcula() && text.equals("Yellow") ? "Brown" : text;
  }
}
