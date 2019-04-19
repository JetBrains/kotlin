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

import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementCreator;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.java.ExplodedDirectoryModuleExtension;

/**
 * @author nik
 */
public class ExplodedDirectoryModuleExtensionImpl extends JpsElementBase<ExplodedDirectoryModuleExtensionImpl> implements
                                                                                                               ExplodedDirectoryModuleExtension {
  private String myExplodedUrl;
  private boolean myExcludeExploded;

  public ExplodedDirectoryModuleExtensionImpl() {
  }

  public ExplodedDirectoryModuleExtensionImpl(ExplodedDirectoryModuleExtensionImpl original) {
    myExcludeExploded = original.myExcludeExploded;
    myExplodedUrl = original.myExplodedUrl;
  }

  @Override
  public String getExplodedUrl() {
    return myExplodedUrl;
  }

  @Override
  public void setExplodedUrl(String explodedUrl) {
    if (!Comparing.equal(myExplodedUrl, explodedUrl)) {
      myExplodedUrl = explodedUrl;
      fireElementChanged();
    }
  }

  @Override
  public boolean isExcludeExploded() {
    return myExcludeExploded;
  }

  @Override
  public void setExcludeExploded(boolean excludeExploded) {
    if (myExcludeExploded != excludeExploded) {
      myExcludeExploded = excludeExploded;
      fireElementChanged();
    }
  }

  @NotNull
  @Override
  public ExplodedDirectoryModuleExtensionImpl createCopy() {
    return new ExplodedDirectoryModuleExtensionImpl(this);
  }

  @Override
  public void applyChanges(@NotNull ExplodedDirectoryModuleExtensionImpl modified) {
    setExcludeExploded(modified.myExcludeExploded);
    setExplodedUrl(modified.myExplodedUrl);
  }

  public static class ExplodedDirectoryModuleExtensionRole extends JpsElementChildRoleBase<ExplodedDirectoryModuleExtension> implements JpsElementCreator<ExplodedDirectoryModuleExtension> {
    public static final ExplodedDirectoryModuleExtensionRole INSTANCE = new ExplodedDirectoryModuleExtensionRole();

    public ExplodedDirectoryModuleExtensionRole() {
      super("exploded directory");
    }

    @NotNull
    @Override
    public ExplodedDirectoryModuleExtension create() {
      return new ExplodedDirectoryModuleExtensionImpl();
    }
  }
}
