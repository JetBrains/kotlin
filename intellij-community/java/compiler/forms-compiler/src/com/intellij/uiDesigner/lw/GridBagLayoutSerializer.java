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
import com.intellij.uiDesigner.compiler.GridBagConverter;
import org.jdom.Element;

import java.awt.*;

public class GridBagLayoutSerializer extends GridLayoutSerializer {
  private GridBagLayoutSerializer() {
  }

  public static GridBagLayoutSerializer INSTANCE = new GridBagLayoutSerializer();

  @Override
  void readLayout(Element element, LwContainer container) {
    container.setLayout(new GridBagLayout());
  }

  @Override
  void readChildConstraints(final Element constraintsElement, final LwComponent component) {
    super.readChildConstraints(constraintsElement, component);
    GridBagConstraints gbc = new GridBagConstraints();
    GridBagConverter.constraintsToGridBag(component.getConstraints(), gbc);
    final Element gridBagElement = LwXmlReader.getChild(constraintsElement, UIFormXmlConstants.ELEMENT_GRIDBAG);
    if (gridBagElement != null) {
      if (gridBagElement.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_TOP) != null) {
        gbc.insets = LwXmlReader.readInsets(gridBagElement);
      }
      gbc.weightx = LwXmlReader.getOptionalDouble(gridBagElement, UIFormXmlConstants.ATTRIBUTE_WEIGHTX, 0.0);
      gbc.weighty = LwXmlReader.getOptionalDouble(gridBagElement, UIFormXmlConstants.ATTRIBUTE_WEIGHTY, 0.0);
      gbc.ipadx = LwXmlReader.getOptionalInt(gridBagElement, UIFormXmlConstants.ATTRIBUTE_IPADX, 0);
      gbc.ipady = LwXmlReader.getOptionalInt(gridBagElement, UIFormXmlConstants.ATTRIBUTE_IPADY, 0);
    }
    component.setCustomLayoutConstraints(gbc);
  }
}
