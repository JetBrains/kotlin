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
package org.jetbrains.jps.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.service.JpsServiceManager;

import java.util.Collection;

/**
 * @author nik
 */
public abstract class JpsPluginManager {
  @NotNull
  public static JpsPluginManager getInstance() {
    return JpsServiceManager.getInstance().getService(JpsPluginManager.class);
  }

  @NotNull
  public abstract <T> Collection<T> loadExtensions(@NotNull Class<T> extensionClass);
}
