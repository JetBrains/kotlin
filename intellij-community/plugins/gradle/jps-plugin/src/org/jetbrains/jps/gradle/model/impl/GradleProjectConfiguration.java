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
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import gnu.trove.THashMap;

import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class GradleProjectConfiguration {
  public static final String CONFIGURATION_FILE_RELATIVE_PATH = "gradle/configuration.xml";

  @Tag("resource-processing")
  @MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false, entryTagName = "gradle-module",
                 keyAttributeName = "name")
  public Map<String, GradleModuleResourceConfiguration> moduleConfigurations = new THashMap<>();
}
