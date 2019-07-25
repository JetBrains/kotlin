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
package org.jetbrains.jps.model.library.impl.sdk;

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementFactory;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.impl.JpsLibraryImpl;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;

/**
 * @author nik
 */
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
    if (!Comparing.equal(myHomePath, homePath)) {
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
    if (!Comparing.equal(myVersionString, versionString)) {
      myVersionString = versionString;
      fireElementChanged();
    }
  }
}
