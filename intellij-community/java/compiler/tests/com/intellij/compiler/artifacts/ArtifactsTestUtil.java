/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.compiler.artifacts;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.packaging.impl.elements.DirectoryPackagingElement;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author nik
 */
public class ArtifactsTestUtil {
  public static String printToString(PackagingElement element, int level) {
    StringBuilder builder = new StringBuilder(StringUtil.repeatSymbol(' ', level));
    if (element instanceof ArchivePackagingElement) {
      builder.append(((ArchivePackagingElement)element).getArchiveFileName());
    }
    else if (element instanceof DirectoryPackagingElement) {
      builder.append(((DirectoryPackagingElement)element).getDirectoryName()).append("/");
    }
    else {
      builder.append(element.toString());
    }
    builder.append("\n");
    if (element instanceof CompositePackagingElement) {
      for (PackagingElement<?> child : ((CompositePackagingElement<?>)element).getChildren()) {
        builder.append(printToString(child, level + 1));
      }
    }
    return builder.toString();
  }

  public static void assertLayout(PackagingElement element, String expected) {
    assertEquals(adjustMultiLine(expected), printToString(element, 0));
  }

  private static String adjustMultiLine(String expected) {
    final List<String> strings = StringUtil.split(StringUtil.trimStart(expected, "\n"), "\n");
    int min = Integer.MAX_VALUE;
    for (String s : strings) {
      int k = 0;
      while (k < s.length() && s.charAt(k) == ' ') {
        k++;
      }
      min = Math.min(min, k);
    }
    List<String> lines = new ArrayList<>();
    for (String s : strings) {
      lines.add(s.substring(min));
    }
    return StringUtil.join(lines, "\n") + "\n";      
  }

  public static void assertLayout(Project project, String artifactName, String expected) {
    assertLayout(findArtifact(project, artifactName).getRootElement(), expected);
  }

  public static void assertOutputPath(Project project, String artifactName, String expected) {
    assertEquals(expected, findArtifact(project, artifactName).getOutputPath());
  }

  public static void assertOutputFileName(Project project, String artifactName, String expected) {
    assertEquals(expected, findArtifact(project, artifactName).getRootElement().getName());
  }

  public static void setOutput(final Project project, final String artifactName, final String outputPath) {
    WriteAction.runAndWait(() -> {
      final ModifiableArtifactModel model = ArtifactManager.getInstance(project).createModifiableModel();
      model.getOrCreateModifiableArtifact(findArtifact(project, artifactName)).setOutputPath(outputPath);
      model.commit();
    });
  }

  public static void addArtifactToLayout(final Project project, final Artifact parent, final Artifact toAdd) {
    WriteAction.runAndWait(() -> {
      final ModifiableArtifactModel model = ArtifactManager.getInstance(project).createModifiableModel();
      final PackagingElement<?> artifactElement = PackagingElementFactory.getInstance().createArtifactElement(toAdd, project);
      model.getOrCreateModifiableArtifact(parent).getRootElement().addOrFindChild(artifactElement);
      model.commit();
    });
  }

  public static Artifact findArtifact(Project project, String artifactName) {
    final ArtifactManager manager = ArtifactManager.getInstance(project);
    final Artifact artifact = manager.findArtifact(artifactName);
    assertNotNull("'" + artifactName + "' artifact not found", artifact);
    return artifact;
  }

  public static void assertManifest(Artifact artifact, PackagingElementResolvingContext context, @Nullable String mainClass, @Nullable String classpath) {
    final CompositePackagingElement<?> rootElement = artifact.getRootElement();
    final ArtifactType type = artifact.getArtifactType();
    assertManifest(rootElement, context, type, mainClass, classpath);
  }

  public static void assertManifest(CompositePackagingElement<?> rootElement,
                                     PackagingElementResolvingContext context,
                                     ArtifactType type,
                                     @Nullable String mainClass, @Nullable String classpath) {
    final VirtualFile file = ManifestFileUtil.findManifestFile(rootElement, context, type);
    assertNotNull(file);
    final Manifest manifest = ManifestFileUtil.readManifest(file);
    assertEquals(mainClass, manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS));
    assertEquals(classpath, manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH));
  }

}
