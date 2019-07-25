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

import java.io.File;
import java.io.IOException;

public class FileSerializable implements WorkingSetSerializable<File, File> {
  @Override
  public String getId() {
    return File.class.getName();
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public void serializeMe(File t, StringBuilder oos) throws IOException {
    oos.append(t.getPath());
  }

  @Override
  public File deserializeMe(Project project, String ois) throws IOException {
    return new File(ois);
  }

  @Override
  public File deserializeMeInvalid(Project project, String ois) throws IOException {
    return new File(ois);
  }
}
