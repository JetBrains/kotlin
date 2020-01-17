/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.FrameworkDetector;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.io.PathKt;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameworkDetectorRegistryImpl extends FrameworkDetectorRegistry implements Disposable {
  private static final Logger LOG = Logger.getInstance(FrameworkDetectorRegistryImpl.class);
  private static final int REGISTRY_VERSION = 0;
  private TObjectIntHashMap<String> myDetectorIds;
  private TIntObjectHashMap<FrameworkDetector> myDetectorById;
  private MultiMap<FileType, Integer> myDetectorsByFileType;
  private int myDetectorsVersion;
  private volatile MultiMap<FileType, Pair<ElementPattern<FileContent>, Integer>> myDetectorsMap;
  private volatile Set<FileType> myAcceptedTypes;
  private final AtomicInteger myNextId = new AtomicInteger();

  public FrameworkDetectorRegistryImpl() {
    loadDetectors();
    FrameworkDetector.EP_NAME.getPoint(null).addExtensionPointListener(new ExtensionPointListener<FrameworkDetector>() {
      @Override
      public void extensionAdded(@NotNull FrameworkDetector detector, @NotNull PluginDescriptor pluginDescriptor) {
        int id = myNextId.getAndIncrement();
        myDetectorIds.put(detector.getDetectorId(), id);
        myDetectorById.put(id, detector);
        myDetectorsByFileType.putValue(detector.getFileType(), id);
        onDetectorsChanged();
      }

      @Override
      public void extensionRemoved(@NotNull FrameworkDetector detector, @NotNull PluginDescriptor pluginDescriptor) {
        if (myDetectorIds.contains(detector.getDetectorId())) {
          int id = myDetectorIds.remove(detector.getDetectorId());
          myDetectorById.remove(id);
          myDetectorsByFileType.remove(detector.getFileType(), id);
          onDetectorsChanged();
        }
      }
    }, false, this);
    saveDetectors();
  }

  private void loadDetectors() {
    Map<String, FrameworkDetector> newDetectors = new HashMap<>();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      newDetectors.put(detector.getDetectorId(), detector);
    }

    myDetectorIds = new TObjectIntHashMap<>();
    final Path file = getDetectorsRegistryFile();
    int maxId = REGISTRY_VERSION;
    if (Files.exists(file)) {
      LOG.debug("loading framework detectors registry from " + file.toAbsolutePath());
      List<String> unknownIds = new ArrayList<>();
      boolean versionChanged = false;
      try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
        input.readInt();
        myDetectorsVersion = input.readInt();
        int size = input.readInt();
        while (size-- > REGISTRY_VERSION) {
          final String stringId = input.readUTF();
          int intId = input.readInt();
          maxId = Math.max(maxId, intId);
          final int version = input.readInt();
          final FrameworkDetector detector = newDetectors.remove(stringId);
          if (detector != null) {
            if (version != detector.getDetectorVersion()) {
              LOG.info("Version of framework detector '" + stringId + "' changed: " + version + " -> " + detector.getDetectorVersion());
              versionChanged = true;
            }
            myDetectorIds.put(stringId, intId);
          }
          else {
            unknownIds.add(stringId);
          }
        }
      }
      catch (IOException e) {
        LOG.info(e);
      }
      if (!unknownIds.isEmpty()) {
        LOG.debug("Unknown framework detectors: " + unknownIds);
      }
      if (versionChanged || !newDetectors.isEmpty()) {
        if (!newDetectors.isEmpty()) {
          LOG.info("New framework detectors: " + newDetectors.keySet());
        }
        myDetectorsVersion++;
        LOG.info("Framework detection index version changed to " + myDetectorsVersion);
      }
    }
    myNextId.set(maxId + 1);
    for (String newDetector : newDetectors.keySet()) {
      myDetectorIds.put(newDetector, myNextId.getAndIncrement());
    }
    myDetectorById = new TIntObjectHashMap<>();
    myDetectorsByFileType = new MultiMap<>();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      final int id = myDetectorIds.get(detector.getDetectorId());
      myDetectorsByFileType.putValue(detector.getFileType(), id);
      myDetectorById.put(id, detector);
      LOG.debug("'" + detector.getDetectorId() + "' framework detector: id = " + id);
    }
  }

  private void saveDetectors() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    final Path file = getDetectorsRegistryFile();
    try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(PathKt.outputStream(file)))) {
      output.writeInt(REGISTRY_VERSION);
      output.writeInt(myDetectorsVersion);
      final FrameworkDetector[] detectors = FrameworkDetector.EP_NAME.getExtensions();
      output.writeInt(detectors.length);
      for (FrameworkDetector detector : detectors) {
        output.writeUTF(detector.getDetectorId());
        output.writeInt(myDetectorIds.get(detector.getDetectorId()));
        output.writeInt(detector.getDetectorVersion());
      }
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  @Override
  @NotNull
  public MultiMap<FileType, Pair<ElementPattern<FileContent>, Integer>> getDetectorsMap() {
    MultiMap<FileType, Pair<ElementPattern<FileContent>, Integer>> cached = myDetectorsMap;
    if (cached != null) return cached;

    MultiMap<FileType, Pair<ElementPattern<FileContent>, Integer>> detectorsMap = new MultiMap<>();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      detectorsMap.putValue(detector.getFileType(), Pair.create(detector.createSuitableFilePattern(), getDetectorId(detector)));
    }
    myDetectorsMap = detectorsMap;
    return detectorsMap;
  }

  @Override
  @NotNull
  public Set<FileType> getAcceptedFileTypes() {
    Set<FileType> cached = myAcceptedTypes;
    if (cached != null) return cached;

    Set<FileType> acceptedTypes = new HashSet<>();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      acceptedTypes.add(detector.getFileType());
    }
    myAcceptedTypes = acceptedTypes;
    return acceptedTypes;
  }

  private void onDetectorsChanged() {
    myAcceptedTypes = null;
    myDetectorsMap = null;
    myDetectorsVersion++;
    saveDetectors();
    ApplicationManager.getApplication().invokeLater(() -> FileBasedIndex.getInstance().requestRebuild(FrameworkDetectionIndex.NAME));
  }

  private static Path getDetectorsRegistryFile() {
    return getDetectionDirPath().resolve("detectors-registry.dat");
  }

  @NotNull
  public static Path getDetectionDirPath() {
    return PathManagerEx.getAppSystemDir().resolve("frameworks").resolve("detection");
  }

  @Override
  public FrameworkType findFrameworkType(@NotNull String typeId) {
    for (FrameworkType type : getFrameworkTypes()) {
      if (typeId.equals(type.getId())) {
        return type;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public List<? extends FrameworkType> getFrameworkTypes() {
    final List<FrameworkType> types = new ArrayList<>();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      types.add(detector.getFrameworkType());
    }
    return types;
  }

  @Override
  public int getDetectorsVersion() {
    return myDetectorsVersion;
  }

  @Override
  public int getDetectorId(@NotNull FrameworkDetector detector) {
    return myDetectorIds.get(detector.getDetectorId());
  }

  @Override
  public FrameworkDetector getDetectorById(int id) {
    return myDetectorById.get(id);
  }

  @NotNull
  @Override
  public Collection<Integer> getDetectorIds(@NotNull FileType fileType) {
    return myDetectorsByFileType.get(fileType);
  }

  @Override
  public Collection<Integer> getAllDetectorIds() {
    final int[] ids = myDetectorIds.getValues();
    final List<Integer> result = new ArrayList<>();
    for (int id : ids) {
      result.add(id);
    }
    return result;
  }

  @Override
  public void dispose() {
  }
}
