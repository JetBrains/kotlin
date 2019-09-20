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
package org.jetbrains.plugins.gradle.model.web;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public interface WebConfiguration extends Serializable {

  List<? extends WarModel> getWarModels();

  interface WarModel extends Serializable {
    @NotNull
    String getWarName();

    File getArchivePath();

    String getWebAppDirName();

    File getWebAppDir();

    File getWebXml();

    List<WebResource> getWebResources();

    Set<File> getClasspath();

    String getManifestContent();
  }

  interface WebResource extends Serializable {

    @NotNull
    String getWarDirectory();

    @NotNull
    String getRelativePath();

    @NotNull
    File getFile();
  }
}
