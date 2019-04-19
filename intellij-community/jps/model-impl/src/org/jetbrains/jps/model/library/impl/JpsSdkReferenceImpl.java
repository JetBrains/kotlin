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
package org.jetbrains.jps.model.library.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsCompositeElement;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementCollection;
import org.jetbrains.jps.model.JpsElementReference;
import org.jetbrains.jps.model.impl.JpsNamedElementReferenceBase;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;

/**
 * @author nik
 */
public class JpsSdkReferenceImpl<P extends JpsElement> extends JpsNamedElementReferenceBase<JpsLibrary, JpsTypedLibrary<JpsSdk<P>>, JpsSdkReferenceImpl<P>> implements JpsSdkReference<P> {
  private final JpsSdkType<P> mySdkType;

  public JpsSdkReferenceImpl(@NotNull String elementName, @NotNull JpsSdkType<P> sdkType,
                             @NotNull JpsElementReference<? extends JpsCompositeElement> parentReference) {
    super(elementName, parentReference);
    mySdkType = sdkType;
  }

  private JpsSdkReferenceImpl(JpsSdkReferenceImpl<P> original) {
    super(original);
    mySdkType = original.mySdkType;
  }

  @Override
  @NotNull
  public String getSdkName() {
    return myElementName;
  }

  @Override
  protected JpsTypedLibrary<JpsSdk<P>> resolve(JpsLibrary element) {
    return element.asTyped(mySdkType);
  }

  @NotNull
  @Override
  public JpsSdkReferenceImpl<P> createCopy() {
    return new JpsSdkReferenceImpl<>(this);
  }

  @Override
  @Nullable
  protected JpsElementCollection<? extends JpsLibrary> getCollection(@NotNull JpsCompositeElement parent) {
    return parent.getContainer().getChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE);
  }
}
