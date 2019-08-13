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

import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleDependency;
import org.jetbrains.jps.model.module.JpsModuleReference;

/**
 * @author nik
 */
public class JpsModuleDependencyImpl extends JpsDependencyElementBase<JpsModuleDependencyImpl> implements JpsModuleDependency {
  private static final JpsElementChildRole<JpsModuleReference>
    MODULE_REFERENCE_CHILD_ROLE = JpsElementChildRoleBase.create("module reference");

  private volatile Ref<JpsModule> myCachedModule = null;

  public JpsModuleDependencyImpl(final JpsModuleReference moduleReference) {
    super();
    myContainer.setChild(MODULE_REFERENCE_CHILD_ROLE, moduleReference);
  }

  public JpsModuleDependencyImpl(JpsModuleDependencyImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsModuleReference getModuleReference() {
    return myContainer.getChild(MODULE_REFERENCE_CHILD_ROLE);
  }

  @Override
  public JpsModule getModule() {
    Ref<JpsModule> moduleRef = myCachedModule;
    if (moduleRef == null) {
      moduleRef = new Ref<>(getModuleReference().resolve());
      myCachedModule = moduleRef;
    }
    return moduleRef.get();
  }

  @NotNull
  @Override
  public JpsModuleDependencyImpl createCopy() {
    return new JpsModuleDependencyImpl(this);
  }

  @Override
  public String toString() {
    return "module dep [" + getModuleReference() + "]";
  }
}
