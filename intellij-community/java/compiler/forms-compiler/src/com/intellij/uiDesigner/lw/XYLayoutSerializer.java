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

import com.intellij.uiDesigner.shared.XYLayoutManager;
import org.jdom.Element;

import java.awt.*;

/**
 * @author yole
 */
public class XYLayoutSerializer extends LayoutSerializer {
  static XYLayoutSerializer INSTANCE = new XYLayoutSerializer();

  private XYLayoutSerializer() {
  }

  @Override
  void readLayout(Element element, LwContainer container) {
    container.setLayout(new XYLayoutManager());
  }

  @Override
  void readChildConstraints(final Element constraintsElement, final LwComponent component) {
    final Element xyElement = LwXmlReader.getChild(constraintsElement, "xy");
    if(xyElement != null){
      component.setBounds(
        new Rectangle(
          LwXmlReader.getRequiredInt(xyElement, "x"),
          LwXmlReader.getRequiredInt(xyElement, "y"),
          LwXmlReader.getRequiredInt(xyElement, "width"),
          LwXmlReader.getRequiredInt(xyElement, "height")
        )
      );
    }
  }
}
