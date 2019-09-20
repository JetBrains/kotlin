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
package org.jetbrains.jps.model.java.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.artifact.impl.elements.JpsModuleOutputPackagingElementBase;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.JpsTestModuleOutputPackagingElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleReference;

/**
 * @author nik
 */
public class JpsTestModuleOutputPackagingElementImpl extends JpsModuleOutputPackagingElementBase<JpsTestModuleOutputPackagingElementImpl>
  implements JpsTestModuleOutputPackagingElement {
  public JpsTestModuleOutputPackagingElementImpl(JpsModuleReference moduleReference) {
    super(moduleReference);
  }

  private JpsTestModuleOutputPackagingElementImpl(JpsTestModuleOutputPackagingElementImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsTestModuleOutputPackagingElementImpl createCopy() {
    return new JpsTestModuleOutputPackagingElementImpl(this);
  }

  @Override
  protected String getOutputUrl(@NotNull JpsModule module) {
    return JpsJavaExtensionService.getInstance().getOutputUrl(module, true);
  }
}
