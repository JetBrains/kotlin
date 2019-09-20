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
import com.intellij.openapi.roots.LibraryOrSdkOrderEntry;
import com.intellij.openapi.util.Comparing;

/**
 * @author Eugene Zhuravlev
 */
public final class NamedLibraryElement {
  public static final DataKey<NamedLibraryElement[]> ARRAY_DATA_KEY = DataKey.create("namedLibrary.array");

  private final Module myContextModule;
  private final LibraryOrSdkOrderEntry myEntry;

  public NamedLibraryElement(Module parent, LibraryOrSdkOrderEntry entry) {
    myContextModule = parent;
    myEntry = entry;
  }

  public Module getModule() {
    return myContextModule;
  }

  public String getName() {
    return myEntry.getPresentableName();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NamedLibraryElement)) return false;

    final NamedLibraryElement namedLibraryElement = (NamedLibraryElement)o;

    if (!myEntry.equals(namedLibraryElement.myEntry)) return false;
    if (!Comparing.equal(myContextModule, namedLibraryElement.myContextModule)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myContextModule != null ? myContextModule.hashCode() : 0;
    result = 29 * result + myEntry.hashCode();
    return result;
  }

  public LibraryOrSdkOrderEntry getOrderEntry() {
    return myEntry;
  }
}
