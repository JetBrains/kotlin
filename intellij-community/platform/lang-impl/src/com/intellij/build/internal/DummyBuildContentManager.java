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
package com.intellij.build.internal;

import com.intellij.build.BuildContentManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.impl.ContentImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public class DummyBuildContentManager implements BuildContentManager {
  @Override
  public void addContent(Content content) {
  }

  @Override
  public void removeContent(Content content) {
  }

  @Override
  public ActionCallback setSelectedContent(Content content,
                                           boolean requestFocus,
                                           boolean forcedFocus,
                                           boolean activate,
                                           Runnable activationCallback) {
    return null;
  }

  @Override
  public Content addTabbedContent(@NotNull JComponent contentComponent,
                                  @NotNull String groupPrefix,
                                  @NotNull String tabName,
                                  @Nullable Icon icon,
                                  @Nullable Disposable childDisposable) {
    return new ContentImpl(contentComponent, tabName, false);
  }
}
