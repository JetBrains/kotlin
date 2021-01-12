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
package com.intellij.openapi.compiler;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Describes a single compiler message that is shown in compiler message view.
 *
 * @see CompileContext#addMessage(CompilerMessageCategory, String, String, int, int)
 */
public interface CompilerMessage {
  /**
   * An empty array of compiler messages which can be reused to avoid unnecessary allocations.
   */
  CompilerMessage[] EMPTY_ARRAY = new CompilerMessage[0];

  /**
   * Returns the category of the message.
   *
   * @return a category this message belongs to (error, warning, information).
   */
  @NotNull
  CompilerMessageCategory getCategory();

  /**
   * Returs the message text.
   *
   * @return message text
   */
  String getMessage();

  /**
   * Returns the navigatable object allowing to navigate to the message source.
   *
   * @return the instance.
   */
  @Nullable
  Navigatable getNavigatable();

  /**
   * Returns the file to which the message applies.
   *
   * @return the file to which the message applies.
   */
  VirtualFile getVirtualFile();

  /**
   * Returns the location prefix prepended to message while exporting compilation results to text.
   *
   * @return location prefix prepended to message while exporting compilation results to text.
   */
  String getExportTextPrefix();

  /**
   * Returns the location prefix prepended to message while exporting compilation results to text.
   *
   * @return location prefix prepended to message while rendering compilation results in UI.
   */
  String getRenderTextPrefix();
}
