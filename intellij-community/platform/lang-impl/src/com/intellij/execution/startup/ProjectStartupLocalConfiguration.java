// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.startup;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;

@State(name = "ProjectStartupLocalConfiguration", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
final class ProjectStartupLocalConfiguration extends ProjectStartupConfigurationBase {
}
