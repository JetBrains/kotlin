// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

import javax.swing.*;

/**
 * @author Sergey.Malenkov
 */
public class EditorOptionsTopHitProvider extends EditorOptionsTopHitProviderBase.NoPrefix {
  @Override
  protected Configurable getConfigurable(Project project) {
    return new EditorOptionsPanel();
  }

  @Override
  protected String getOptionName(JCheckBox checkbox) {
    String optionName = super.getOptionName(checkbox);
    if (optionName != null && optionName.contains(ApplicationBundle.message("checkbox.use.soft.wraps.at.editor"))) {
      String masks = EditorSettingsExternalizable.getInstance().getSoftWrapFileMasks();
      optionName += masks.isEmpty() ? ApplicationBundle.message("soft.wraps.file.masks.empty.text") : masks;
    }
    return optionName;
  }
}
