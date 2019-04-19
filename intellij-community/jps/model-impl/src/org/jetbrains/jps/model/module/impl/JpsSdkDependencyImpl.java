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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.module.JpsSdkDependency;

/**
 * @author nik
 */
public class JpsSdkDependencyImpl extends JpsDependencyElementBase<JpsSdkDependencyImpl> implements JpsSdkDependency {
  private final JpsSdkType<?> mySdkType;

  public JpsSdkDependencyImpl(@NotNull JpsSdkType<?> sdkType) {
    super();
    mySdkType = sdkType;
  }

  public JpsSdkDependencyImpl(JpsSdkDependencyImpl original) {
    super(original);
    mySdkType = original.mySdkType;
  }

  @NotNull
  @Override
  public JpsSdkDependencyImpl createCopy() {
    return new JpsSdkDependencyImpl(this);
  }

  @Override
  @NotNull
  public JpsSdkType<?> getSdkType() {
    return mySdkType;
  }

  @Override
  public JpsLibrary resolveSdk() {
    final JpsSdkReference<?> reference = getSdkReference();
    if (reference == null) return null;
    return reference.resolve();
  }

  @Override
  @Nullable
  public JpsSdkReference<?> getSdkReference() {
    return getContainingModule().getSdkReference(mySdkType);
  }

  @Override
  public boolean isInherited() {
    return false;
  }

  @Override
  public String toString() {
    return "sdk dep [" + mySdkType + "]";
  }
}
