/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.compiler.ant.taskdefs;

import com.intellij.compiler.ant.Tag;
import com.intellij.openapi.compiler.make.ManifestBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;

/**
 * @author Eugene Zhuravlev
 */
public class Manifest extends Tag{
  public Manifest() {
    super("manifest");
  }

  public void applyAttributes(final java.util.jar.Manifest manifest) {
    ManifestBuilder.setGlobalAttributes(manifest.getMainAttributes());
    final Attributes mainAttributes = manifest.getMainAttributes();

    List<Object> keys = new ArrayList<>(mainAttributes.keySet());
    Collections.sort(keys, (o1, o2) -> {
      Attributes.Name name1 = (Attributes.Name)o1;
      Attributes.Name name2 = (Attributes.Name)o2;
      return name1.toString().compareTo(name2.toString());
    });
    for (final Object o : keys) {
      Attributes.Name name = (Attributes.Name)o;
      String value = (String)mainAttributes.get(name);
      add(new Attribute(name.toString(), value));
    }
  }
}
