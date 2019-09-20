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
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.impl.JpsNamedElementReferenceImpl;
import org.jetbrains.jps.model.impl.JpsProjectElementReference;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleReference;

/**
 * @author nik
 */
public class JpsModuleReferenceImpl extends JpsNamedElementReferenceImpl<JpsModule, JpsModuleReferenceImpl> implements JpsModuleReference {
  public JpsModuleReferenceImpl(String elementName) {
    super(JpsModuleRole.MODULE_COLLECTION_ROLE, elementName, new JpsProjectElementReference());
  }

  @NotNull
  @Override
  public JpsModuleReferenceImpl createCopy() {
    return new JpsModuleReferenceImpl(myElementName);
  }

  @NotNull
  @Override
  public String getModuleName() {
    return myElementName;
  }

  @Override
  public JpsModuleReference asExternal(@NotNull JpsModel model) {
    model.registerExternalReference(this);
    return this;
  }

  @Override
  public String toString() {
    return "module ref: '" + myElementName + "' in " + getParentReference();
  }
}
