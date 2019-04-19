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
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementType;
import org.jetbrains.jps.model.ex.JpsCompositeElementBase;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.jps.model.module.JpsTypedModuleSourceRoot;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.Set;

/**
 * @author nik
 */
public class JpsModuleSourceRootImpl<P extends JpsElement> extends JpsCompositeElementBase<JpsModuleSourceRootImpl<P>> implements JpsTypedModuleSourceRoot<P> {
  private final JpsModuleSourceRootType<P> myRootType;
  private final String myUrl;

  public JpsModuleSourceRootImpl(String url, JpsModuleSourceRootType<P> type, P properties) {
    super();
    myRootType = type;
    myContainer.setChild(type.getPropertiesRole(), properties);
    myUrl = url;
  }

  private JpsModuleSourceRootImpl(JpsModuleSourceRootImpl<P> original) {
    super(original);
    myRootType = original.myRootType;
    myUrl = original.myUrl;
  }

  @Override
  public <P extends JpsElement> P getProperties(@NotNull JpsModuleSourceRootType<P> type) {
    if (myRootType.equals(type)) {
      //noinspection unchecked
      return (P)myContainer.getChild(myRootType.getPropertiesRole());
    }
    return null;
  }

  @Nullable
  @Override
  public <P extends JpsElement> P getProperties(@NotNull Set<? extends JpsModuleSourceRootType<P>> types) {
    if (types.contains(myRootType)) {
      return (P)getProperties();
    }
    return null;
  }

  @Nullable
  @Override
  public <P extends JpsElement> JpsTypedModuleSourceRoot<P> asTyped(@NotNull JpsModuleSourceRootType<P> type) {
    //noinspection unchecked
    return myRootType.equals(type) ? (JpsTypedModuleSourceRoot<P>)this : null;
  }

  @NotNull
  @Override
  public JpsTypedModuleSourceRoot<?> asTyped() {
    return this;
  }

  @Override
  public JpsElementType<?> getType() {
    return myRootType;
  }

  @NotNull
  @Override
  public P getProperties() {
    return myContainer.getChild(myRootType.getPropertiesRole());
  }

  @NotNull
  @Override
  public JpsModuleSourceRootType<P> getRootType() {
    return myRootType;
  }

  @Override
  @NotNull
  public String getUrl() {
    return myUrl;
  }

  @NotNull
  @Override
  public File getFile() {
    return JpsPathUtil.urlToFile(myUrl);
  }

  @NotNull
  @Override
  public JpsModuleSourceRootImpl<P> createCopy() {
    return new JpsModuleSourceRootImpl<>(this);
  }
}
