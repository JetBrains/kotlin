/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.compiler.impl;

import com.intellij.compiler.CompilerEncodingService;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.openapi.vfs.encoding.EncodingProjectManagerImpl;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author nik
 */
public class CompilerEncodingServiceImpl extends CompilerEncodingService {
  @NotNull private final Project myProject;
  private final CachedValue<Map<Module, Set<Charset>>> myModuleFileEncodings;

  public CompilerEncodingServiceImpl(@NotNull Project project) {
    myProject = project;
    myModuleFileEncodings = CachedValuesManager.getManager(project).createCachedValue(() -> {
      Map<Module, Set<Charset>> result = computeModuleCharsetMap();
      return CachedValueProvider.Result.create(result, ProjectRootManager.getInstance(myProject),
                                               ((EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject)).getModificationTracker());
    }, false);
  }

  @NotNull
  private Map<Module, Set<Charset>> computeModuleCharsetMap() {
    final Map<Module, Set<Charset>> map = new THashMap<>();
    final Map<VirtualFile, Charset> mappings = ((EncodingProjectManagerImpl)EncodingProjectManager.getInstance(myProject)).getAllMappings();
    ProjectFileIndex index = ProjectRootManager.getInstance(myProject).getFileIndex();
    final CompilerManager compilerManager = CompilerManager.getInstance(myProject);
    for (Map.Entry<VirtualFile, Charset> entry : mappings.entrySet()) {
      final VirtualFile file = entry.getKey();
      final Charset charset = entry.getValue();
      if (file == null || charset == null || (!file.isDirectory() && !compilerManager.isCompilableFileType(file.getFileType()))
          || !index.isUnderSourceRootOfType(file, JavaModuleSourceRootTypes.SOURCES)) continue;

      final Module module = index.getModuleForFile(file);
      if (module == null) continue;

      Set<Charset> set = map.get(module);
      if (set == null) {
        set = new LinkedHashSet<>();
        map.put(module, set);

        final VirtualFile sourceRoot = index.getSourceRootForFile(file);
        VirtualFile current = file.getParent();
        Charset parentCharset = null;
        while (current != null) {
          final Charset currentCharset = mappings.get(current);
          if (currentCharset != null) {
            parentCharset = currentCharset;
          }
          if (current.equals(sourceRoot)) {
            break;
          }
          current = current.getParent();
        }
        if (parentCharset != null) {
          set.add(parentCharset);
        }
      }
      set.add(charset);
    }
    //todo[nik,jeka] perhaps we should take into account encodings of source roots only not individual files
    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      for (VirtualFile file : ModuleRootManager.getInstance(module).getSourceRoots(true)) {
        Charset encoding = EncodingProjectManager.getInstance(myProject).getEncoding(file, true);
        if (encoding != null) {
          Set<Charset> charsets = map.get(module);
          if (charsets == null) {
            charsets = new LinkedHashSet<>();
            map.put(module, charsets);
          }
          charsets.add(encoding);
        }
      }
    }
    
    return map;
  }

  @Override
  @Nullable
  public Charset getPreferredModuleEncoding(@NotNull Module module) {
    final Set<Charset> encodings = myModuleFileEncodings.getValue().get(module);
    return ContainerUtil.getFirstItem(encodings, EncodingProjectManager.getInstance(myProject).getDefaultCharset());
  }

  @NotNull
  @Override
  public Collection<Charset> getAllModuleEncodings(@NotNull Module module) {
    final Set<Charset> encodings = myModuleFileEncodings.getValue().get(module);
    if (encodings != null) {
      return encodings;
    }
    return ContainerUtil.createMaybeSingletonList(EncodingProjectManager.getInstance(myProject).getDefaultCharset());
  }
}
