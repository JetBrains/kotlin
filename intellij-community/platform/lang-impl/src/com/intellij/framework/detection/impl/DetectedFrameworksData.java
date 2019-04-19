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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author nik
 */
public class DetectedFrameworksData {
  private static final Logger LOG = Logger.getInstance("#com.intellij.framework.detection.impl.DetectedFrameworksData");
  private PersistentHashMap<Integer,TIntHashSet> myExistentFrameworkFiles;
  private final TIntObjectHashMap<TIntHashSet> myNewFiles;
  private final MultiMap<Integer, DetectedFrameworkDescription> myDetectedFrameworks;

  public DetectedFrameworksData(Project project) {
    myDetectedFrameworks = new MultiMap<>();
    File file = ProjectUtil.getProjectCachePath(project, FrameworkDetectorRegistryImpl.getDetectionDirPath(), true).resolve("files").toFile();
    myNewFiles = new TIntObjectHashMap<>();
    try {
      myExistentFrameworkFiles = new PersistentHashMap<>(file, EnumeratorIntegerDescriptor.INSTANCE, new TIntHashSetExternalizer());
    }
    catch (IOException e) {
      LOG.info(e);
      PersistentHashMap.deleteFilesStartingWith(file);
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

  public Collection<VirtualFile> retainNewFiles(@NotNull Integer detectorId, @NotNull Collection<VirtualFile> files) {
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

  public Set<Integer> getDetectorsForDetectedFrameworks() {
    return myDetectedFrameworks.keySet();
  }

  public Collection<? extends DetectedFrameworkDescription> updateFrameworksList(Integer detectorId,
                                                                                 Collection<? extends DetectedFrameworkDescription> frameworks) {
    final Collection<DetectedFrameworkDescription> oldFrameworks = myDetectedFrameworks.remove(detectorId);
    myDetectedFrameworks.putValues(detectorId, frameworks);
    if (oldFrameworks != null) {
      frameworks.removeAll(oldFrameworks);
    }
    return frameworks;
  }

  public void putExistentFrameworkFiles(Integer id, Collection<? extends VirtualFile> files) {
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
