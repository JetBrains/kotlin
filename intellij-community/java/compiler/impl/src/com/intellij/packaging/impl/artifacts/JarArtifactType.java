/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.icons.AllIcons;
import com.intellij.packaging.artifacts.ArtifactTemplate;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.elements.PackagingElementOutputKind;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class JarArtifactType extends ArtifactType {
  public JarArtifactType() {
    super("jar", "JAR");
  }

  public static JarArtifactType getInstance() {
    return EP_NAME.findExtension(JarArtifactType.class);
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public String getDefaultPathFor(@NotNull PackagingElementOutputKind kind) {
    return "/";
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
    return PackagingElementFactory.getInstance().createArchive(ArtifactUtil.suggestArtifactFileName(artifactName) + ".jar");
  }

  @NotNull
  @Override
  public List<? extends ArtifactTemplate> getNewArtifactTemplates(@NotNull PackagingElementResolvingContext context) {
    return Collections.singletonList(new JarFromModulesTemplate(context));
  }
}
