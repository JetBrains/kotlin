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

import com.intellij.uiDesigner.core.Spacer;
import com.intellij.uiDesigner.lw.LwComponent;
import com.intellij.uiDesigner.lw.LwContainer;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public class GridBagLayoutCodeGenerator extends LayoutCodeGenerator {
  private static final Type ourGridBagLayoutType = Type.getType(GridBagLayout.class);
  private static final Type ourGridBagConstraintsType = Type.getType(GridBagConstraints.class);
  private static final Method ourDefaultConstructor = Method.getMethod("void <init> ()");

  private static final Type myPanelType = Type.getType(JPanel.class);

  @Override
  public String mapComponentClass(final String componentClassName) {
    if (componentClassName.equals(Spacer.class.getName())) {
      return JPanel.class.getName();
    }
    return super.mapComponentClass(componentClassName);
  }

  @Override
  public void generateContainerLayout(final LwContainer lwContainer, final GeneratorAdapter generator, final int componentLocal) {
    generator.loadLocal(componentLocal);

    generator.newInstance(ourGridBagLayoutType);
    generator.dup();
    generator.invokeConstructor(ourGridBagLayoutType, ourDefaultConstructor);

    generator.invokeVirtual(ourContainerType, ourSetLayoutMethod);
  }

  private void generateFillerPanel(final GeneratorAdapter generator, final int parentLocal, final GridBagConverter.Result result) {
    int panelLocal = generator.newLocal(myPanelType);

    generator.newInstance(myPanelType);
    generator.dup();
    generator.invokeConstructor(myPanelType, ourDefaultConstructor);
    generator.storeLocal(panelLocal);

    generateConversionResult(generator, result, panelLocal, parentLocal);

  }

  @Override
  public void generateComponentLayout(final LwComponent component,
                                      final GeneratorAdapter generator,
                                      final int componentLocal,
                                      final int parentLocal) {
    GridBagConstraints gbc;
    if (component.getCustomLayoutConstraints() instanceof GridBagConstraints) {
      gbc = (GridBagConstraints) component.getCustomLayoutConstraints();
    }
    else {
      gbc = new GridBagConstraints();
    }

    GridBagConverter.constraintsToGridBag(component.getConstraints(), gbc);

    generateGridBagConstraints(generator, gbc, componentLocal, parentLocal);
  }

  private static void generateConversionResult(final GeneratorAdapter generator, final GridBagConverter.Result result,
                                               final int componentLocal, final int parentLocal) {
    checkSetSize(generator, componentLocal, "setMinimumSize", result.minimumSize);
    checkSetSize(generator, componentLocal, "setPreferredSize", result.preferredSize);
    checkSetSize(generator, componentLocal, "setMaximumSize", result.maximumSize);

    generateGridBagConstraints(generator, result.constraints, componentLocal, parentLocal);
  }

  private static void generateGridBagConstraints(final GeneratorAdapter generator, GridBagConstraints constraints, final int componentLocal,
                                                 final int parentLocal) {
    int gbcLocal = generator.newLocal(ourGridBagConstraintsType);

    generator.newInstance(ourGridBagConstraintsType);
    generator.dup();
    generator.invokeConstructor(ourGridBagConstraintsType, ourDefaultConstructor);
    generator.storeLocal(gbcLocal);

    GridBagConstraints defaults = new GridBagConstraints();
    if (defaults.gridx != constraints.gridx) {
      setIntField(generator, gbcLocal, "gridx", constraints.gridx);
    }
    if (defaults.gridy != constraints.gridy) {
      setIntField(generator, gbcLocal, "gridy", constraints.gridy);
    }
    if (defaults.gridwidth != constraints.gridwidth) {
      setIntField(generator, gbcLocal, "gridwidth", constraints.gridwidth);
    }
    if (defaults.gridheight != constraints.gridheight) {
      setIntField(generator, gbcLocal, "gridheight", constraints.gridheight);
    }
    if (defaults.weightx != constraints.weightx) {
      setDoubleField(generator, gbcLocal, "weightx", constraints.weightx);
    }
    if (defaults.weighty != constraints.weighty) {
      setDoubleField(generator, gbcLocal, "weighty", constraints.weighty);
    }
    if (defaults.anchor != constraints.anchor) {
      setIntField(generator, gbcLocal, "anchor", constraints.anchor);
    }
    if (defaults.fill != constraints.fill) {
      setIntField(generator, gbcLocal, "fill", constraints.fill);
    }
    if (defaults.ipadx != constraints.ipadx) {
      setIntField(generator, gbcLocal, "ipadx", constraints.ipadx);
    }
    if (defaults.ipady != constraints.ipady) {
      setIntField(generator, gbcLocal, "ipady", constraints.ipady);
    }
    if (!defaults.insets.equals(constraints.insets)) {
      generator.loadLocal(gbcLocal);
      AsmCodeGenerator.pushPropValue(generator, Insets.class.getName(), constraints.insets);
      generator.putField(ourGridBagConstraintsType, "insets", Type.getType(Insets.class));
    }

    generator.loadLocal(parentLocal);
    generator.loadLocal(componentLocal);
    generator.loadLocal(gbcLocal);

    generator.invokeVirtual(ourContainerType, ourAddMethod);
  }

  private static void checkSetSize(final GeneratorAdapter generator, final int componentLocal, final String methodName, final Dimension dimension) {
    if (dimension != null) {
      generator.loadLocal(componentLocal);
      AsmCodeGenerator.pushPropValue(generator, "java.awt.Dimension", dimension);
      generator.invokeVirtual(Type.getType(Component.class),
                              new Method(methodName, Type.VOID_TYPE, new Type[] { Type.getType(Dimension.class) }));
    }
  }

  private static void setIntField(final GeneratorAdapter generator, final int local, final String fieldName, final int value) {
    generator.loadLocal(local);
    generator.push(value);
    generator.putField(ourGridBagConstraintsType, fieldName, Type.INT_TYPE);
  }

  private static void setDoubleField(final GeneratorAdapter generator, final int local, final String fieldName, final double value) {
    generator.loadLocal(local);
    generator.push(value);
    generator.putField(ourGridBagConstraintsType, fieldName, Type.DOUBLE_TYPE);
  }
}
