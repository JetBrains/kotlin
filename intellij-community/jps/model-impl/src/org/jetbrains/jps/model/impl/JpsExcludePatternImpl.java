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
package org.jetbrains.jps.model.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsExcludePattern;
import org.jetbrains.jps.model.ex.JpsElementBase;

/**
 * @author nik
 */
public class JpsExcludePatternImpl extends JpsElementBase<JpsExcludePatternImpl> implements JpsExcludePattern {
  private final String myBaseDirUrl;
  private final String myPattern;

  public JpsExcludePatternImpl(@NotNull String baseDirUrl, @NotNull String pattern) {
    myBaseDirUrl = baseDirUrl;
    myPattern = pattern;
  }

  @NotNull
  @Override
  public String getBaseDirUrl() {
    return myBaseDirUrl;
  }

  @NotNull
  @Override
  public String getPattern() {
    return myPattern;
  }

  @NotNull
  @Override
  public JpsExcludePatternImpl createCopy() {
    return new JpsExcludePatternImpl(myBaseDirUrl, myPattern);
  }

  @Override
  public void applyChanges(@NotNull JpsExcludePatternImpl modified) {
  }
}
