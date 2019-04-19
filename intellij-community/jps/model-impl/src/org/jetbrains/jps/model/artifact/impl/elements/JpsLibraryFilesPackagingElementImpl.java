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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.artifact.elements.JpsLibraryFilesPackagingElement;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElement;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElementFactory;
import org.jetbrains.jps.model.artifact.elements.ex.JpsComplexPackagingElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryReference;
import org.jetbrains.jps.model.library.JpsOrderRootType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class JpsLibraryFilesPackagingElementImpl extends JpsComplexPackagingElementBase<JpsLibraryFilesPackagingElementImpl> implements JpsLibraryFilesPackagingElement {
  private static final JpsElementChildRole<JpsLibraryReference>
    LIBRARY_REFERENCE_CHILD_ROLE = JpsElementChildRoleBase.create("library reference");

  public JpsLibraryFilesPackagingElementImpl(@NotNull JpsLibraryReference reference) {
    myContainer.setChild(LIBRARY_REFERENCE_CHILD_ROLE, reference);
  }

  private JpsLibraryFilesPackagingElementImpl(JpsLibraryFilesPackagingElementImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsLibraryFilesPackagingElementImpl createCopy() {
    return new JpsLibraryFilesPackagingElementImpl(this);
  }

  @Override
  @NotNull
  public JpsLibraryReference getLibraryReference() {
    return myContainer.getChild(LIBRARY_REFERENCE_CHILD_ROLE);
  }

  @Override
  public List<JpsPackagingElement> getSubstitution() {
    JpsLibrary library = getLibraryReference().resolve();
    if (library == null) return Collections.emptyList();
    List<JpsPackagingElement> result = new ArrayList<>();
    for (File file : library.getFiles(JpsOrderRootType.COMPILED)) {
      String path = FileUtil.toSystemIndependentName(file.getAbsolutePath());
      if (file.isDirectory()) {
        result.add(JpsPackagingElementFactory.getInstance().createDirectoryCopy(path));
      }
      else {
        result.add(JpsPackagingElementFactory.getInstance().createFileCopy(path, null));
      }
    }
    return result;
  }
}
