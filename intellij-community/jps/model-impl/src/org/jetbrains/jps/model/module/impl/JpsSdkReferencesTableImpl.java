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

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementCreator;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.module.JpsSdkReferencesTable;

import java.util.concurrent.ConcurrentMap;

/**
 * @author nik
 */
public class JpsSdkReferencesTableImpl extends JpsCompositeElementBase<JpsSdkReferencesTableImpl> implements JpsSdkReferencesTable {
  public static final JpsSdkReferencesTableRole ROLE = new JpsSdkReferencesTableRole();
  private static final ConcurrentMap<JpsSdkType, JpsSdkReferenceRole> ourReferenceRoles = ContainerUtil.newConcurrentMap();

  public JpsSdkReferencesTableImpl() {
    super();
  }

  private JpsSdkReferencesTableImpl(JpsSdkReferencesTableImpl original) {
    super(original);
  }

  @NotNull
  @Override
  public JpsSdkReferencesTableImpl createCopy() {
    return new JpsSdkReferencesTableImpl(this);
  }

  @Override
  public <P extends JpsElement> void setSdkReference(@NotNull JpsSdkType<P> type, @Nullable JpsSdkReference<P> sdkReference) {
    JpsSdkReferenceRole<P> role = getSdkReferenceRole(type);
    if (sdkReference != null) {
      myContainer.setChild(role, sdkReference);
    }
    else {
      myContainer.removeChild(role);
    }
  }

  @Override
  public <P extends JpsElement> JpsSdkReference<P> getSdkReference(@NotNull JpsSdkType<P> type) {
    return myContainer.getChild(getSdkReferenceRole(type));
  }

  @SuppressWarnings("unchecked")
  @NotNull
  private static <P extends JpsElement> JpsSdkReferenceRole<P> getSdkReferenceRole(@NotNull JpsSdkType<P> type) {
    JpsSdkReferenceRole<P> role = ourReferenceRoles.get(type);
    if (role != null) return role;
    ourReferenceRoles.putIfAbsent(type, new JpsSdkReferenceRole<>(type));
    return ourReferenceRoles.get(type);
  }

  private static class JpsSdkReferencesTableRole extends JpsElementChildRoleBase<JpsSdkReferencesTable> implements JpsElementCreator<JpsSdkReferencesTable> {
    JpsSdkReferencesTableRole() {
      super("sdk references");
    }

    @NotNull
    @Override
    public JpsSdkReferencesTable create() {
      return new JpsSdkReferencesTableImpl();
    }
  }
}
