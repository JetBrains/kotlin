// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.module;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.project.ProjectBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public abstract class WebModuleTypeBase<T extends ModuleBuilder> extends ModuleType<T> implements ModuleTypeWithWebFeatures {
  @NonNls public static final String WEB_MODULE = ModuleTypeId.WEB_MODULE;

  public WebModuleTypeBase() {
    super(WEB_MODULE);
  }

  @NotNull
  public static WebModuleTypeBase<?> getInstance() {
    return (WebModuleTypeBase<?>)ModuleTypeManager.getInstance().findByID(WEB_MODULE);
  }

  @NotNull
  @Override
  public String getName() {
    return ProjectBundle.message("module.web.title");
  }

  @NotNull
  @Override
  public String getDescription() {
    return ProjectBundle.message("module.web.description");
  }

  @NotNull
  @Override
  public Icon getNodeIcon(final boolean isOpened) {
    return AllIcons.Nodes.Module;
  }

  /**
   * @deprecated Use {@link ModuleTypeWithWebFeatures#isAvailable}
   */
  @Deprecated
  public static boolean isWebModule(@NotNull Module module) {
    return ModuleTypeWithWebFeatures.isAvailable(module);
  }

  @Override
  public boolean hasWebFeatures(@NotNull Module module) {
    return WEB_MODULE.equals(ModuleType.get(module).getId());
  }
}