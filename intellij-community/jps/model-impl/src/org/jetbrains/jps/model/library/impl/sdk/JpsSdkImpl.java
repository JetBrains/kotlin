// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.library.impl.sdk;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.library.impl.JpsLibraryImpl;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;

public class JpsSdkImpl<P extends JpsElement> extends JpsCompositeElementBase<JpsSdkImpl<P>> implements JpsSdk<P> {
  private final JpsSdkType<P> mySdkType;
  private String myHomePath;
  private String myVersionString;

  public JpsSdkImpl(String homePath, String versionString, JpsSdkType<P> type, P properties) {
    myHomePath = homePath;
    myVersionString = versionString;
    mySdkType = type;
    myContainer.setChild(type.getSdkPropertiesRole(), properties);
  }

  private JpsSdkImpl(JpsSdkImpl<P> original) {
    super(original);
    mySdkType = original.mySdkType;
    myHomePath = original.myHomePath;
    myVersionString = original.myVersionString;
  }

  @NotNull
  @Override
  public JpsSdkImpl<P> createCopy() {
    return new JpsSdkImpl<>(this);
  }

  @Override
  public P getSdkProperties() {
    return myContainer.getChild(mySdkType.getSdkPropertiesRole());
  }

  @Override
  public JpsSdkReference<P> createReference() {
    return JpsElementFactory.getInstance().createSdkReference(getParent().getName(), mySdkType);
  }

  @Override
  public JpsSdkType<P> getSdkType() {
    return mySdkType;
  }

  @NotNull
  @Override
  public JpsLibraryImpl<JpsSdk<P>> getParent() {
    //noinspection unchecked
    return (JpsLibraryImpl<JpsSdk<P>>)super.getParent();
  }

  @Override
  public void applyChanges(@NotNull JpsSdkImpl<P> modified) {
    super.applyChanges(modified);
    setHomePath(modified.getHomePath());
    setVersionString(modified.getVersionString());
  }

  @Override
  public String getHomePath() {
    return myHomePath;
  }

  @Override
  public void setHomePath(String homePath) {
    if (!Objects.equals(myHomePath, homePath)) {
      myHomePath = homePath;
      fireElementChanged();
    }
  }

  @Override
  public String getVersionString() {
    return myVersionString;
  }

  @Override
  public void setVersionString(String versionString) {
    if (!Objects.equals(myVersionString, versionString)) {
      myVersionString = versionString;
      fireElementChanged();
    }
  }
}
