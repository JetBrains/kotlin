// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.artifact.impl.elements;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.artifact.elements.JpsExtractedDirectoryPackagingElement;

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
    if (!Objects.equals(myPathInJar, pathInJar)) {
      myPathInJar = pathInJar;
      fireElementChanged();
    }
  }
}
