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
import com.intellij.uiDesigner.lw.LwTabbedPane;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import javax.swing.*;

/**
 * @author yole
 */
public class TabbedPaneLayoutCodeGenerator extends LayoutCodeGenerator {
  private final Type myTabbedPaneType = Type.getType(JTabbedPane.class);
  private final Method myAddTabMethod = Method.getMethod("void addTab(java.lang.String,javax.swing.Icon,java.awt.Component,java.lang.String)");
  private final Method mySetDisabledIconAtMethod = Method.getMethod("void setDisabledIconAt(int,javax.swing.Icon)");
  private final Method mySetEnabledAtMethod = Method.getMethod("void setEnabledAt(int,boolean)");

  @Override
  public void generateComponentLayout(final LwComponent lwComponent,
                                      final GeneratorAdapter generator,
                                      final int componentLocal,
                                      final int parentLocal) {
    generator.loadLocal(parentLocal);
    final LwTabbedPane.Constraints tabConstraints = (LwTabbedPane.Constraints)lwComponent.getCustomLayoutConstraints();
    if (tabConstraints == null){
      throw new IllegalArgumentException("tab constraints cannot be null: " + lwComponent.getId());
    }
    AsmCodeGenerator.pushPropValue(generator, String.class.getName(), tabConstraints.myTitle);
    if (tabConstraints.myIcon == null) {
      generator.push((String) null);
    }
    else {
      AsmCodeGenerator.pushPropValue(generator, Icon.class.getName(), tabConstraints.myIcon);
    }
    generator.loadLocal(componentLocal);
    if (tabConstraints.myToolTip == null) {
      generator.push((String) null);
    }
    else {
      AsmCodeGenerator.pushPropValue(generator, String.class.getName(), tabConstraints.myToolTip);
    }
    generator.invokeVirtual(myTabbedPaneType, myAddTabMethod);

    int index = lwComponent.getParent().indexOfComponent(lwComponent);
    if (tabConstraints.myDisabledIcon != null) {
      generator.loadLocal(parentLocal);
      generator.push(index);
      AsmCodeGenerator.pushPropValue(generator, Icon.class.getName(), tabConstraints.myDisabledIcon);
      generator.invokeVirtual(myTabbedPaneType, mySetDisabledIconAtMethod);
    }
    if (!tabConstraints.myEnabled) {
      generator.loadLocal(parentLocal);
      generator.push(index);
      generator.push(tabConstraints.myEnabled);
      generator.invokeVirtual(myTabbedPaneType, mySetEnabledAtMethod);
    }
  }
}
