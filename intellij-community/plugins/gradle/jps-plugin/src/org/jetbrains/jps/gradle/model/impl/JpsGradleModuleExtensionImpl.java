/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.jps.gradle.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gradle.model.JpsGradleModuleExtension;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

/**
 * @author Vladislav.Soroka
 */
public class JpsGradleModuleExtensionImpl extends JpsElementBase<JpsGradleModuleExtensionImpl> implements JpsGradleModuleExtension {
  public static final JpsElementChildRole<JpsGradleModuleExtension> ROLE = JpsElementChildRoleBase.create("gradle");

  private final String myModuleType;

  public JpsGradleModuleExtensionImpl(String moduleType) {
    myModuleType = moduleType;
  }

  @Nullable
  @Override
  public String getModuleType() {
    return myModuleType;
  }

  @NotNull
  @Override
  public JpsGradleModuleExtensionImpl createCopy() {
    return new JpsGradleModuleExtensionImpl(myModuleType);
  }

  @Override
  public void applyChanges(@NotNull JpsGradleModuleExtensionImpl modified) {
  }
}
