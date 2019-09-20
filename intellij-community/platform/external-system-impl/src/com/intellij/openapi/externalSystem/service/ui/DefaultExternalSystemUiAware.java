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
package com.intellij.openapi.externalSystem.service.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import icons.ExternalSystemIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * This class is not singleton but offers single-point-of-usage field - {@link #INSTANCE}.
 * 
 * @author Denis Zhdanov
 */
public class DefaultExternalSystemUiAware implements ExternalSystemUiAware {

  @NotNull public static final DefaultExternalSystemUiAware INSTANCE = new DefaultExternalSystemUiAware();

  @NotNull
  @Override
  public String getProjectRepresentationName(@NotNull String targetProjectPath, @Nullable String rootProjectPath) {
    return new File(targetProjectPath).getParentFile().getName();
  }

  @Nullable
  @Override
  public FileChooserDescriptor getExternalProjectConfigDescriptor() {
    return null;
  }

  @NotNull
  @Override
  public Icon getProjectIcon() {
    return AllIcons.Nodes.IdeaProject;
  }

  @NotNull
  @Override
  public Icon getTaskIcon() {
    return ExternalSystemIcons.Task;
  }
}
