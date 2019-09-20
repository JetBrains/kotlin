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
package org.jetbrains.jps.model.module.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryReference;
import org.jetbrains.jps.model.module.JpsLibraryDependency;

/**
 * @author nik
 */
public class JpsLibraryDependencyImpl extends JpsDependencyElementBase<JpsLibraryDependencyImpl> implements JpsLibraryDependency {
  public static final JpsElementChildRole<JpsLibraryReference>
    LIBRARY_REFERENCE_CHILD_ROLE = JpsElementChildRoleBase.create("library reference");

  public JpsLibraryDependencyImpl(final JpsLibraryReference reference) {
    super();
    myContainer.setChild(LIBRARY_REFERENCE_CHILD_ROLE, reference);
  }

  public JpsLibraryDependencyImpl(JpsLibraryDependencyImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsLibraryReference getLibraryReference() {
    return myContainer.getChild(LIBRARY_REFERENCE_CHILD_ROLE);
  }

  @Override
  public JpsLibrary getLibrary() {
    return getLibraryReference().resolve();
  }

  @NotNull
  @Override
  public JpsLibraryDependencyImpl createCopy() {
    return new JpsLibraryDependencyImpl(this);
  }

  @Override
  public String toString() {
    return "lib dep [" + getLibraryReference() + "]";
  }
}
