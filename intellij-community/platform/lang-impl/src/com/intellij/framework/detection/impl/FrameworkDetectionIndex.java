/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl;

import com.intellij.framework.detection.FrameworkDetector;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author nik
 */
public class FrameworkDetectionIndex extends ScalarIndexExtension<Integer> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.framework.detection.impl.FrameworkDetectionIndex");
  public static final ID<Integer,Void> NAME = ID.create("FrameworkDetectionIndex");

  private final EventDispatcher<FrameworkDetectionIndexListener> myDispatcher = EventDispatcher.create(FrameworkDetectionIndexListener.class);

  public static FrameworkDetectionIndex getInstance() {
    return EXTENSION_POINT_NAME.findExtension(FrameworkDetectionIndex.class);
  }

  @NotNull
  @Override
  public ID<Integer, Void> getName() {
    return NAME;
  }

  public void addListener(@NotNull FrameworkDetectionIndexListener listener, @NotNull Disposable parentDisposable) {
    myDispatcher.addListener(listener, parentDisposable);
  }

  @NotNull
  @Override
  public DataIndexer<Integer, Void, FileContent> getIndexer() {
    final MultiMap<FileType, Pair<ElementPattern<FileContent>, Integer>> detectors = new MultiMap<>();
    FrameworkDetectorRegistry registry = FrameworkDetectorRegistry.getInstance();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      detectors.putValue(detector.getFileType(), Pair.create(detector.createSuitableFilePattern(), registry.getDetectorId(detector)));
    }
    return new DataIndexer<Integer, Void, FileContent>() {
      @NotNull
      @Override
      public Map<Integer, Void> map(@NotNull FileContent inputData) {
        final FileType fileType = inputData.getFileType();
        if (!detectors.containsKey(fileType)) {
          return Collections.emptyMap();
        }
        Map<Integer, Void> result = null;
        for (Pair<ElementPattern<FileContent>, Integer> pair : detectors.get(fileType)) {
          if (pair.getFirst().accepts(inputData)) {
            if (LOG.isDebugEnabled()) {
              LOG.debug(inputData.getFile() + " accepted by detector " + pair.getSecond());
            }
            if (result == null) {
              result = new HashMap<>();
            }
            myDispatcher.getMulticaster().fileUpdated(inputData.getFile(), pair.getSecond());
            result.put(pair.getSecond(), null);
          }
        }
        return result != null ? result : Collections.emptyMap();
      }
    };
  }

  @NotNull
  @Override
  public KeyDescriptor<Integer> getKeyDescriptor() {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    final Set<FileType> acceptedTypes = new HashSet<>();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      acceptedTypes.add(detector.getFileType());
    }
    return new DefaultFileTypeSpecificInputFilter(acceptedTypes.toArray(FileType.EMPTY_ARRAY));
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return FrameworkDetectorRegistry.getInstance().getDetectorsVersion();
  }
}
