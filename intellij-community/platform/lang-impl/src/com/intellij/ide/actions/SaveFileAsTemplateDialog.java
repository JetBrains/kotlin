// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class SaveFileAsTemplateDialog extends SingleConfigurableEditor {
  public SaveFileAsTemplateDialog(@Nullable Project project, Configurable configurable) {
    super(project, configurable, "save.file.as.template.dialog");
    setTitle("Save File as Template");
  }

  @Override
  protected String getHelpId() {
    return "reference.save.file.as.template";
  }
}