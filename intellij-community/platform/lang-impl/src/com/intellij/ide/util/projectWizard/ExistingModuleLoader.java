// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.projectWizard;

import com.intellij.CommonBundle;
import com.intellij.application.options.PathMacrosCollector;
import com.intellij.conversion.ConversionResult;
import com.intellij.conversion.ConversionService;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectMacrosUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 */
public class ExistingModuleLoader extends ModuleBuilder {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.util.projectWizard.ExistingModuleLoader");

  public static ExistingModuleLoader setUpLoader(final String moduleFilePath) {
    final ExistingModuleLoader moduleLoader = new ExistingModuleLoader();
    moduleLoader.setModuleFilePath(moduleFilePath);
    final int startIndex = moduleFilePath.lastIndexOf('/');
    final int endIndex = moduleFilePath.lastIndexOf(".");
    if (startIndex >= 0 && endIndex > startIndex + 1) {
      final String name = moduleFilePath.substring(startIndex + 1, endIndex);
      moduleLoader.setName(name);
    }
    return moduleLoader;
  }

  @Override
  @NotNull
  public Module createModule(@NotNull ModifiableModuleModel moduleModel)
    throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
    LOG.assertTrue(getName() != null);

    final String moduleFilePath = getModuleFilePath();

    LOG.assertTrue(moduleFilePath != null);
    LOG.assertTrue(new File(moduleFilePath).exists());

    return moduleModel.loadModule(moduleFilePath);
  }

  @Override
  public ModuleType getModuleType() {
    return null; // no matter
  }

  @Override
  public boolean validate(final Project current, final Project dest) {
    if (getName() == null) return false;
    String moduleFilePath = getModuleFilePath();
    if (moduleFilePath == null) return false;
    final File file = new File(moduleFilePath);
    if (file.exists()) {
      try {
        final ConversionResult result = ConversionService.getInstance().convertModule(dest, file);
        if (result.openingIsCanceled()) {
          return false;
        }
        final Element root = JDOMUtil.load(file);
        final Set<String> usedMacros = PathMacrosCollector.getMacroNames(root);
        usedMacros.remove(PathMacroUtil.DEPRECATED_MODULE_DIR);
        usedMacros.removeAll(PathMacros.getInstance().getAllMacroNames());

        if (usedMacros.size() > 0) {
          final boolean ok = ProjectMacrosUtil.showMacrosConfigurationDialog(current, usedMacros);
          if (!ok) {
            return false;
          }
        }
      }
      catch (JDOMException | IOException e) {
        Messages.showMessageDialog(e.getMessage(), IdeBundle.message("title.error.reading.file"), Messages.getErrorIcon());
        return false;
      }
    } else {
      Messages.showErrorDialog(current, IdeBundle.message("title.module.file.does.not.exist", moduleFilePath),
                               CommonBundle.message("title.error"));
      return false;
    }
    return true;
  }
}
