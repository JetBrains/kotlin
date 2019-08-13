/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.config;

import com.intellij.execution.Location;
import com.intellij.openapi.externalSystem.psi.search.ExternalModuleBuildGlobalSearchScope;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JdkOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.LibraryScopeCache;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.NonClasspathDirectoriesScope;
import icons.GradleIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.GradleBuildClasspathManager;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.extensions.GroovyRunnableScriptType;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.runner.GroovyScriptRunConfiguration;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author peter
 */
public class GradleScriptType extends GroovyRunnableScriptType {

  public static final GradleScriptType INSTANCE = new GradleScriptType();

  private GradleScriptType() {
    super(GradleConstants.EXTENSION);
  }

  @NotNull
  @Override
  public Icon getScriptIcon() {
    return GradleIcons.GradleFile;
  }

  @Override
  public boolean isConfigurationByLocation(@NotNull GroovyScriptRunConfiguration existing, @NotNull Location location) {
    return false;
  }

  @Override
  public GlobalSearchScope patchResolveScope(@NotNull GroovyFile file, @NotNull GlobalSearchScope baseScope) {
    if (!FileUtilRt.extensionEquals(file.getName(), GradleConstants.EXTENSION)) return baseScope;
    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    return patchResolveScopeInner(module, baseScope);
  }

  public GlobalSearchScope patchResolveScopeInner(@Nullable Module module, @NotNull GlobalSearchScope baseScope) {
    if (module == null) return GlobalSearchScope.EMPTY_SCOPE;
    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return baseScope;
    GlobalSearchScope result = GlobalSearchScope.EMPTY_SCOPE;
    final Project project = module.getProject();
    GlobalSearchScope[] jdkScopes = Arrays.stream(ModuleRootManager.getInstance(module).getOrderEntries())
      .filter(entry -> entry instanceof JdkOrderEntry)
      .map(entry -> LibraryScopeCache.getInstance(project).getScopeForSdk((JdkOrderEntry)entry))
      .toArray(GlobalSearchScope[]::new);
    result = jdkScopes.length == 0 ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.union(jdkScopes);

    String modulePath = ExternalSystemApiUtil.getExternalProjectPath(module);
    if (modulePath == null) return result;

    final Collection<VirtualFile> files = GradleBuildClasspathManager.getInstance(project).getModuleClasspathEntries(modulePath);

    result = new ExternalModuleBuildGlobalSearchScope(project, result.uniteWith(new NonClasspathDirectoriesScope(files)), modulePath);

    return result;
  }
}
