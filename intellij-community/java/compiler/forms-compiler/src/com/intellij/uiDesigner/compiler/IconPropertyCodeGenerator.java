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

import com.intellij.uiDesigner.lw.IconDescriptor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.GeneratorAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import javax.swing.*;

/**
 * @author yole
 */
public class IconPropertyCodeGenerator extends PropertyCodeGenerator {
  private static final Type ourImageIconType = Type.getType(ImageIcon.class);
  private static final Method ourInitMethod = Method.getMethod("void <init>(java.net.URL)");
  private static final Method ourGetResourceMethod = Method.getMethod("java.net.URL getResource(java.lang.String)");
  private static final Method ourGetClassMethod = new Method("getClass", "()Ljava/lang/Class;");
  private static final Type ourObjectType = Type.getType(Object.class);
  private static final Type ourClassType = Type.getType(Class.class);

  @Override
  public void generatePushValue(final GeneratorAdapter generator, final Object value) {
    IconDescriptor descriptor = (IconDescriptor) value;
    generator.newInstance(ourImageIconType);
    generator.dup();

    generator.loadThis();
    generator.invokeVirtual(ourObjectType, ourGetClassMethod);
    generator.push("/" + descriptor.getIconPath());
    generator.invokeVirtual(ourClassType, ourGetResourceMethod);

    generator.invokeConstructor(ourImageIconType, ourInitMethod);
  }
}
