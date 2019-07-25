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
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.awt.*;

/**
 * @author yole
 * @noinspection HardCodedStringLiteral
 */
public abstract class LayoutCodeGenerator {
  protected static final Method ourSetLayoutMethod = Method.getMethod("void setLayout(java.awt.LayoutManager)");
  protected static final Type ourContainerType = Type.getType(Container.class);
  protected static final Method ourAddMethod = Method.getMethod("void add(java.awt.Component,java.lang.Object)");
  protected static final Method ourAddNoConstraintMethod = Method.getMethod("java.awt.Component add(java.awt.Component)");

  public void generateContainerLayout(final LwContainer lwContainer, final GeneratorAdapter generator, final int componentLocal) {
  }

  public abstract void generateComponentLayout(final LwComponent lwComponent, final GeneratorAdapter generator, final int componentLocal,
                                               final int parentLocal);

  protected static void newDimensionOrNull(final GeneratorAdapter generator, final Dimension dimension) {
    if (dimension.width == -1 && dimension.height == -1) {
      generator.visitInsn(Opcodes.ACONST_NULL);
    }
    else {
      AsmCodeGenerator.pushPropValue(generator, "java.awt.Dimension", dimension);
    }
  }

  public String mapComponentClass(final String componentClassName) {
    return componentClassName;
  }
}
