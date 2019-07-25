// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.impl.elements.FileOrDirectoryCopyPackagingElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
final class ArtifactVirtualFileListener implements BulkFileListener {
  private final CachedValue<MultiValuesMap<String, Artifact>> myParentPathsToArtifacts;
  private final ArtifactManagerImpl myArtifactManager;

  ArtifactVirtualFileListener(@NotNull Project project, @NotNull ArtifactManagerImpl artifactManager) {
    myArtifactManager = artifactManager;
    myParentPathsToArtifacts =
      CachedValuesManager.getManager(project).createCachedValue(() -> {
        MultiValuesMap<String, Artifact> result = computeParentPathToArtifactMap();
        return CachedValueProvider.Result.createSingleDependency(result, artifactManager.getModificationTracker());
      }, false);
  }

  private MultiValuesMap<String, Artifact> computeParentPathToArtifactMap() {
    final MultiValuesMap<String, Artifact> result = new MultiValuesMap<>();
    for (final Artifact artifact : myArtifactManager.getArtifacts()) {
      ArtifactUtil.processFileOrDirectoryCopyElements(artifact, new PackagingElementProcessor<FileOrDirectoryCopyPackagingElement<?>>() {
        @Override
        public boolean process(@NotNull FileOrDirectoryCopyPackagingElement<?> element, @NotNull PackagingElementPath pathToElement) {
          String path = element.getFilePath();
          while (path.length() > 0) {
            result.put(path, artifact);
            path = PathUtil.getParentPath(path);
          }
          return true;
        }
      }, myArtifactManager.getResolvingContext(), false);
    }
    return result;
  }

  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (event instanceof VFileMoveEvent) {
        filePathChanged(((VFileMoveEvent)event).getOldPath(), event.getPath());
      }
      else if (event instanceof VFilePropertyChangeEvent) {
        propertyChanged((VFilePropertyChangeEvent)event);
      }
    }
  }

  private void filePathChanged(@NotNull final String oldPath, @NotNull final String newPath) {
    final Collection<Artifact> artifacts = myParentPathsToArtifacts.getValue().get(oldPath);
    if (artifacts != null) {
      final ModifiableArtifactModel model = myArtifactManager.createModifiableModel();
      for (Artifact artifact : artifacts) {
        final Artifact copy = model.getOrCreateModifiableArtifact(artifact);
        ArtifactUtil.processFileOrDirectoryCopyElements(copy, new PackagingElementProcessor<FileOrDirectoryCopyPackagingElement<?>>() {
          @Override
          public boolean process(@NotNull FileOrDirectoryCopyPackagingElement<?> element, @NotNull PackagingElementPath pathToElement) {
            final String path = element.getFilePath();
            if (FileUtil.startsWith(path, oldPath)) {
              element.setFilePath(newPath + path.substring(oldPath.length()));
            }
            return true;
          }
        }, myArtifactManager.getResolvingContext(), false);
      }
      model.commit();
    }
  }

  private void propertyChanged(@NotNull VFilePropertyChangeEvent event) {
    if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
      final VirtualFile parent = event.getFile().getParent();
      if (parent != null) {
        String parentPath = parent.getPath();
        filePathChanged(parentPath + "/" + event.getOldValue(), parentPath + "/" + event.getNewValue());
      }
    }
  }
}
