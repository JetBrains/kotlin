// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform;

import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
final class PlatformProjectConfigurator implements DirectoryProjectConfigurator {
  private static final Logger LOG = Logger.getInstance(PlatformProjectConfigurator.class);

  @Override
  public void configureProject(@NotNull Project project, @NotNull VirtualFile baseDir, @NotNull Ref<Module> moduleRef, boolean isProjectCreatedWithWizard) {
    final ModuleManager moduleManager = ModuleManager.getInstance(project);
    final Module[] modules = moduleManager.getModules();
    if (modules.length != 0) {
      LOG.info("PlatformProjectConfigurator is not applicable because modules are already configured (module count: " + modules.length + ")");
      return;
    }

    // correct module name when opening root of drive as project (RUBY-5181)
    String moduleName = baseDir.getName().replace(":", "");
    String imlName = baseDir.getPath() + "/.idea/" + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION;
    ModuleTypeManager moduleTypeManager = ModuleTypeManager.getInstance();

    ApplicationManager.getApplication().runWriteAction(() -> {
      Module module = moduleManager.newModule(imlName, moduleTypeManager == null ? "unknown" : moduleTypeManager.getDefaultModuleType().getId());
      ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
      try {
        VirtualFile[] contentRoots = model.getContentRoots();
        if (contentRoots.length == 0) {
          model.addContentEntry(baseDir);
          LOG.debug("content root " + baseDir + " is added");
        }
        else {
          LOG.info("content root " + baseDir + " is not added because content roots are already configured " +
                   "(content root count: " + contentRoots.length + ")");
        }
        model.inheritSdk();
        model.commit();
      }
      finally {
        if (!model.isDisposed()) {
          model.dispose();
        }
      }
      moduleRef.set(module);
    });
  }
}
