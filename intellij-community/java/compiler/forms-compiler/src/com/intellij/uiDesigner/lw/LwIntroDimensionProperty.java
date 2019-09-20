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

import org.jdom.Element;

import java.awt.*;

/**
 * @author Anton Katilin
 */
public final class LwIntroDimensionProperty extends LwIntrospectedProperty {
  public LwIntroDimensionProperty(final String name){
    super(name, "java.awt.Dimension");
  }

  @Override
  public Object read(final Element element) throws Exception{
    final int width = LwXmlReader.getRequiredInt(element, "width");
    final int height = LwXmlReader.getRequiredInt(element, "height");
    return new Dimension(width, height);
  }
}
