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
package com.intellij.openapi.externalSystem.service;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
public class InProcessExternalSystemFacadeImpl<S extends ExternalSystemExecutionSettings> extends AbstractExternalSystemFacadeImpl<S> {

  public InProcessExternalSystemFacadeImpl(@NotNull Class<ExternalSystemProjectResolver<S>> projectResolverClass,
                                           @NotNull Class<ExternalSystemTaskManager<S>> buildManagerClass)
    throws IllegalAccessException, InstantiationException
  {
    super(projectResolverClass, buildManagerClass);
  }

  @Override
  protected <I extends RemoteExternalSystemService<S>, C extends I> I createService(@NotNull Class<I> interfaceClass, @NotNull C impl) {
    return impl;
  }
}
