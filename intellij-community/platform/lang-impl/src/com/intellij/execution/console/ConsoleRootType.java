/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.console;

import com.intellij.ide.scratch.RootType;
import com.intellij.openapi.util.text.StringHash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author gregsh
 */
public abstract class ConsoleRootType extends RootType {

  public static final String SEPARATOR = "-. . -..- - / . -. - .-. -.--";
  private static final String PATH_PREFIX = "consoles/";

  protected ConsoleRootType(@NotNull String consoleTypeId, @Nullable String displayName) {
    super(PATH_PREFIX + consoleTypeId, displayName);
  }

  public final String getConsoleTypeId() {
    return getId().substring(PATH_PREFIX.length());
  }

  @NotNull
  public String getEntrySeparator() {
    return "\n" + SEPARATOR + "\n";
  }

  @NotNull
  public String getContentPathName(@NotNull String id) {
    return Long.toHexString(StringHash.calc(id));
  }

  @NotNull
  public String getHistoryPathName(@NotNull String id) {
    return Long.toHexString(StringHash.calc(id));
  }

  @NotNull
  public String getDefaultFileExtension() {
    return "txt";
  }
}
