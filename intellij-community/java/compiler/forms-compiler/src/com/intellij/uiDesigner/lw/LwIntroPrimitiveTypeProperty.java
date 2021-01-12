package com.intellij.uiDesigner.lw;

import com.intellij.uiDesigner.UIFormXmlConstants;
import org.jdom.Element;

public final class LwIntroPrimitiveTypeProperty extends LwIntrospectedProperty {
  private final Class myValueClass;

  public LwIntroPrimitiveTypeProperty(final String name, final Class valueClass){
    super(name, valueClass.getName());
    myValueClass = valueClass;
  }

  @Override
  public Object read(final Element element) throws Exception{
    return LwXmlReader.getRequiredPrimitiveTypeValue(element, UIFormXmlConstants.ATTRIBUTE_VALUE, myValueClass);
  }
}