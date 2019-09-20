/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.ManifestFileProvider;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.packaging.ui.ManifestFileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
* @author nik
*/
public class DefaultManifestFileProvider implements ManifestFileProvider {
  private final PackagingElementResolvingContext myContext;

  public DefaultManifestFileProvider(PackagingElementResolvingContext context) {
    myContext = context;
  }

  @Override
  public List<String> getClasspathFromManifest(@NotNull CompositePackagingElement<?> archiveRoot, @NotNull ArtifactType artifactType) {
    final VirtualFile manifestFile = ManifestFileUtil.findManifestFile(archiveRoot, myContext, artifactType);
    if (manifestFile == null) {
      return null;
    }

    ManifestFileConfiguration configuration = ManifestFileUtil.createManifestFileConfiguration(manifestFile);
    return configuration.getClasspath();
  }
}
