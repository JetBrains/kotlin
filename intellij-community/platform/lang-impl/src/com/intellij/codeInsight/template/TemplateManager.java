/*
* Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.codeInsight.template;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.PairProcessor;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class TemplateManager {
  public static final Topic<TemplateManagerListener> TEMPLATE_STARTED_TOPIC = Topic.create("TEMPLATE_STARTED", TemplateManagerListener.class);

  public static TemplateManager getInstance(Project project) {
    return ServiceManager.getService(project, TemplateManager.class);
  }

  public abstract void startTemplate(@NotNull Editor editor, @NotNull Template template);

  public abstract void startTemplate(@NotNull Editor editor, String selectionString, @NotNull Template template);

  public abstract void startTemplate(@NotNull Editor editor, @NotNull Template template, TemplateEditingListener listener);

  public abstract void startTemplate(@NotNull final Editor editor,
                                     @NotNull final Template template,
                                     boolean inSeparateCommand,
                                     Map<String, String> predefinedVarValues,
                                     @Nullable TemplateEditingListener listener);

  public abstract void startTemplate(@NotNull Editor editor,
                                     @NotNull Template template,
                                     TemplateEditingListener listener,
                                     final PairProcessor<String, String> callback);

  public abstract boolean startTemplate(@NotNull Editor editor, char shortcutChar);

  public abstract Template createTemplate(@NotNull String key, String group);

  public abstract Template createTemplate(@NotNull String key, String group, @NonNls String text);

  @Nullable
  public abstract Template getActiveTemplate(@NotNull Editor editor);

  /**
   * Finished a live template in the given editor, if it's present
   * @return whether a live template was present
   */
  public abstract boolean finishTemplate(@NotNull Editor editor);
}
