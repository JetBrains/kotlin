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

import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @deprecated this class is part of the old deprecated build infrastructure; plug into the external build process instead (see {@link org.jetbrains.jps.builders.BuildTarget})
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "192.0")
public class DummyPersistentState {
  public static final DummyPersistentState INSTANCE = new DummyPersistentState();
  public static final DataExternalizer<DummyPersistentState> EXTERNALIZER = new DummyPersistentStateExternalizer();

  private DummyPersistentState() {
  }

  private static class DummyPersistentStateExternalizer implements DataExternalizer<DummyPersistentState> {
    @Override
    public void save(@NotNull DataOutput out, DummyPersistentState value) throws IOException {
    }

    @Override
    public DummyPersistentState read(@NotNull DataInput in) throws IOException {
      return INSTANCE;
    }
  }
}
