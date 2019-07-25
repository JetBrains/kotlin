/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.uiDesigner.UIFormXmlConstants;
import com.intellij.uiDesigner.lw.LwComponent;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.awt.*;

/**
 * @author Alexander Lobas
 */
public class CardLayoutCodeGenerator extends SimpleLayoutCodeGenerator {
  private static final Method ourGetLayoutMethod = Method.getMethod("java.awt.LayoutManager getLayout()");
  private static final Method ourShowMethod = Method.getMethod("void show(java.awt.Container,java.lang.String)");

  public CardLayoutCodeGenerator() {
    super(Type.getType(CardLayout.class));
  }

  @Override
  public void generateComponentLayout(LwComponent lwComponent,
                                      GeneratorAdapter generator,
                                      int componentLocal,
                                      int parentLocal) {
    super.generateComponentLayout(lwComponent, generator, componentLocal, parentLocal);

    String defaultCard = (String)lwComponent.getParent().getClientProperty(UIFormXmlConstants.LAYOUT_CARD);
    if (lwComponent.getId().equals(defaultCard)) {
      generator.loadLocal(parentLocal);
      generator.invokeVirtual(ourContainerType, ourGetLayoutMethod);
      generator.checkCast(myLayoutType);
      generator.loadLocal(parentLocal);
      generator.push((String) lwComponent.getCustomLayoutConstraints());
      generator.invokeVirtual(myLayoutType, ourShowMethod);
    }
  }
}