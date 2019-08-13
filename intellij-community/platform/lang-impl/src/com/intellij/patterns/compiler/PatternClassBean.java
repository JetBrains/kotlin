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

package com.intellij.patterns.compiler;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

public class PatternClassBean extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance("#com.intellij.pattern.compiler.PatternClassBean");
  @Attribute("className")
  public String className;
  @Attribute("alias")
  public String alias;
  @Tag("description")
  public String description;

  public String getDescription() {
    return description;
  }

  public String getAlias() {
    return alias;
  }

  public Class getPatternClass() {
    try {
      return findClass(className);
    }
    catch (ClassNotFoundException e) {
      LOG.error(e);
    }
    return null;
  }

}
