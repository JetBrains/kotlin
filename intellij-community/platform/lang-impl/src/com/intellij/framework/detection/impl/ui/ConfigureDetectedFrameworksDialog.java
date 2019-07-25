/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl.ui;

import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.framework.detection.impl.FrameworkDetectionContextImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import javax.swing.*;
import java.util.List;

/**
 * @author nik
 */
public class ConfigureDetectedFrameworksDialog extends DialogWrapper {
  private final DetectedFrameworksComponent myComponent;
  private final Project myProject;

  public ConfigureDetectedFrameworksDialog(Project project, List<? extends DetectedFrameworkDescription> descriptions) {
    super(project, true);
    myProject = project;
    setTitle("Setup Frameworks");
    myComponent = new DetectedFrameworksComponent(new FrameworkDetectionContextImpl(project));
    myComponent.getTree().rebuildTree(descriptions);
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myComponent.getMainPanel();
  }

  public List<DetectedFrameworkDescription> getSelectedFrameworks() {
    return myComponent.getSelectedFrameworks();
  }

  @Override
  protected void doOKAction() {
    myComponent.processUncheckedNodes(DetectionExcludesConfiguration.getInstance(myProject));
    super.doOKAction();
  }
}
