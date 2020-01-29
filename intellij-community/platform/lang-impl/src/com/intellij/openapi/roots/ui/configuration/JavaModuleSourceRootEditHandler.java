// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.ui.DarculaColors;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class JavaModuleSourceRootEditHandler extends JavaSourceRootEditHandlerBase {
  private static final Color SOURCES_COLOR = new JBColor(new Color(0x0A50A1), DarculaColors.BLUE);

  public JavaModuleSourceRootEditHandler() {
    super(JavaSourceRootType.SOURCE);
  }

  @NotNull
  @Override
  public String getRootTypeName() {
    return ProjectBundle.message("module.toggle.sources.action");
  }

  @NotNull
  @Override
  public String getRootsGroupTitle() {
    return ProjectBundle.message("module.paths.sources.group");
  }

  @NotNull
  @Override
  public Icon getRootIcon() {
    return AllIcons.Modules.SourceRoot;
  }

  @NotNull
  @Override
  protected Icon getGeneratedRootIcon() {
    return AllIcons.Modules.GeneratedSourceRoot;
  }

  @Nullable
  @Override
  public Icon getFolderUnderRootIcon() {
    return AllIcons.Nodes.Package;
  }

  @Override
  public CustomShortcutSet getMarkRootShortcutSet() {
    return new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));
  }

  @NotNull
  @Override
  public Color getRootsGroupColor() {
    return SOURCES_COLOR;
  }

  @NotNull
  @Override
  public String getUnmarkRootButtonText() {
    return ProjectBundle.message("module.paths.unmark.source.tooltip");
  }
}
