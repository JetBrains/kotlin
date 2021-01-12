package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Denis Zhdanov
 */
public interface ExternalSystemConfigLocator {

  ExtensionPointName<ExternalSystemConfigLocator> EP_NAME = ExtensionPointName.create("com.intellij.externalSystemConfigLocator");
  
  @NotNull ProjectSystemId getTargetExternalSystemId();
  
  /**
   * Allows to adjust target external system config file.
   * <p/>
   * Example: 'gradle' external system stores config file parent as config path and we might want to locate exact config file
   * given it's directory file.
   * 
   * @param configPath  base config file
   * @return            config file to use (if any)
   */
  @Nullable
  VirtualFile adjust(@NotNull VirtualFile configPath);

  /**
   * Returns all configuration files used by external system to build the project.
   *
   * @param externalProjectSettings external system project settings
   * @return external system project config files
   */
  @NotNull
  List<VirtualFile> findAll(@NotNull ExternalProjectSettings externalProjectSettings);
}
