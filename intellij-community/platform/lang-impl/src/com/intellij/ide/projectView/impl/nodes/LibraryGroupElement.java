/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene Zhuravlev
 */
public final class LibraryGroupElement {
  public static final DataKey<LibraryGroupElement[]> ARRAY_DATA_KEY = DataKey.create("libraryGroup.array");
  
  private final Module myModule;

  public LibraryGroupElement(@NotNull Module module) {
    myModule = module;
  }

  public Module getModule() {
    return myModule;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LibraryGroupElement)) return false;

    final LibraryGroupElement libraryGroupElement = (LibraryGroupElement)o;

    if (!myModule.equals(libraryGroupElement.myModule)) return false;

    return true;
  }

  public int hashCode() {
    return myModule.hashCode();
  }
}
