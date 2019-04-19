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

public abstract class LwIntrospectedProperty implements IProperty {

  private final String myName;
  private final String myPropertyClassName;
  private String myDeclaringClassName;

  public LwIntrospectedProperty(
    final String name,
    final String propertyClassName
  ){
    if (name == null){
      throw new IllegalArgumentException("name cannot be null");
    }
    if (propertyClassName == null){
      throw new IllegalArgumentException("propertyClassName cannot be null");
    }

    myName = name;
    myPropertyClassName = propertyClassName;
  }

  /**
   * @return never null
   */
  @Override
  public final String getName(){
    return myName;
  }

  /**
   * @return never null
   */
  public final String getPropertyClassName(){
    return myPropertyClassName;
  }

  public final String getReadMethodName() {
    return "get" + Character.toUpperCase(myName.charAt(0)) + myName.substring(1);
  }

  public final String getWriteMethodName() {
    return "set" + Character.toUpperCase(myName.charAt(0)) + myName.substring(1);
  }

  public String getDeclaringClassName() {
    return myDeclaringClassName;
  }

  public void setDeclaringClassName(final String definingClassName) {
    myDeclaringClassName = definingClassName;
  }

  /**
   * @param element element that contains serialized property data. This element was
   * written by {@link com.intellij.uiDesigner.propertyInspector.IntrospectedProperty#write(Object, com.intellij.uiDesigner.XmlWriter)}
   * method. So {@code read} and {@code write} methods should be consistent.
   *
   * @return property value. Should never return {@code null}. For example,
   * value can be {@code java.lang.Integer} for {@code IntroIntProperty}.
   *
   */
  public abstract Object read(Element element) throws Exception;

  @Override
  public Object getPropertyValue(final IComponent component) {
    return ((LwComponent) component).getPropertyValue(this);
  }

  public String getCodeGenPropertyClassName() {
    return getPropertyClassName();
  }
}
