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
package org.jetbrains.jps.model.jarRepository.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.jarRepository.JpsRemoteRepositoriesConfiguration;
import org.jetbrains.jps.model.jarRepository.JpsRemoteRepositoryService;

/**
 * @author Eugene Zhuravlev
 */
public class JpsRemoteRepositoryServiceImpl extends JpsRemoteRepositoryService {
  @Nullable
  @Override
  public JpsRemoteRepositoriesConfiguration getRemoteRepositoriesConfiguration(@NotNull JpsProject project) {
    return project.getContainer().getChild(JpsRemoteRepositoriesConfigurationImpl.ROLE);
  }

  @NotNull
  @Override
  public synchronized JpsRemoteRepositoriesConfiguration getOrCreateRemoteRepositoriesConfiguration(@NotNull JpsProject project) {
    JpsRemoteRepositoriesConfiguration config = getRemoteRepositoriesConfiguration(project);
    if (config == null) {
      config = project.getContainer().setChild(JpsRemoteRepositoriesConfigurationImpl.ROLE, new JpsRemoteRepositoriesConfigurationImpl());
    }
    return config;
  }
}
