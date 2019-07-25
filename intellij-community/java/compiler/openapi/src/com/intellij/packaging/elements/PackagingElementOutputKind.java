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
package com.intellij.packaging.elements;

import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class PackagingElementOutputKind {
  @NotNull public static final PackagingElementOutputKind DIRECTORIES_WITH_CLASSES = new PackagingElementOutputKind(true, false);
  @NotNull public static final PackagingElementOutputKind JAR_FILES = new PackagingElementOutputKind(false, true);
  @NotNull public static final PackagingElementOutputKind OTHER = new PackagingElementOutputKind(false, false);

  private final boolean myContainsDirectoriesWithClasses;
  private final boolean myContainsJarFiles;

  public PackagingElementOutputKind(boolean containsDirectoriesWithClasses, boolean containsJarFiles) {
    myContainsDirectoriesWithClasses = containsDirectoriesWithClasses;
    myContainsJarFiles = containsJarFiles;
  }

  public boolean containsDirectoriesWithClasses() {
    return myContainsDirectoriesWithClasses;
  }

  public boolean containsJarFiles() {
    return myContainsJarFiles;
  }
}
