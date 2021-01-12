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

import com.intellij.uiDesigner.compiler.UnexpectedFormElementException;
import org.jdom.Element;

import java.awt.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class LwSplitPane extends LwContainer{
  public static final String POSITION_LEFT = "left";
  public static final String POSITION_RIGHT = "right";

  public LwSplitPane(String className) {
    super(className);
  }

  @Override
  protected LayoutManager createInitialLayout(){
    return null;
  }

  @Override
  public void read(final Element element, final PropertiesProvider provider) throws Exception {
    readNoLayout(element, provider);
  }

  @Override
  protected void readConstraintsForChild(final Element element, final LwComponent component) {
    final Element constraintsElement = LwXmlReader.getRequiredChild(element, "constraints");
    final Element splitterChild = LwXmlReader.getRequiredChild(constraintsElement, "splitpane");
    final String position = LwXmlReader.getRequiredString(splitterChild, "position");
    if ("left".equals(position)) {
      component.setCustomLayoutConstraints(POSITION_LEFT);
    }
    else if ("right".equals(position)) {
      component.setCustomLayoutConstraints(POSITION_RIGHT);
    }
    else {
      throw new UnexpectedFormElementException("unexpected position: " + position);
    }
  }
}
