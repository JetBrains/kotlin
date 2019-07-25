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

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElement;
import org.jetbrains.jps.model.ex.JpsElementBase;

/**
 * @author nik
 */
public abstract class JpsFileCopyPackagingElementBase<Self extends JpsFileCopyPackagingElementBase<Self>> extends JpsElementBase<Self> implements
                                                                                                                                 JpsPackagingElement {
  protected String myFilePath;

  public JpsFileCopyPackagingElementBase(String filePath) {
    myFilePath = filePath;
  }

  @Override
  public void applyChanges(@NotNull Self modified) {
    setFilePath(modified.myFilePath);
  }

  public String getFilePath() {
    return myFilePath;
  }

  public void setFilePath(String filePath) {
    if (!Comparing.equal(myFilePath, filePath)) {
      myFilePath = filePath;
      fireElementChanged();
    }
  }
}
