// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import com.intellij.util.indexing.FileContent;
import com.intellij.util.io.PathKt;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class FrameworkDetectorRegistryImpl extends FrameworkDetectorRegistry implements Disposable {
  private static final Logger LOG = Logger.getInstance(FrameworkDetectorRegistryImpl.class);
  private static final int REGISTRY_VERSION = 0;
  private Object2IntMap<String> myDetectorIds;
  private Int2ObjectMap<FrameworkDetector> myDetectorById;
  private MultiMap<FileType, Integer> myDetectorsByFileType;
  private int myDetectorsVersion;
  private volatile MultiMap<FileType, Pair<ElementPattern<FileContent>, Integer>> myDetectorsMap;
  private volatile Set<FileType> myAcceptedTypes;
  private final AtomicInteger myNextId = new AtomicInteger();

  public FrameworkDetectorRegistryImpl() {
    loadDetectors();
    FrameworkDetector.EP_NAME.getPoint().addExtensionPointListener(new ExtensionPointListener<FrameworkDetector>() {
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
        if (myDetectorIds.containsKey(detector.getDetectorId())) {
          int id = myDetectorIds.removeInt(detector.getDetectorId());
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

    myDetectorIds = new Object2IntOpenHashMap<>();
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
    myDetectorById = new Int2ObjectOpenHashMap<>();
    myDetectorsByFileType = new MultiMap<>();
    for (FrameworkDetector detector : FrameworkDetector.EP_NAME.getExtensions()) {
      final int id = myDetectorIds.getInt(detector.getDetectorId());
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
        output.writeInt(myDetectorIds.getInt(detector.getDetectorId()));
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
    List<FrameworkType> types = new ArrayList<>();
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
    return myDetectorIds.getInt(detector.getDetectorId());
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
  public int[] getAllDetectorIds() {
    return myDetectorIds.values().toIntArray();
  }

  @Override
  public void dispose() {
  }
}
