/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.colors.pluginExport;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PluginInfoDialog extends DialogWrapper {
  private final PluginExportData myExportData;
  private PluginInfoForm myForm;

  protected PluginInfoDialog(@NotNull Component parent, @NotNull PluginExportData exportData) {
    super(parent, false);
    myExportData = exportData;
    setTitle("Create Color Scheme Plug-in");
    init();
  }

  @Override
  protected void init() {
    super.init();
    myForm.init(myExportData);
  }

  public void apply() {
    myForm.apply(myExportData);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    myForm = new PluginInfoForm();
    return myForm.getTopPanel();
  }

}
