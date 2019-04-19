/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * @author nik
 */
public class JavaTestSourceRootEditHandler extends JavaSourceRootEditHandlerBase {
  private static final Color TESTS_COLOR = new JBColor(new Color(0x008C2E), new Color(73, 140, 101));

  public JavaTestSourceRootEditHandler() {
    super(JavaSourceRootType.TEST_SOURCE);
  }

  @NotNull
  @Override
  public String getRootTypeName() {
    return ProjectBundle.message("module.toggle.test.sources.action");
  }

  @NotNull
  @Override
  public String getRootsGroupTitle() {
    return ProjectBundle.message("module.paths.test.sources.group");
  }

  @NotNull
  @Override
  public Icon getRootIcon() {
    return AllIcons.Modules.TestRoot;
  }

  @NotNull
  @Override
  protected Icon getGeneratedRootIcon() {
    return AllIcons.Modules.GeneratedTestRoot;
  }

  @Nullable
  @Override
  public Icon getFolderUnderRootIcon() {
    return AllIcons.Modules.SourceFolder;
  }

  @Override
  public CustomShortcutSet getMarkRootShortcutSet() {
    return new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.ALT_MASK));
  }

  @NotNull
  @Override
  public Color getRootsGroupColor() {
    return TESTS_COLOR;
  }

  @NotNull
  @Override
  public String getMarkRootButtonText() {
    return "Tests";
  }

  @NotNull
  @Override
  public String getUnmarkRootButtonText() {
    return ProjectBundle.message("module.paths.unmark.tests.tooltip");
  }
}
