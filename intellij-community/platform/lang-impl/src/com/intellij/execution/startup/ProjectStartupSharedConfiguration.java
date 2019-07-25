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
package com.intellij.execution.startup;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * @author Irina.Chernushina on 8/19/2015.
 */
@State(name = "ProjectStartupSharedConfiguration", storages = @Storage("startup.xml"))
public class ProjectStartupSharedConfiguration extends ProjectStartupConfigurationBase {
}
