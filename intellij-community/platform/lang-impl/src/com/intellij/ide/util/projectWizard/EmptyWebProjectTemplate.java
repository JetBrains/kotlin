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
package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ProjectGeneratorPeer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EmptyWebProjectTemplate extends WebProjectTemplate<Object> {
  @Nls
  @NotNull
  @Override
  public String getName() {
    return "Empty Project";
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull Object settings, @NotNull Module module) {
    //ignore
  }

  @NotNull
  @Override
  public ProjectGeneratorPeer<Object> createPeer() {
    return new ProjectGeneratorPeer<Object>() {
      @NotNull
      @Override
      public JComponent getComponent() {
        return new JPanel();
      }

      @Override
      public void buildUI(@NotNull SettingsStep settingsStep) {
        settingsStep.addSettingsComponent(getComponent());
      }

      @NotNull
      @Override
      public Object getSettings() {
        return new Object();
      }

      @Nullable
      @Override
      public ValidationInfo validate() {
        return null;
      }

      @Override
      public boolean isBackgroundJobRunning() {
        return false;
      }

      @Override
      public void addSettingsStateListener(@NotNull SettingsStateListener listener) {
      }
    };
  }
}
