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

package com.intellij.uiDesigner.lw;

import com.intellij.uiDesigner.UIFormXmlConstants;
import org.jdom.Element;

import java.lang.reflect.Method;

/**
 * @author yole
 */
public class LwIntroEnumProperty extends LwIntrospectedProperty {
  private final Class myEnumClass;

  public LwIntroEnumProperty(final String name, final Class enumClass) {
    super(name, enumClass.getName());
    myEnumClass = enumClass;
  }

  @Override
  public Object read(Element element) throws Exception {
    String value = element.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_VALUE);
    final Method method = myEnumClass.getMethod("valueOf", String.class);
    return method.invoke(null, value);
  }

  @Override
  public String getCodeGenPropertyClassName() {
    return "java.lang.Enum";
  }
}
