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

import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsModule;

/**
 * @author nik
 */
public abstract class JpsDependencyElementBase<Self extends JpsDependencyElementBase<Self>> extends JpsCompositeElementBase<Self> implements JpsDependencyElement {
  protected JpsDependencyElementBase() {
    super();
  }

  protected JpsDependencyElementBase(JpsDependencyElementBase<Self> original) {
    super(original);
  }

  @Override
  public void remove() {
    ((JpsDependenciesListImpl)myParent.getParent()).getContainer().getChild(JpsDependenciesListImpl.DEPENDENCY_COLLECTION_ROLE).removeChild(this);
  }

  public JpsDependenciesListImpl getDependenciesList() {
    return (JpsDependenciesListImpl)myParent.getParent();
  }

  @Override
  public JpsModule getContainingModule() {
    return getDependenciesList().getParent();
  }
}
