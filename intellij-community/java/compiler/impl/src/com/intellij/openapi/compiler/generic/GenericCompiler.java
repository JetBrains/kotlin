/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated this class is part of the old deprecated build infrastructure; plug into the external build process instead
 * (see {@link org.jetbrains.jps.incremental.TargetBuilder})
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "192.0")
public abstract class GenericCompiler<Key, SourceState, OutputState> implements Compiler {
  protected static final KeyDescriptor<String> STRING_KEY_DESCRIPTOR = EnumeratorStringDescriptor.INSTANCE;
  private final String myId;
  private final int myVersion;
  private final CompileOrderPlace myOrderPlace;

  protected GenericCompiler(@NotNull String id, int version, @NotNull CompileOrderPlace orderPlace) {
    myId = id;
    myVersion = version;
    myOrderPlace = orderPlace;
  }

  @NotNull
  public abstract KeyDescriptor<Key> getItemKeyDescriptor();
  @NotNull
  public abstract DataExternalizer<SourceState> getSourceStateExternalizer();
  @NotNull
  public abstract DataExternalizer<OutputState> getOutputStateExternalizer();

  @NotNull
  public abstract GenericCompilerInstance<?, ? extends CompileItem<Key, SourceState, OutputState>, Key, SourceState, OutputState> createInstance(@NotNull CompileContext context);

  public final String getId() {
    return myId;
  }

  public final int getVersion() {
    return myVersion;
  }

  @Override
  public boolean validateConfiguration(CompileScope scope) {
    return true;
  }

  public CompileOrderPlace getOrderPlace() {
    return myOrderPlace;
  }

  public enum CompileOrderPlace {
    CLASS_INSTRUMENTING, CLASS_POST_PROCESSING, PACKAGING, VALIDATING
  }

}
