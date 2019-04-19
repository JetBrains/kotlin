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

import java.awt.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class LwTabbedPane extends LwContainer implements ITabbedPane {
  public LwTabbedPane(String className) {
    super(className);
  }

  @Override
  protected LayoutManager createInitialLayout() {
    return null;
  }

  @Override
  public void read(final Element element, final PropertiesProvider provider) throws Exception {
    readNoLayout(element, provider);
  }

  @Override
  protected void readConstraintsForChild(final Element element, final LwComponent component) {
    final Element constraintsElement = LwXmlReader.getRequiredChild(element, UIFormXmlConstants.ELEMENT_CONSTRAINTS);
    final Element tabbedPaneChild = LwXmlReader.getRequiredChild(constraintsElement, UIFormXmlConstants.ELEMENT_TABBEDPANE);

    final StringDescriptor descriptor = LwXmlReader.getStringDescriptor(tabbedPaneChild,
                                                                        UIFormXmlConstants.ATTRIBUTE_TITLE,
                                                                        UIFormXmlConstants.ATTRIBUTE_TITLE_RESOURCE_BUNDLE,
                                                                        UIFormXmlConstants.ATTRIBUTE_TITLE_KEY);
    if (descriptor == null) {
      throw new IllegalArgumentException("String descriptor value required");
    }
    final Constraints constraints = new Constraints(descriptor);

    final Element tooltipElement = LwXmlReader.getChild(tabbedPaneChild, UIFormXmlConstants.ELEMENT_TOOLTIP);
    if (tooltipElement != null) {
      constraints.myToolTip = LwXmlReader.getStringDescriptor(tooltipElement,
                                                              UIFormXmlConstants.ATTRIBUTE_VALUE,
                                                              UIFormXmlConstants.ATTRIBUTE_RESOURCE_BUNDLE,
                                                              UIFormXmlConstants.ATTRIBUTE_KEY);
    }

    String icon = tabbedPaneChild.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_ICON);
    if (icon != null) {
      constraints.myIcon = new IconDescriptor(icon);
    }
    icon = tabbedPaneChild.getAttributeValue(UIFormXmlConstants.ATTRIBUTE_DISABLED_ICON);
    if (icon != null) {
      constraints.myDisabledIcon = new IconDescriptor(icon);
    }
    constraints.myEnabled = LwXmlReader.getOptionalBoolean(tabbedPaneChild, UIFormXmlConstants.ATTRIBUTE_ENABLED, true);

    component.setCustomLayoutConstraints(constraints);
  }

  public static final class Constraints {
    /**
     * never null
     */
    public StringDescriptor myTitle;
    public StringDescriptor myToolTip;
    public IconDescriptor myIcon;
    public IconDescriptor myDisabledIcon;
    public boolean myEnabled = true;

    public Constraints(final StringDescriptor title){
      if (title == null){
        throw new IllegalArgumentException("title cannot be null");
      }
      myTitle = title;
    }

    public StringDescriptor getProperty(final String propName) {
      if (propName.equals(TAB_TITLE_PROPERTY)) {
        return myTitle;
      }
      if (propName.equals(TAB_TOOLTIP_PROPERTY)) {
        return myToolTip;
      }
      throw new IllegalArgumentException("Unknown property name " + propName);
    }
  }

  @Override
  public StringDescriptor getTabProperty(IComponent component, final String propName) {
    LwComponent lwComponent = (LwComponent) component;
    LwTabbedPane.Constraints constraints = (LwTabbedPane.Constraints) lwComponent.getCustomLayoutConstraints();
    if (constraints == null) {
      return null;
    }
    return constraints.getProperty(propName);
  }

  @Override
  public boolean areChildrenExclusive() {
    return true;
  }
}
