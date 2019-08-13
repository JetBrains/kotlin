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
import com.intellij.uiDesigner.lw.LwContainer;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

/**
 * Layout code generator shared between BorderLayout and CardLayout.
 *
 * @author yole
 */
public class SimpleLayoutCodeGenerator extends LayoutCodeGenerator {
  protected final Type myLayoutType;
  private static final Method ourConstructor = Method.getMethod("void <init>(int,int)");

  public SimpleLayoutCodeGenerator(final Type layoutType) {
    myLayoutType = layoutType;
  }

  @Override
  public void generateContainerLayout(final LwContainer lwContainer, final GeneratorAdapter generator, final int componentLocal) {
    generator.loadLocal(componentLocal);

    generator.newInstance(myLayoutType);
    generator.dup();
    generator.push(Utils.getHGap(lwContainer.getLayout()));
    generator.push(Utils.getVGap(lwContainer.getLayout()));

    generator.invokeConstructor(myLayoutType, ourConstructor);

    generator.invokeVirtual(ourContainerType, ourSetLayoutMethod);
  }

  @Override
  public void generateComponentLayout(final LwComponent lwComponent,
                                      final GeneratorAdapter generator,
                                      final int componentLocal,
                                      final int parentLocal) {
    generator.loadLocal(parentLocal);
    generator.loadLocal(componentLocal);
    generator.push((String) lwComponent.getCustomLayoutConstraints());
    generator.invokeVirtual(ourContainerType, ourAddMethod);
  }
}
