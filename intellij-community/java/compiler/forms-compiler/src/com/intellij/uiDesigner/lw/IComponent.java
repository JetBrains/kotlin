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

import com.intellij.uiDesigner.core.GridConstraints;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public interface IComponent {
  Object getClientProperty(Object key);

  void putClientProperty(Object key, Object value);

  /**
   * @return name of the field (in bound class). Returns {@code null}
   * if the component is not bound to any field.
   */
  String getBinding();

  String getComponentClassName();

  String getId();

  boolean isCustomCreate();

  IProperty[] getModifiedProperties();

  IContainer getParentContainer();

  GridConstraints getConstraints();

  Object getCustomLayoutConstraints();

  boolean accept(ComponentVisitor visitor);

  /**
   * Returns true if only one of the children of the component can be visible at a time
   * (for example, the component is a tabbed pane or a container with CardLayout).
   *
   * @return true if children are exclusive, false otherwise.
   */
  boolean areChildrenExclusive();
}
