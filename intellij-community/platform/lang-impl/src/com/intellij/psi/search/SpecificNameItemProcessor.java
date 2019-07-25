/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.psi.search;

import com.intellij.openapi.util.io.FileUtil;

public abstract class SpecificNameItemProcessor implements PsiFileSystemItemProcessor {
  private final String myName;

  protected SpecificNameItemProcessor(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  @Override
  public boolean acceptItem(String name, boolean isDirectory) {
    return FileUtil.namesEqual(myName, name);
  }
}
