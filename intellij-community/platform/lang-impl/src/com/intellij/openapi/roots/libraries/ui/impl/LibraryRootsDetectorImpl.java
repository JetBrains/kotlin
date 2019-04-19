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
package com.intellij.openapi.roots.libraries.ui.impl;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.libraries.LibraryRootType;
import com.intellij.openapi.roots.libraries.ui.DetectedLibraryRoot;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsDetector;
import com.intellij.openapi.roots.libraries.ui.RootDetector;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public class LibraryRootsDetectorImpl extends LibraryRootsDetector {
  private final List<? extends RootDetector> myDetectors;

  public LibraryRootsDetectorImpl(List<? extends RootDetector> detectors) {
    myDetectors = detectors;
  }

  @Override
  public Collection<DetectedLibraryRoot> detectRoots(@NotNull VirtualFile rootCandidate, @NotNull ProgressIndicator progressIndicator) {
    List<DetectedLibraryRoot> result = new ArrayList<>();
    for (RootDetector detector : myDetectors) {
      final Collection<VirtualFile> files = detector.detectRoots(rootCandidate, progressIndicator);
      for (VirtualFile file : files) {
        result.add(new DetectedLibraryRoot(file, detector.getRootType(), detector.isJarDirectory()));
      }
    }
    return result;
  }

  @Override
  public String getRootTypeName(@NotNull LibraryRootType rootType) {
    for (RootDetector detector : myDetectors) {
      if (detector.getRootType().equals(rootType.getType()) && detector.isJarDirectory() == rootType.isJarDirectory()) {
        return detector.getPresentableRootTypeName();
      }
    }
    return null;
  }
}
