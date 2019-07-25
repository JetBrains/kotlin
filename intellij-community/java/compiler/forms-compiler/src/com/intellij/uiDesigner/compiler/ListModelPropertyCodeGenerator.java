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

import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

/**
 * @author yole
 */
public class ListModelPropertyCodeGenerator extends PropertyCodeGenerator {
  private final Type myListModelType;
  private static final Method ourInitMethod = Method.getMethod("void <init>()");
  private static final Method ourAddElementMethod = Method.getMethod("void addElement(java.lang.Object)");

  public ListModelPropertyCodeGenerator(final Class aClass) {
    myListModelType = Type.getType(aClass);
  }

  @Override
  public void generatePushValue(final GeneratorAdapter generator, final Object value) {
    String[] items = (String[]) value;
    int listModelLocal = generator.newLocal(myListModelType);

    generator.newInstance(myListModelType);
    generator.dup();
    generator.invokeConstructor(myListModelType, ourInitMethod);
    generator.storeLocal(listModelLocal);

    for (String item : items) {
      generator.loadLocal(listModelLocal);
      generator.push(item);
      generator.invokeVirtual(myListModelType, ourAddElementMethod);
    }

    generator.loadLocal(listModelLocal);
  }
}
