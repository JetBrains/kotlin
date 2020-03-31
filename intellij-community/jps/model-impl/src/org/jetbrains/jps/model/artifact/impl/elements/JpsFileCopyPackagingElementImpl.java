// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.artifact.impl.elements;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.artifact.elements.JpsFileCopyPackagingElement;

public class JpsFileCopyPackagingElementImpl extends JpsFileCopyPackagingElementBase<JpsFileCopyPackagingElementImpl> implements JpsFileCopyPackagingElement {
  private String myRenamedOutputFileName;

  public JpsFileCopyPackagingElementImpl(String filePath, String renamedOutputFileName) {
    super(filePath);
    myRenamedOutputFileName = renamedOutputFileName;
  }

  @NotNull
  @Override
  public JpsFileCopyPackagingElementImpl createCopy() {
    return new JpsFileCopyPackagingElementImpl(myFilePath, myRenamedOutputFileName);
  }

  @Override
  public void applyChanges(@NotNull JpsFileCopyPackagingElementImpl modified) {
    super.applyChanges(modified);
    setRenamedOutputFileName(modified.myRenamedOutputFileName);
  }

  @Override
  public String getRenamedOutputFileName() {
    return myRenamedOutputFileName;
  }

  @Override
  public void setRenamedOutputFileName(String renamedOutputFileName) {
    if (!Objects.equals(myRenamedOutputFileName, renamedOutputFileName)) {
      myRenamedOutputFileName = renamedOutputFileName;
      fireElementChanged();
    }
  }
}
