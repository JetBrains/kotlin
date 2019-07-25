/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.artifact.impl.elements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.artifact.elements.JpsDirectoryPackagingElement;

/**
 * @author nik
 */
public class JpsDirectoryPackagingElementImpl extends JpsCompositePackagingElementBase<JpsDirectoryPackagingElementImpl> implements JpsDirectoryPackagingElement {
  private String myDirectoryName;

  public JpsDirectoryPackagingElementImpl(String directoryName) {
    myDirectoryName = directoryName;
  }

  private JpsDirectoryPackagingElementImpl(JpsDirectoryPackagingElementImpl original) {
    super(original);
    myDirectoryName = original.myDirectoryName;
  }

  @NotNull
  @Override
  public JpsDirectoryPackagingElementImpl createCopy() {
    return new JpsDirectoryPackagingElementImpl(this);
  }

  @Override
  public String getDirectoryName() {
    return myDirectoryName;
  }

  @Override
  public void setDirectoryName(String directoryName) {
    if (!myDirectoryName.equals(directoryName)) {
      myDirectoryName = directoryName;
      fireElementChanged();
    }
  }
}
