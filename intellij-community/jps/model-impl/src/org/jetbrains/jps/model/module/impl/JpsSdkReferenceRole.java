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
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;

public class JpsSdkReferenceRole<P extends JpsElement> extends JpsElementChildRoleBase<JpsSdkReference<P>> {
  private final JpsSdkType<P> myType;

  public JpsSdkReferenceRole(@NotNull JpsSdkType<P> type) {
    super("sdk reference " + type);
    myType = type;
  }

  @Override
  public int hashCode() {
    return myType.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof JpsSdkReferenceRole && myType.equals(((JpsSdkReferenceRole)obj).myType);
  }
}
