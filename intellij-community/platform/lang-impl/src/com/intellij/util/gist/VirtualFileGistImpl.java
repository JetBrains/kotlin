/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.util.gist;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.FileAttribute;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author peter
 */
class VirtualFileGistImpl<Data> implements VirtualFileGist<Data> {
  private static final Logger LOG = Logger.getInstance(VirtualFileGist.class);
  private static final int ourInternalVersion = 2;

  @NotNull private final String myId;
  private final int myVersion;
  @NotNull private final GistCalculator<Data> myCalculator;
  @NotNull private final DataExternalizer<Data> myExternalizer;

  VirtualFileGistImpl(@NotNull String id, int version, @NotNull DataExternalizer<Data> externalizer, @NotNull GistCalculator<Data> calcData) {
    myId = id;
    myVersion = version;
    myExternalizer = externalizer;
    myCalculator = calcData;
  }

  @Override
  public Data getFileData(@Nullable Project project, @NotNull VirtualFile file) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    ProgressManager.checkCanceled();

    if (!(file instanceof VirtualFileWithId)) return myCalculator.calcData(project, file);

    int stamp = PersistentFS.getInstance().getModificationCount(file) + ((GistManagerImpl)GistManager.getInstance()).getReindexCount();

    try (DataInputStream stream = getFileAttribute(project).readAttribute(file)) {
      if (stream != null && DataInputOutputUtil.readINT(stream) == stamp) {
        return stream.readBoolean() ? myExternalizer.read(stream) : null;
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }

    Data result = myCalculator.calcData(project, file);
    cacheResult(stamp, result, project, file);
    return result;
  }

  private void cacheResult(int modCount, @Nullable Data result, Project project, VirtualFile file) {
    try (DataOutputStream out = getFileAttribute(project).writeAttribute(file)) {
      DataInputOutputUtil.writeINT(out, modCount);
      out.writeBoolean(result != null);
      if (result != null) {
        myExternalizer.save(out, result);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static final Map<Pair<String, Integer>, FileAttribute> ourAttributes =
    FactoryMap.create(key -> new FileAttribute(key.first, key.second, false));

  private FileAttribute getFileAttribute(@Nullable Project project) {
    synchronized (ourAttributes) {
      return ourAttributes.get(Pair.create(myId + (project == null ? "###noProject###" : project.getLocationHash()), myVersion + ourInternalVersion));
    }
  }

}

