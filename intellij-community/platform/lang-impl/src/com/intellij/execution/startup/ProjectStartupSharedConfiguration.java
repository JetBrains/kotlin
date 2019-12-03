// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.startup;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

/**
 * @author Irina.Chernushina on 8/19/2015.
 */
@State(name = "ProjectStartupSharedConfiguration", storages = @Storage("startup.xml"))
final class ProjectStartupSharedConfiguration extends ProjectStartupConfigurationBase {
}
