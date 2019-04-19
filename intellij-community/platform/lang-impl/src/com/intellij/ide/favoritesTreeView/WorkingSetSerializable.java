/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.favoritesTreeView;

import com.intellij.openapi.project.Project;

import java.io.IOException;

public interface WorkingSetSerializable<Valid, Invalid> {
  String getId();

  int getVersion();

  void serializeMe(final Valid t, final StringBuilder oos) throws IOException;

  Valid deserializeMe(Project project, final String ois) throws IOException;

  Invalid deserializeMeInvalid(Project project, final String ois) throws IOException;
}
