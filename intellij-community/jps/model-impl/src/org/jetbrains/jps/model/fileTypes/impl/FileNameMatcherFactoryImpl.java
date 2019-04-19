/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.jps.model.fileTypes.impl;

import com.intellij.openapi.fileTypes.ExactFileNameMatcher;
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.WildcardFileNameMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.fileTypes.FileNameMatcherFactory;

/**
 * @author nik
 */
public class FileNameMatcherFactoryImpl extends FileNameMatcherFactory {
  @Override
  @NotNull
  public FileNameMatcher createMatcher(@NotNull String pattern) {
    if (pattern.startsWith("*.") &&
        pattern.indexOf('*', 2) < 0 &&
        pattern.indexOf('.', 2) < 0 &&
        pattern.indexOf('?', 2) < 0) {
      return new ExtensionFileNameMatcher(pattern.substring(2).toLowerCase());
    }

    if (pattern.contains("*") || pattern.contains("?")) {
      return new WildcardFileNameMatcher(pattern);
    }

    return new ExactFileNameMatcher(pattern);
  }
}
