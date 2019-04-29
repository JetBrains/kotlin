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

package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.Template;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * An extension controlling aspects of Live Template insertion, like reformatting, adding imports, etc. {@link #getOptionName()} allows
 * to show a checkbox to enable/disable specific such aspect in Live Template settings, {@link #processText} does the actual
 * modifications during live template expansion.<p/>
 *
 * During indexing, {@link #processText} is executed only for instances implementing {@link com.intellij.openapi.project.DumbAware}.
 * @author yole
 */
public interface TemplateOptionalProcessor {
  ExtensionPointName<TemplateOptionalProcessor> EP_NAME = ExtensionPointName.create("com.intellij.liveTemplateOptionalProcessor");

  /**
   * Invoked inside a write action when a live template is finished.
   * Note that this happens even if the corresponding option is disabled, so the implementations
   * should check themselves whether they have to perform any actions.
   */
  void processText(final Project project, final Template template, final Document document, final RangeMarker templateRange, final Editor editor);

  /**
   * @return the text of a checkbox in Live Template settings controlling whether this option is enaled.
   */
  @Nls
  String getOptionName();

  /**
   * @return whether this processor is enabled for a given template
   */
  boolean isEnabled(final Template template);

  /**
   * Change whether this processor is enabled for a given template. It's invoked, for example, when a user changes the value of
   * the corresponding checkbox in Live template settings.
   */
  default void setEnabled(Template template, boolean value) {
  }

  /**
   * @return whether a checkbox for this template should be shown in the configuration dialog, with the given template context.
   */
  default boolean isVisible(@NotNull Template template, @NotNull TemplateContext context) {
    //noinspection deprecation
    return isVisible(template);
  }

  /**
   * @deprecated Please override {@link #isVisible(Template, TemplateContext)}
   */
  @Deprecated
  default boolean isVisible(final Template template) {
    return true;
  }
}
