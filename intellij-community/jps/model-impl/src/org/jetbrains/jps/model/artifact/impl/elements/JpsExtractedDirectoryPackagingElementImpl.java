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
import org.jetbrains.jps.model.artifact.elements.JpsExtractedDirectoryPackagingElement;

/**
 * @author nik
 */
public class JpsExtractedDirectoryPackagingElementImpl extends JpsFileCopyPackagingElementBase<JpsExtractedDirectoryPackagingElementImpl>
  implements JpsExtractedDirectoryPackagingElement {
  private String myPathInJar;

  public JpsExtractedDirectoryPackagingElementImpl(String filePath, String pathInJar) {
    super(filePath);
    myPathInJar = pathInJar;
  }

  @NotNull
  @Override
  public JpsExtractedDirectoryPackagingElementImpl createCopy() {
    return new JpsExtractedDirectoryPackagingElementImpl(myFilePath, myPathInJar);
  }

  @Override
  public void applyChanges(@NotNull JpsExtractedDirectoryPackagingElementImpl modified) {
    super.applyChanges(modified);
    setPathInJar(modified.myPathInJar);
  }

  @Override
  public String getPathInJar() {
    return myPathInJar;
  }

  @Override
  public void setPathInJar(String pathInJar) {
    if (!Comparing.equal(myPathInJar, pathInJar)) {
      myPathInJar = pathInJar;
      fireElementChanged();
    }
  }
}
