/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.jps.gradle.model.impl.artifacts;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;

import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class GradleArtifactExtensionProperties {
  @Attribute("external-project-path")
  public String externalProjectPath;
  @Tag("manifest")
  public String manifest;
  @Tag("files")
  @MapAnnotation(surroundWithTag = false, keyAttributeName = "path", entryTagName = "file")
  public Map<String, String> additionalFiles;
}
