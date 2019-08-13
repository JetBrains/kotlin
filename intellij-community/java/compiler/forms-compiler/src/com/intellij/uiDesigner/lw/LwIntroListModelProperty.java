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

import java.util.List;

/**
 * @author yole
 */
public class LwIntroListModelProperty extends LwIntrospectedProperty {
  public LwIntroListModelProperty(final String name, final String propertyClassName) {
    super(name, propertyClassName);
  }

  @Override
  public Object read(Element element) throws Exception {
    final List list = element.getChildren(UIFormXmlConstants.ELEMENT_ITEM, element.getNamespace());
    String[] result = new String[list.size()];
    for(int i=0; i<list.size(); i++) {
      Element itemElement = (Element) list.get(i);
      result [i] = LwXmlReader.getRequiredString(itemElement, UIFormXmlConstants.ATTRIBUTE_VALUE);
    }
    return result;
  }
}
