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
package com.intellij.packaging.elements;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public abstract class PackagingElementFactory {

  public static PackagingElementFactory getInstance() {
    return ServiceManager.getService(PackagingElementFactory.class);
  }

  @NotNull
  public abstract ArtifactRootElement<?> createArtifactRootElement();

  @NotNull
  public abstract CompositePackagingElement<?> createDirectory(@NotNull @NonNls String directoryName);

  @NotNull
  public abstract CompositePackagingElement<?> createArchive(@NotNull @NonNls String archiveFileName);

  public abstract PackagingElement<?> createFileCopy(@NotNull String filePath, @Nullable String outputFileName);

  @NotNull
  public abstract PackagingElement<?> createModuleOutput(@NotNull String moduleName, @NotNull Project project);

  @NotNull
  public abstract PackagingElement<?> createModuleOutput(@NotNull Module module);

  @NotNull
  public abstract PackagingElement<?> createModuleSource(@NotNull Module module);

  @NotNull
  public abstract PackagingElement<?> createTestModuleOutput(@NotNull Module module);

  @NotNull
  public abstract List<? extends PackagingElement<?>> createLibraryElements(@NotNull Library library);

  @NotNull
  public abstract PackagingElement<?> createArtifactElement(@NotNull ArtifactPointer artifactPointer, @NotNull Project project);

  @NotNull
  public abstract PackagingElement<?> createArtifactElement(@NotNull Artifact artifact, @NotNull Project project);

  @NotNull
  public abstract PackagingElement<?> createLibraryFiles(@NotNull String libraryName, @NotNull String level, String moduleName);


  @NotNull
  public abstract PackagingElement<?> createDirectoryCopyWithParentDirectories(@NotNull String filePath, @NotNull String relativeOutputPath);

  @NotNull
  public abstract PackagingElement<?> createExtractedDirectoryWithParentDirectories(@NotNull String jarPath, @NotNull String pathInJar,
                                                                                    @NotNull String relativeOutputPath);

  @NotNull
  public abstract PackagingElement<?> createExtractedDirectory(@NotNull VirtualFile jarEntry);

  @NotNull
  public abstract PackagingElement<?> createFileCopyWithParentDirectories(@NotNull String filePath, @NotNull String relativeOutputPath,
                                                                          @Nullable String outputFileName);
  
  @NotNull
  public abstract PackagingElement<?> createFileCopyWithParentDirectories(@NotNull String filePath, @NotNull String relativeOutputPath);
  

  @NotNull
  public abstract CompositePackagingElement<?> getOrCreateDirectory(@NotNull CompositePackagingElement<?> parent, @NotNull String relativePath);

  @NotNull
  public abstract CompositePackagingElement<?> getOrCreateArchive(@NotNull CompositePackagingElement<?> parent, @NotNull String relativePath);

  public abstract void addFileCopy(@NotNull CompositePackagingElement<?> root, @NotNull String outputDirectoryPath, @NotNull String sourceFilePath,
                                   final String outputFileName);

  public abstract void addFileCopy(@NotNull CompositePackagingElement<?> root, @NotNull String outputDirectoryPath, @NotNull String sourceFilePath);

  @NotNull
  public abstract PackagingElement<?> createParentDirectories(@NotNull String relativeOutputPath, @NotNull PackagingElement<?> element);


  @NotNull
  public abstract List<? extends PackagingElement<?>> createParentDirectories(@NotNull String relativeOutputPath, @NotNull List<? extends PackagingElement<?>> elements);

  @NotNull
  
  public abstract CompositePackagingElementType<?>[] getCompositeElementTypes();

  @Nullable
  public abstract PackagingElementType<?> findElementType(String id);

  @NotNull
  public abstract PackagingElementType<?>[] getNonCompositeElementTypes();

  @NotNull
  public abstract PackagingElementType[] getAllElementTypes();

  @NotNull
  public abstract ComplexPackagingElementType<?>[] getComplexElementTypes();
}
