// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.framework.detection.impl;

import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.PersistentHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class DetectedFrameworksData {
  private static final Logger LOG = Logger.getInstance(DetectedFrameworksData.class);
  private PersistentHashMap<Integer, TIntHashSet> myExistentFrameworkFiles;
  private final TIntObjectHashMap<TIntHashSet> myNewFiles;
  private final MultiMap<Integer, DetectedFrameworkDescription> myDetectedFrameworks;
  private final Object myLock = new Object();

  public DetectedFrameworksData(@NotNull Project project) {
    myDetectedFrameworks = new MultiMap<>();
    Path file = ProjectUtil.getProjectCachePath(project, FrameworkDetectorRegistryImpl.getDetectionDirPath(), true).resolve("files");
    myNewFiles = new TIntObjectHashMap<>();
    try {
      myExistentFrameworkFiles = new PersistentHashMap<>(file, EnumeratorIntegerDescriptor.INSTANCE, new TIntHashSetExternalizer());
    }
    catch (IOException e) {
      LOG.info(e);
      PersistentHashMap.deleteFilesStartingWith(file.toFile());
      try {
        myExistentFrameworkFiles = new PersistentHashMap<>(file, EnumeratorIntegerDescriptor.INSTANCE, new TIntHashSetExternalizer());
      }
      catch (IOException e1) {
        LOG.error(e1);
      }
    }
  }

  public void saveDetected() {
    try {
      myExistentFrameworkFiles.close();
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  public Collection<VirtualFile> retainNewFiles(@NotNull Integer detectorId, @NotNull Collection<? extends VirtualFile> files) {
    synchronized (myLock) {
      TIntHashSet oldSet = myNewFiles.get(detectorId);
      if (oldSet == null) {
        oldSet = new TIntHashSet();
        myNewFiles.put(detectorId, oldSet);
      }

      TIntHashSet existentFilesSet = null;
      try {
        existentFilesSet = myExistentFrameworkFiles.get(detectorId);
      }
      catch (IOException e) {
        LOG.info(e);
      }
      final ArrayList<VirtualFile> newFiles = new ArrayList<>();
      TIntHashSet newSet = new TIntHashSet();
      for (VirtualFile file : files) {
        final int fileId = FileBasedIndex.getFileId(file);
        if (existentFilesSet == null || !existentFilesSet.contains(fileId)) {
          newFiles.add(file);
          newSet.add(fileId);
        }
      }
      if (newSet.equals(oldSet)) {
        return Collections.emptyList();
      }
      myNewFiles.put(detectorId, newSet);
      return newFiles;
    }
  }

  public Set<Integer> getDetectorsForDetectedFrameworks() {
    synchronized (myLock) {
      return new HashSet<>(myDetectedFrameworks.keySet());
    }
  }

  public Collection<? extends DetectedFrameworkDescription> updateFrameworksList(Integer detectorId,
                                                                                 Collection<? extends DetectedFrameworkDescription> frameworks) {
    synchronized (myLock) {
      final Collection<DetectedFrameworkDescription> oldFrameworks = myDetectedFrameworks.remove(detectorId);
      myDetectedFrameworks.putValues(detectorId, frameworks);
      if (oldFrameworks != null) {
        frameworks.removeAll(oldFrameworks);
      }
      return frameworks;
    }
  }

  public void putExistentFrameworkFiles(Integer id, Collection<? extends VirtualFile> files) {
    synchronized (myLock) {
      TIntHashSet set = null;
      try {
        set = myExistentFrameworkFiles.get(id);
      }
      catch (IOException e) {
        LOG.info(e);
      }
      if (set == null) {
        set = new TIntHashSet();
        try {
          myExistentFrameworkFiles.put(id, set);
        }
        catch (IOException e) {
          LOG.info(e);
        }
      }
      for (VirtualFile file : files) {
        set.add(FileBasedIndex.getFileId(file));
      }
    }
  }

  private static class TIntHashSetExternalizer implements DataExternalizer<TIntHashSet> {
    @Override
    public void save(@NotNull DataOutput out, TIntHashSet value) throws IOException {
      out.writeInt(value.size());
      final TIntIterator iterator = value.iterator();
      while (iterator.hasNext()) {
        out.writeInt(iterator.next());
      }
    }

    @Override
    public TIntHashSet read(@NotNull DataInput in) throws IOException {
      int size = in.readInt();
      final TIntHashSet set = new TIntHashSet(size);
      while (size-- > 0) {
        set.add(in.readInt());
      }
      return set;
    }
  }
}
