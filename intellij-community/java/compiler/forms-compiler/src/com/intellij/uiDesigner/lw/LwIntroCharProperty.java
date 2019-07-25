package com.intellij.uiDesigner.lw;

import com.intellij.uiDesigner.UIFormXmlConstants;
import org.jdom.Element;

public final class LwIntroCharProperty extends LwIntrospectedProperty {
  public LwIntroCharProperty(final String name){
    super(name, Character.class.getName());
  }

  @Override
  public Object read(final Element element) throws Exception{
    return Character.valueOf(LwXmlReader.getRequiredString(element, UIFormXmlConstants.ATTRIBUTE_VALUE).charAt(0));
  }
}