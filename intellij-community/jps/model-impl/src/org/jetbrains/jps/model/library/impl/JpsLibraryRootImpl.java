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
package org.jetbrains.jps.model.library.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryRoot;
import org.jetbrains.jps.model.library.JpsOrderRootType;

/**
 * @author nik
 */
public class JpsLibraryRootImpl extends JpsElementBase<JpsLibraryRootImpl> implements JpsLibraryRoot {
  private final String myUrl;
  private final JpsOrderRootType myRootType;
  private final InclusionOptions myOptions;

  public JpsLibraryRootImpl(@NotNull String url, @NotNull JpsOrderRootType rootType, @NotNull InclusionOptions options) {
    myUrl = url;
    myRootType = rootType;
    myOptions = options;
  }

  public JpsLibraryRootImpl(JpsLibraryRootImpl original) {
    myUrl = original.myUrl;
    myRootType = original.myRootType;
    myOptions = original.myOptions;
  }

  @NotNull
  @Override
  public JpsOrderRootType getRootType() {
    return myRootType;
  }

  @Override
  @NotNull
  public String getUrl() {
    return myUrl;
  }

  @NotNull
  @Override
  public InclusionOptions getInclusionOptions() {
    return myOptions;
  }

  @NotNull
  @Override
  public JpsLibraryRootImpl createCopy() {
    return new JpsLibraryRootImpl(this);
  }

  @Override
  public void applyChanges(@NotNull JpsLibraryRootImpl modified) {
  }

  @Override
  @NotNull
  public JpsLibrary getLibrary() {
    return (JpsLibrary)myParent.getParent();
  }
}
