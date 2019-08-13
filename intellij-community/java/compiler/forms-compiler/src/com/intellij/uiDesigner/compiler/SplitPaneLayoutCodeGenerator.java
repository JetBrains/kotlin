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
package com.intellij.uiDesigner.compiler;

import com.intellij.uiDesigner.lw.LwComponent;
import com.intellij.uiDesigner.lw.LwSplitPane;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import javax.swing.*;

/**
 * @author yole
 */
public class SplitPaneLayoutCodeGenerator extends LayoutCodeGenerator {
  private final Type mySplitPaneType = Type.getType(JSplitPane.class);
  private final Method mySetLeftMethod = Method.getMethod("void setLeftComponent(java.awt.Component)");
  private final Method mySetRightMethod = Method.getMethod("void setRightComponent(java.awt.Component)");

  @Override
  public void generateComponentLayout(final LwComponent lwComponent,
                                      final GeneratorAdapter generator,
                                      final int componentLocal,
                                      final int parentLocal) {
    generator.loadLocal(parentLocal);
    generator.loadLocal(componentLocal);
    if (LwSplitPane.POSITION_LEFT.equals(lwComponent.getCustomLayoutConstraints())) {
      generator.invokeVirtual(mySplitPaneType, mySetLeftMethod);
    }
    else {
      generator.invokeVirtual(mySplitPaneType, mySetRightMethod);
    }
  }
}
