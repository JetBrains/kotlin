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
package com.intellij.openapi.compiler.make;

import com.intellij.openapi.application.ApplicationNamesInfo;
import org.jetbrains.annotations.NonNls;

import java.util.jar.Attributes;

public class ManifestBuilder {
  @NonNls private static final String NAME = "Created-By";
  private static final Attributes.Name CREATED_BY = new Attributes.Name(NAME);

  private ManifestBuilder() {
  }

  public static void setGlobalAttributes(Attributes mainAttributes) {
    setVersionAttribute(mainAttributes);
    setIfNone(mainAttributes, CREATED_BY, ApplicationNamesInfo.getInstance().getFullProductName());
  }

  public static void setVersionAttribute(Attributes mainAttributes) {
    setIfNone(mainAttributes, Attributes.Name.MANIFEST_VERSION, "1.0");
  }

  private static void setIfNone(Attributes mainAttributes, Attributes.Name attrName, String value) {
    if (mainAttributes.getValue(attrName) == null) {
      mainAttributes.put(attrName, value);
    }
  }
}
