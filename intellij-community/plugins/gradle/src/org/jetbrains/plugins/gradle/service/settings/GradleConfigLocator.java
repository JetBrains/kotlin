// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.gradle.tooling.GradleConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * We store not gradle config file but its parent dir path instead. That is implied by gradle design
 * ({@link GradleConnector#forProjectDirectory(File)}).
 * <p/>
 * That's why we need to provide special code which maps that directory to exact config file.
 *
 * @author Denis Zhdanov
 */
public class GradleConfigLocator implements ExternalSystemConfigLocator {

  @NotNull
  @Override
  public ProjectSystemId getTargetExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  @Nullable
  @Override
  public VirtualFile adjust(@NotNull VirtualFile configPath) {
    if (!configPath.isDirectory()) {
      return configPath;
    }

    VirtualFile result = configPath.findChild(GradleConstants.DEFAULT_SCRIPT_NAME);
    if (result != null) {
      return result;
    }
    result = configPath.findChild(GradleConstants.KOTLIN_DSL_SCRIPT_NAME);
    if (result != null) {
      return result;
    }

    for (VirtualFile child : configPath.getChildren()) {
      String name = child.getName();
      if (!name.endsWith(GradleConstants.EXTENSION) || !name.endsWith(GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION)) {
        continue;
      }
      if (!GradleConstants.SETTINGS_FILE_NAME.equals(name) &&
          !GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME.equals(name) &&
          !child.isDirectory()) {
        return child;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public List<VirtualFile> findAll(@NotNull ExternalProjectSettings externalProjectSettings) {
    List<VirtualFile> list = new ArrayList<>();
    for (String path : externalProjectSettings.getModules()) {
      VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(path));
      if (vFile != null) {
        for (VirtualFile child : vFile.getChildren()) {
          String name = child.getName();
          if (!child.isDirectory() && name.endsWith(GradleConstants.EXTENSION)) {
            list.add(child);
          }
        }
      }
    }
    return list;
  }
}
