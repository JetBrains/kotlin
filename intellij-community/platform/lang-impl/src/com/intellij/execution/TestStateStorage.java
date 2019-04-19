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
package com.intellij.execution;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Dmitry Avdeev
 */
public class TestStateStorage implements Disposable {
  
  private static final File TEST_HISTORY_PATH = new File(PathManager.getSystemPath(), "testHistory");

  private static final int CURRENT_VERSION = 5;
  
  private final File myFile;

  public static File getTestHistoryRoot(Project project) {
    return new File(TEST_HISTORY_PATH, project.getLocationHash());
  }

  public static class Record {
    public final int magnitude;
    public final long configurationHash;
    public final Date date;
    public int failedLine;
    public final String failedMethod;
    public final String errorMessage;
    public final String topStacktraceLine;

    public Record(int magnitude, Date date, long configurationHash, int failLine, String method, String errorMessage, String topStacktraceLine) {
      this.magnitude = magnitude;
      this.date = date;
      this.configurationHash = configurationHash;
      this.failedLine = failLine;
      failedMethod = method;
      this.errorMessage = errorMessage;
      this.topStacktraceLine = topStacktraceLine;
    }
  }

  private static final Logger LOG = Logger.getInstance(TestStateStorage.class);
  @Nullable
  private PersistentHashMap<String, Record> myMap;
  private volatile ScheduledFuture<?> myMapFlusher;

  public static TestStateStorage getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, TestStateStorage.class);
  }

  public TestStateStorage(Project project) {
    String directoryPath = getTestHistoryRoot(project).getPath();

    myFile = new File(directoryPath + "/testStateMap");
    FileUtilRt.createParentDirs(myFile);

    try {
      myMap = initializeMap();
    } catch (IOException e) {
      LOG.error(e);
    }
    myMapFlusher = FlushingDaemon.everyFiveSeconds(this::flushMap);
  }

  private PersistentHashMap<String, Record> initializeMap() throws IOException {
    return IOUtil.openCleanOrResetBroken(getComputable(myFile), myFile);
  }

  private synchronized void flushMap() {
    if (myMapFlusher == null) return; // disposed
    if (myMap != null && myMap.isDirty()) myMap.force();
  }

  @NotNull
  private static ThrowableComputable<PersistentHashMap<String, Record>, IOException> getComputable(final File file) {
    return () -> new PersistentHashMap<>(file, EnumeratorStringDescriptor.INSTANCE, new DataExternalizer<Record>() {
      @Override
      public void save(@NotNull DataOutput out, Record value) throws IOException {
        out.writeInt(value.magnitude);
        out.writeLong(value.date.getTime());
        out.writeLong(value.configurationHash);
        out.writeInt(value.failedLine);
        out.writeUTF(StringUtil.notNullize(value.failedMethod));
        out.writeUTF(StringUtil.notNullize(value.errorMessage));
        out.writeUTF(StringUtil.notNullize(value.topStacktraceLine));
      }

      @Override
      public Record read(@NotNull DataInput in) throws IOException {
        return new Record(in.readInt(), new Date(in.readLong()), in.readLong(), in.readInt(), in.readUTF(), in.readUTF(), in.readUTF());
      }
    }, 4096, CURRENT_VERSION);
  }

  @NotNull
  public synchronized Collection<String> getKeys() {
    try {
      return myMap == null ? Collections.emptyList() : myMap.getAllKeysWithExistingMapping();
    }
    catch (IOException e) {
      thingsWentWrongLetsReinitialize(e, "Can't get keys");
      return Collections.emptyList();
    }
  }

  @Nullable
  public synchronized Record getState(String testUrl) {
    try {
      return myMap == null ? null : myMap.get(testUrl);
    }
    catch (IOException e) {
      thingsWentWrongLetsReinitialize(e, "Can't get state for " + testUrl);
      return null;
    }
  }

  public synchronized void removeState(String url) {
    if (myMap != null) {
      try {
        myMap.remove(url);
      }
      catch (IOException e) {
        thingsWentWrongLetsReinitialize(e, "Can't remove state for " + url);
      }
    }
  }

  @Nullable
  public synchronized Map<String, Record> getRecentTests(int limit, Date since) {
    if (myMap == null) return null;

    Map<String, Record> result = ContainerUtil.newHashMap();
    try {
      for (String key : myMap.getAllKeysWithExistingMapping()) {
        Record record = myMap.get(key);
        if (record != null && record.date.compareTo(since) > 0) {
          result.put(key, record);
          if (result.size() >= limit) {
            break;
          }
        }
      }
    }
    catch (IOException e) {
      thingsWentWrongLetsReinitialize(e, "Can't get recent tests");
    }

    return result;
  }

  public synchronized void writeState(@NotNull String testUrl, Record record) {
    if (myMap == null) return;
    try {
      myMap.put(testUrl, record);
    }
    catch (IOException e) {
      thingsWentWrongLetsReinitialize(e, "Can't write state for " + testUrl);
    }
  }

  @Override
  public synchronized void dispose() {
    myMapFlusher.cancel(false);
    myMapFlusher = null;
    if (myMap == null) return;
    try {
      myMap.close();
    }
    catch (IOException e) {
      LOG.error(e);
    }
    finally {
      myMap = null;
    }
  }

  private void thingsWentWrongLetsReinitialize(IOException e, String message) {
    try {
      if (myMap != null) {
        try {
          myMap.close();
        }
        catch (IOException ignore) {
        }
        IOUtil.deleteAllFilesStartingWith(myFile);
      }
      myMap = initializeMap();
      LOG.warn(message, e);
    }
    catch (IOException e1) {
      LOG.error("Cannot repair", e1);
      myMap = null;
    }
  }
}
