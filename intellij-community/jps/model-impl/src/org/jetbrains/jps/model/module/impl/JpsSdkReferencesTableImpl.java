// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.module.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementCreator;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.module.JpsSdkReferencesTable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JpsSdkReferencesTableImpl extends JpsCompositeElementBase<JpsSdkReferencesTableImpl> implements JpsSdkReferencesTable {
  public static final JpsSdkReferencesTableRole ROLE = new JpsSdkReferencesTableRole();
  private static final ConcurrentMap<JpsSdkType, JpsSdkReferenceRole> ourReferenceRoles = new ConcurrentHashMap<>();

  public JpsSdkReferencesTableImpl() {
    super();
  }

  private JpsSdkReferencesTableImpl(JpsSdkReferencesTableImpl original) {
    super(original);
  }

  @Override
  public @NotNull JpsSdkReferencesTableImpl createCopy() {
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
  private static @NotNull <P extends JpsElement> JpsSdkReferenceRole<P> getSdkReferenceRole(@NotNull JpsSdkType<P> type) {
    JpsSdkReferenceRole<P> role = ourReferenceRoles.get(type);
    if (role != null) return role;
    ourReferenceRoles.putIfAbsent(type, new JpsSdkReferenceRole<>(type));
    return ourReferenceRoles.get(type);
  }

  private static class JpsSdkReferencesTableRole extends JpsElementChildRoleBase<JpsSdkReferencesTable> implements JpsElementCreator<JpsSdkReferencesTable> {
    JpsSdkReferencesTableRole() {
      super("sdk references");
    }

    @Override
    public @NotNull JpsSdkReferencesTable create() {
      return new JpsSdkReferencesTableImpl();
    }
  }
}
