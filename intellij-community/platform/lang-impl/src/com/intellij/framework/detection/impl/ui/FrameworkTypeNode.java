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

import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.ui.ColoredTreeCellRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
class FrameworkTypeNode extends DetectedFrameworkTreeNodeBase {
  private final FrameworkType myFrameworkType;

  FrameworkTypeNode(@NotNull FrameworkType frameworkType) {
    super(frameworkType);
    myFrameworkType = frameworkType;
  }

  @Override
  public void renderNode(ColoredTreeCellRenderer renderer) {
    renderer.setIcon(myFrameworkType.getIcon());
    renderer.append(myFrameworkType.getPresentableName());
  }

  @Override
  public String getCheckedDescription() {
    return null;
  }

  @Override
  public String getUncheckedDescription() {
    return "'" + myFrameworkType.getPresentableName() + "' framework detection will be disabled.";
  }

  @Override
  public void disableDetection(DetectionExcludesConfiguration configuration) {
    configuration.addExcludedFramework(myFrameworkType);
  }
}
