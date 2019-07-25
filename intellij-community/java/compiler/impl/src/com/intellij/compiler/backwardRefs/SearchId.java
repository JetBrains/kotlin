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
package com.intellij.compiler.backwardRefs;

import org.jetbrains.annotations.Nullable;

public class SearchId {
  private final String myDeserializedName;
  private final int myId;

  SearchId(@Nullable String deserializedName, int id) {
    myDeserializedName = deserializedName;
    myId = id;
  }

  SearchId(@Nullable String deserializedName) {
    this(deserializedName, -1);
  }

  SearchId(int id) {
    this(null, id);
  }

  public String getDeserializedName() {
    return myDeserializedName;
  }

  public int getId() {
    return myId;
  }
}
