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
package com.intellij.codeInsight.hint;

import com.intellij.openapi.editor.Editor;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

/**
 * A external changes provider to update a parameter info state.
 */
public interface ExternalParameterInfoChangesProvider {
  Topic<ExternalParameterInfoChangesProvider> TOPIC =
    Topic.create("ExternalParameterInfoChangesProvider topic", ExternalParameterInfoChangesProvider.class);

  /**
   * Sends update request to {@link ParameterInfoController}.
   * @see ParameterInfoController
   * @param editor editor that parameter info belongs, or null to update controller unconditionally
   * @param offset start of argument list on which parameter info was called
   */
  void fireChangeAtOffset(@Nullable Editor editor, int offset);
}
