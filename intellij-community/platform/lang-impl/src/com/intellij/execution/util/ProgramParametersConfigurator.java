// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.util;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.configurations.SimpleProgramParameters;
import com.intellij.icons.AllIcons;
import com.intellij.ide.macro.*;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.WorkingDirectoryProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ExternalProjectSystemRegistry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProgramParametersConfigurator {
  private static final ExtensionPointName<WorkingDirectoryProvider> WORKING_DIRECTORY_PROVIDER_EP_NAME= ExtensionPointName
    .create("com.intellij.module.workingDirectoryProvider");
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated
  public static final String MODULE_WORKING_DIR = "%MODULE_WORKING_DIR%";

  public void configureConfiguration(SimpleProgramParameters parameters, CommonProgramRunConfigurationParameters configuration) {
    Project project = configuration.getProject();
    Module module = getModule(configuration);

    parameters.getProgramParametersList().addParametersString(expandMacros(expandPath(configuration.getProgramParameters(), module, project)));

    parameters.setWorkingDirectory(getWorkingDir(configuration, project, module));

    Map<String, String> envs = new HashMap<>(configuration.getEnvs());
    EnvironmentUtil.inlineParentOccurrences(envs);
    for (Map.Entry<String, String> each : envs.entrySet()) {
      each.setValue(expandPath(each.getValue(), module, project));
    }

    parameters.setEnv(envs);
    parameters.setPassParentEnvs(configuration.isPassParentEnvs());
  }

  public static void addMacroSupport(@NotNull ExpandableTextField expandableTextField) {
    if (Registry.is("allow.macros.for.run.configurations")) {
      expandableTextField.addExtension(ExtendableTextComponent.Extension.create(AllIcons.General.InlineAdd, AllIcons.General.InlineAddHover, "Insert Macros", ()
        -> MacrosDialog.show(expandableTextField, macro -> {
        if (macro instanceof PromptMacro) return true;
        return !(macro instanceof PromptingMacro) && !(macro instanceof EditorMacro);
      })));
    }
  }

  public static String expandMacros(@Nullable String path) {
    if (path != null && Registry.is("allow.macros.for.run.configurations")) {
      Collection<Macro> macros = MacroManager.getInstance().getMacros();
      for (Macro macro : macros) {
        String template = "$" + macro.getName() + "$";
        for (int index = path.indexOf(template);
             index != -1 && index < path.length() + template.length();
             index = path.indexOf(template, index)) {
          String value = StringUtil.notNullize(macro instanceof PromptMacro ? ((PromptMacro)macro).promptUser() :
                                               macro.preview());
          if (StringUtil.containsWhitespaces(value)) {
            value = "\"" + value + "\"";
          }
          path = path.substring(0, index) + value + path.substring(index + template.length());
          index += value.length();
        }
      }
    }
    return path;
  }

  @Nullable
  public String getWorkingDir(CommonProgramRunConfigurationParameters configuration, Project project, Module module) {
    String workingDirectory = configuration.getWorkingDirectory();
    String defaultWorkingDir = getDefaultWorkingDir(project);
    if (StringUtil.isEmptyOrSpaces(workingDirectory)) {
      workingDirectory = defaultWorkingDir;
      if (workingDirectory == null) {
        return null;
      }
    }
    workingDirectory = expandPath(workingDirectory, module, project);
    if (!FileUtil.isAbsolutePlatformIndependent(workingDirectory) && defaultWorkingDir != null) {
      if (PathMacroUtil.DEPRECATED_MODULE_DIR.equals(workingDirectory)) {
        return defaultWorkingDir;
      }

      //noinspection deprecation
      if (MODULE_WORKING_DIR.equals(workingDirectory)) {
        workingDirectory = PathMacroUtil.MODULE_WORKING_DIR;
      }

      if (PathMacroUtil.MODULE_WORKING_DIR.equals(workingDirectory)) {
        if (module == null) {
          return defaultWorkingDir;
        }
        else {
          String workingDir = getDefaultWorkingDir(module);
          if (workingDir != null) {
            return workingDir;
          }
        }
      }
      workingDirectory = defaultWorkingDir + "/" + workingDirectory;
    }
    return workingDirectory;
  }

  @Nullable
  protected String getDefaultWorkingDir(@NotNull Project project) {
    return PathUtil.getLocalPath(project.getBaseDir());
  }

  @Nullable
  protected String getDefaultWorkingDir(@NotNull Module module) {
    for (WorkingDirectoryProvider provider : WORKING_DIRECTORY_PROVIDER_EP_NAME.getExtensions()) {
      @SystemIndependent String path = provider.getWorkingDirectoryPath(module);
      if (path != null) return path;
    }
    VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
    if (roots.length > 0) {
      return PathUtil.getLocalPath(roots[0]);
    }
    return null;
  }

  public void checkWorkingDirectoryExist(CommonProgramRunConfigurationParameters configuration, Project project, Module module)
    throws RuntimeConfigurationWarning {
    final String workingDir = getWorkingDir(configuration, project, module);
    if (workingDir == null) {
      throw new RuntimeConfigurationWarning("Working directory is null for "+
                                            "project '" + project.getName() + "' ("+project.getBasePath()+")"
                                            + ", module " + (module == null? "null" : "'" + module.getName() + "' (" + module.getModuleFilePath() + ")"));
    }
    if (!new File(workingDir).exists()) {
      throw new RuntimeConfigurationWarning("Working directory '" + workingDir + "' doesn't exist");
    }
  }

  protected String expandPath(@Nullable String path, Module module, Project project) {
    // https://youtrack.jetbrains.com/issue/IDEA-190100
    // if old macro is used (because stored in the default project and applied for a new imported project) and module file stored under .idea, use new module macro instead
    if (module != null && PathMacroUtil.DEPRECATED_MODULE_DIR.equals(path) &&
        module.getModuleFilePath().contains("/" + Project.DIRECTORY_STORE_FOLDER + "/") &&
        ExternalProjectSystemRegistry.getInstance().getExternalSource(module) != null /* not really required but to reduce possible impact */) {
      return getDefaultWorkingDir(module);
    }

    path = PathMacroManager.getInstance(project).expandPath(path);
    if (module != null) {
      path = PathMacroManager.getInstance(module).expandPath(path);
    }
    return path;
  }

  @Nullable
  protected Module getModule(CommonProgramRunConfigurationParameters configuration) {
    if (configuration instanceof ModuleBasedConfiguration) {
      return ((ModuleBasedConfiguration)configuration).getConfigurationModule().getModule();
    }
    return null;
  }
}
