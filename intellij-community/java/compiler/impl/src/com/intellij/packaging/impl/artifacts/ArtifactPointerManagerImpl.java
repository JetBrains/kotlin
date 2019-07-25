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

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactPointerManagerImpl extends ArtifactPointerManager {
  private final Map<String, ArtifactPointerImpl> myUnresolvedPointers = new HashMap<>();
  private final Map<Artifact, ArtifactPointerImpl> myPointers = new HashMap<>();
  private ArtifactManager myArtifactManager;

  public ArtifactPointerManagerImpl(Project project) {
    project.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, new ArtifactAdapter() {
      @Override
      public void artifactRemoved(@NotNull Artifact artifact) {
        disposePointer(artifact);
      }

      @Override
      public void artifactAdded(@NotNull Artifact artifact) {
        final ArtifactPointerImpl pointer = myPointers.get(artifact);
        if (pointer != null) {
          pointer.setName(artifact.getName());
        }

        final ArtifactPointerImpl unresolved = myUnresolvedPointers.remove(artifact.getName());
        if (unresolved != null) {
          unresolved.setArtifact(artifact);
          if (pointer == null) {
            myPointers.put(artifact, unresolved);
          }
        }
      }

      @Override
      public void artifactChanged(@NotNull Artifact artifact, @NotNull String oldName) {
        artifactAdded(artifact);
      }
    });
  }

  public void setArtifactManager(ArtifactManager artifactManager) {
    myArtifactManager = artifactManager;
  }

  private void disposePointer(Artifact artifact) {
    final ArtifactPointerImpl pointer = myPointers.remove(artifact);
    if (pointer != null) {
      pointer.setArtifact(null);
      myUnresolvedPointers.put(pointer.getArtifactName(), pointer);
    }
  }

  @Override
  public ArtifactPointer createPointer(@NotNull String name) {
    if (myArtifactManager != null) {
      final Artifact artifact = myArtifactManager.findArtifact(name);
      if (artifact != null) {
        return createPointer(artifact);
      }
    }

    ArtifactPointerImpl pointer = myUnresolvedPointers.get(name);
    if (pointer == null) {
      pointer = new ArtifactPointerImpl(name);
      myUnresolvedPointers.put(name, pointer);
    }
    return pointer;
  }

  @Override
  public ArtifactPointer createPointer(@NotNull Artifact artifact) {
    ArtifactPointerImpl pointer = myPointers.get(artifact);
    if (pointer == null) {
      pointer = myUnresolvedPointers.get(artifact.getName());
      if (pointer != null) {
        pointer.setArtifact(artifact);
      }
      else {
        pointer = new ArtifactPointerImpl(artifact);
      }
      myPointers.put(artifact, pointer);
    }
    return pointer;
  }

  @Override
  public ArtifactPointer createPointer(@NotNull Artifact artifact, @NotNull ArtifactModel artifactModel) {
    return createPointer(artifactModel.getOriginalArtifact(artifact));
  }

  public void disposePointers(List<? extends Artifact> artifacts) {
    for (Artifact artifact : artifacts) {
      disposePointer(artifact);
    }
  }
}
