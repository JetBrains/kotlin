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
package com.intellij.openapi.compiler.generic;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated this class is part of the old deprecated build infrastructure; plug into the external build process instead (see {@link org.jetbrains.jps.builders.BuildTarget})
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "192.0")
public class VirtualFileWithDependenciesState {
  public static final DataExternalizer<VirtualFileWithDependenciesState> EXTERNALIZER = new VirtualFileWithDependenciesExternalizer();
  private final long mySourceTimestamp;
  private final Map<String, Long> myDependencies = new HashMap<>();

  public VirtualFileWithDependenciesState(long sourceTimestamp) {
    mySourceTimestamp = sourceTimestamp;
  }

  public void addDependency(@NotNull VirtualFile file) {
    myDependencies.put(file.getUrl(), file.getTimeStamp());
  }

  public boolean isUpToDate(@NotNull VirtualFile sourceFile) {
    if (sourceFile.getTimeStamp() != mySourceTimestamp) {
      return false;
    }

    VirtualFileManager manager = VirtualFileManager.getInstance();
    for (Map.Entry<String, Long> entry : myDependencies.entrySet()) {
      final VirtualFile file = manager.findFileByUrl(entry.getKey());
      if (file == null || file.getTimeStamp() != entry.getValue()) {
        return false;
      }
    }
    return true;
  }


  private static class VirtualFileWithDependenciesExternalizer implements DataExternalizer<VirtualFileWithDependenciesState> {
    @Override
    public void save(@NotNull DataOutput out, VirtualFileWithDependenciesState value) throws IOException {
      out.writeLong(value.mySourceTimestamp);
      final Map<String, Long> dependencies = value.myDependencies;
      out.writeInt(dependencies.size());
      for (Map.Entry<String, Long> entry : dependencies.entrySet()) {
        IOUtil.writeUTF(out, entry.getKey());
        out.writeLong(entry.getValue());
      }
    }

    @Override
    public VirtualFileWithDependenciesState read(@NotNull DataInput in) throws IOException {
      final VirtualFileWithDependenciesState state = new VirtualFileWithDependenciesState(in.readLong());
      int size = in.readInt();
      while (size-- > 0) {
        final String url = IOUtil.readUTF(in);
        final long timestamp = in.readLong();
        state.myDependencies.put(url, timestamp);
      }
      return state;
    }
  }
}
