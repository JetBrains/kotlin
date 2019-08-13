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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.artifact.elements.JpsModuleOutputPackagingElement;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleReference;

/**
 * @author nik
 */
public abstract class JpsModuleOutputPackagingElementBase<Self extends JpsModuleOutputPackagingElementBase<Self>> extends JpsCompositeElementBase<Self> implements
                                                                                                                                                        JpsModuleOutputPackagingElement {
  private static final JpsElementChildRole<JpsModuleReference>
    MODULE_REFERENCE_CHILD_ROLE = JpsElementChildRoleBase.create("module reference");

  public JpsModuleOutputPackagingElementBase(JpsModuleReference moduleReference) {
    myContainer.setChild(MODULE_REFERENCE_CHILD_ROLE, moduleReference);
  }

  public JpsModuleOutputPackagingElementBase(JpsModuleOutputPackagingElementBase<Self> original) {
    super(original);
  }

  @Override
  @NotNull
  public JpsModuleReference getModuleReference() {
    return myContainer.getChild(MODULE_REFERENCE_CHILD_ROLE);
  }

  @Override
  @Nullable
  public String getOutputUrl() {
    JpsModule module = getModuleReference().resolve();
    if (module == null) return null;
    return getOutputUrl(module);
  }

  @Nullable
  protected abstract String getOutputUrl(@NotNull JpsModule module);
}
