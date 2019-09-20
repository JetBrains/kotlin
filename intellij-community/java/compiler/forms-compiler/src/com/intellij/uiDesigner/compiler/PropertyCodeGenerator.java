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

import com.intellij.compiler.instrumentation.InstrumentationClassFinder;
import com.intellij.uiDesigner.lw.LwComponent;
import com.intellij.uiDesigner.lw.LwIntrospectedProperty;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;

import java.io.IOException;

/**
 * @author yole
 */
public abstract class PropertyCodeGenerator {
  public abstract void generatePushValue(final GeneratorAdapter generator, final Object value);

  public boolean generateCustomSetValue(final LwComponent lwComponent,
                                        final InstrumentationClassFinder.PseudoClass componentClass, final LwIntrospectedProperty property,
                                        final GeneratorAdapter generator,
                                        GetFontMethodProvider fontMethodProvider,
                                        final int componentLocal, final String formClassName) throws IOException, ClassNotFoundException {
    return false;
  }

  public void generateClassStart(AsmCodeGenerator.FormClassVisitor visitor, final String name, final InstrumentationClassFinder classFinder) {
  }

  public void generateClassEnd(AsmCodeGenerator.FormClassVisitor visitor) {
  }
}
